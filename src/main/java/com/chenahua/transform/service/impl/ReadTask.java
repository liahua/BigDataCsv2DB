package com.chenahua.transform.service.impl;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static com.chenahua.transform.service.impl.TransformServiceImpl.*;

@ToString
@Getter
@Setter
public class ReadTask implements Runnable {
    private static final Log log = LogFactory.get();
    private long beginPos;
    private long realLength;
    private String filePath;
    private boolean lastSegmentFlag = false;

    public ReadTask(long beginPos, long realLength, String filePath) {
        this.beginPos = beginPos;
        this.realLength = realLength;
        this.filePath = filePath;
    }

    public ReadTask(long beginPos, long realLength, String filePath, boolean lastSegmentFlag) {
        this(beginPos, realLength, filePath);
        this.lastSegmentFlag = lastSegmentFlag;
    }

    public void doTask() throws InterruptedException, IOException {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        FileChannel channel = fileInputStream.getChannel();
        MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, beginPos, realLength);
        int offset = 0;
        int pos = 0;
        while (offset < realLength) {
            byte b = map.get();
            offset = offset + 1;
            if (b == NEW_LINE_CHAR_ASCII) {
                log.debug(map.position() + "|" + pos + "|" + offset + "|" + this.toString());
                byte[] bytes = new byte[offset - pos];
//                map.get(pos, bytes);
                provideMarketQuote(new String(bytes));
                pos = offset;
            }
        }
        if (lastSegmentFlag) {
            long length = realLength - pos;
            if (length != 0) {
                byte[] bytes = new byte[(int) length];
//                map.get(pos, bytes);
                provideMarketQuote(new String(bytes));
            }
        }
    }

    @Override
    public void run() {
        try {
            log.debug(this.toString());
            doTask();
        } catch (InterruptedException | IOException e) {
            log.error(e);
        }
    }
}
