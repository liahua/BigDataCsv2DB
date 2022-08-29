package com.chenahua.transform.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.chenahua.transform.service.TransformService;
import com.chenahua.transform.vo.MarketQuoteVO;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * market_quote.csv是一份市场报价信息，每天下午16点由上游系统发送到SFTP根目录下，
 * 大小约为1G，请完成一个基于springboot框架的java程序（及mysql数据库表结构涉及），
 * 可以完成每日market_quote.csv的读取（不要求实现定时任务），并存入数据库
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 *
 * 痛点:
 * 1.文件过大,造成内存溢出 可以通过blockedQueue搞
 * 2.文件过大,单线程读可能成为瓶颈,可以采用多线程读方式解决,不保证顺序
 * 3.可以先切分文件(已实现接口,可输出小文件)
 * 4.写入sql,通过连接池多connection batch批处理
 * 5.降低单库压力可以通过负载均衡写入多个库(动态路由)
 * 6.如果写入多个库,可以启动后台线程服务,异步统一到一个库
 * 7.如果是单点服务非分布式,若要解决脏数据问题(重复数据),要么建立唯一索引(会降低数据库写入速度),
 * 要么通过位图法来搞,000011111011101111,通过bit占位的方式,占据指定位置(cpu只需要做进位),判断也只需要作与即可确定是否该数据已录入
 *
 * 1.切分CSV分发
 */

@Service
public class TransformServiceImpl implements TransformService {

    private static final Log log = LogFactory.get();
    /**
     * 默认每行标准长度
     */
    public static final long TEMP_LINE_LENGTH = 100;
    /**
     * 多线程读生产者数目
     */
    private final int MULTI_PROVIDER_NUM = 10;
    /**
     * 换行符的ASCII码 \n
     */
    public final static int NEW_LINE_CHAR_ASCII = 10;


    private final String SQL = "insert into market_quote values (null,?,?,?,?,?,?,?)";


    @Value("${csvFile.path}${csvFile.name}")
    String filePath;
//    @Autowired
    private HikariDataSource dataSource;

    ThreadPoolExecutor consumerPool = new ThreadPoolExecutor(1, 5, 20, TimeUnit.SECONDS, new SynchronousQueue<>(), ThreadFactoryBuilder.create().setNamePrefix("consumer-").setUncaughtExceptionHandler((t, e) -> {
        log.error(t.getName() + t.getId(), e);
    }).build());
    ThreadPoolExecutor providerPool = new ThreadPoolExecutor(5, 5, 20, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), ThreadFactoryBuilder.create().setNamePrefix("provider-").setUncaughtExceptionHandler((t, e) -> {
        log.error(t.getName() + t.getId(), e);
    }).build());

    volatile boolean finish = false;

    private static LinkedBlockingDeque<MarketQuoteVO> queue = new LinkedBlockingDeque<>();

    private int CONSUMER_BATCH_NO = 10;

    private int CONSUMER_NO = 4;

    CountDownLatch lock = new CountDownLatch(CONSUMER_NO);

    /**
     * @param highPerformance true 为开启多线程读,不保证入库顺序
     * @throws InterruptedException
     * @throws IOException
     * @throws ExecutionException
     */
    @Override
    public void transformCSV(boolean highPerformance) throws InterruptedException, IOException, ExecutionException {
        checkFile(filePath);
        if (!highPerformance) {
            new Thread(() -> {
                try {
                    provider();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            for (int i = 0; i < CONSUMER_NO; i++) {
                consumerPool.submit(() -> {
                    try {
                        consumerMarketQuote();
                    } catch (SQLException e) {
                        log.error(e);
                    }
                });
            }


            lock.await();
        } else {
            ArrayList<Future> providerFutures = new ArrayList<>();
            ArrayList<ReadTask> readTasks = providerMulti();
            for (ReadTask task : readTasks) {
                Future<?> submit = providerPool.submit(task);
                providerFutures.add(submit);
            }

            for (int i = 0; i < CONSUMER_NO; i++) {
                consumerPool.submit(() -> {
                    try {
                        consumerMarketQuote();
                    } catch (SQLException e) {
                        log.error(e);
                    }
                });
            }

            //等待生产者都结束 测试用防止 JUNIT直接退出
            for (Future providerFuture : providerFutures) {
                providerFuture.get();
            }
            finish = true;
            //等待生产者都结束 测试用防止 JUNIT直接退出,provider未启动
            lock.await();
        }
    }

    private void provider() throws IOException, InterruptedException {


        FileReader fileReader = new FileReader(filePath);

        BufferedReader reader = fileReader.getReader();
        reader.readLine();
        String s;
        int count = 0;
        while ((s = reader.readLine()) != null) {
            provideMarketQuote(s);

            count = count + 1;
            log.debug("count = " + count);
        }
        finish = true;
    }

    /**
     * 多线程方式读
     * @param s
     * @throws InterruptedException
     */
    public static void provideMarketQuote(String s) throws InterruptedException {
        String[] split = s.split(",");
        MarketQuoteVO marketQuoteVO = new MarketQuoteVO();
        marketQuoteVO
                .setCurveName(split[0])
                .setInstrumentType(split[1])
                .setInstrumentName(split[2])
                .setTenor(split[3])
                .setQuote(split[4])
                .setMaturityDate(split[5])
                .setMHRepDate(split[6]);
        //扔到消费者队列里
        queue.put(marketQuoteVO);
    }

    /**
     * begin_pos 起始文件指针
     * avg_read_size  平均分段长度
     * expect_end_pos =begin_pos+avg_read_size
     * real_end_pos=fixPoint(expect_end_pos)  //修正指针到/r/n
     * real_length=real_end_pos-begin_pos+1
     * readTask(begin_pos,real_length)
     * <p>
     * next_begin_pos=real_end_pos+1
     *
     * @return
     * @throws IOException
     */

    public ArrayList<ReadTask> providerMulti() throws IOException {
        long beginPos = 0;
        long size = new RandomAccessFile(filePath, "r").length();
        long AVG_READ_SIZE = size / MULTI_PROVIDER_NUM;
        ArrayList<ReadTask> readTasksQueue = new ArrayList<>(10);
        while (true) {
            if (size - beginPos < AVG_READ_SIZE) {
                //最后一段 不满足平均距离 一直读到文件末
                ReadTask task = new ReadTask(beginPos, size - beginPos, filePath, true);
                readTasksQueue.add(task);
                break;
            }
            long expectEndPos = beginPos + AVG_READ_SIZE;
            long realEndPos = fixPoint(expectEndPos, filePath);
            long realLength = realEndPos - beginPos + 1;
            ReadTask task = new ReadTask(beginPos, realLength, filePath);
            beginPos = realEndPos + 1;
            readTasksQueue.add(task);
        }
        return readTasksQueue;
    }

    /**
     * 截取位置
     * \nbbaaab
     * --------------
     * bba\naab
     * --------------
     * bbaaab\n
     *
     * @param expectEndPos
     * @return \n 的 real_end_pos
     * @throws IOException
     */
    private long fixPoint(long expectEndPos, String filePath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        FileChannel channel = fileInputStream.getChannel();
        MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, expectEndPos, TEMP_LINE_LENGTH);
        int offset = 0;
        while (true) {
            if (map.get() != NEW_LINE_CHAR_ASCII) {
                offset = offset + 1;
            } else {
                break;
            }
            if (offset >= TEMP_LINE_LENGTH) {
                map = channel.map(FileChannel.MapMode.READ_ONLY, expectEndPos + offset, TEMP_LINE_LENGTH);
            }
        }
        return offset + expectEndPos;
    }


    void consumerMarketQuote() throws SQLException {
        while (!queue.isEmpty() || !finish) {
            Connection connection = dataSource.getConnection();

            log.info("use connection = " + connection);

            connection.setAutoCommit(false);
            PreparedStatement stat = connection.prepareStatement(SQL);
            MarketQuoteVO marketQuoteVO = null;
            try {
                for (int i = 0; i < CONSUMER_BATCH_NO; i++) {
                    //空了
                    marketQuoteVO = queue.poll(60, TimeUnit.SECONDS);
                    if (marketQuoteVO != null) {
                        stat.setString(1, marketQuoteVO.getCurveName());
                        stat.setString(2, marketQuoteVO.getInstrumentType());
                        stat.setString(3, marketQuoteVO.getInstrumentName());
                        stat.setString(4, marketQuoteVO.getTenor());
                        stat.setString(5, marketQuoteVO.getQuote());
                        stat.setString(6, marketQuoteVO.getMaturityDate());
                        stat.setString(7, marketQuoteVO.getMHRepDate());
                        stat.addBatch();
                    }
                }
                stat.executeBatch();
                connection.commit();


            } catch (InterruptedException e) {
                log.error(e);
            } finally {
                connection.close();
            }
        }
        lock.countDown();
        log.debug("lock = " + lock.getCount());
        log.debug("queue.isEmpty() = " + queue.isEmpty());
        log.debug("finish = " + finish);
    }


    /**
     * 文件是否存在
     * 文件日期是否是当前日期
     * 不太重要  不太想写
     * 基本就是 文件===>bak 移位
     * 如果是定时任务
     * 可以将文件日期 当前定时任务日期入库 防止分布式定时任务重复执行
     * 包括要有超时机制,活动任务定时更新最后运行时间
     *
     * @param filePath
     * @return
     */
    private boolean checkFile(String filePath) {
        boolean exist = FileUtil.exist(filePath) && FileUtil.isFile(filePath);
        boolean notEmpty = FileUtil.isNotEmpty(FileUtil.file(filePath));

        return false;
    }
}
