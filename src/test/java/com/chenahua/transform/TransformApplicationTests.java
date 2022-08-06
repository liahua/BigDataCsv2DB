package com.chenahua.transform;

import com.chenahua.transform.service.TransformService;
import com.chenahua.transform.service.impl.ReadTask;
import com.google.common.primitives.Longs;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.AnnotationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@EnableAspectJAutoProxy
@SpringBootTest
class TransformApplicationTests {

    @Autowired
    private TransformService transformService;

    @Test
    void contextLoads() throws SQLException, IOException, InterruptedException, ExecutionException {
//        ArrayList<ReadTask> readTasks = transformService.providerMulti();
//        System.out.pr0intln(readTasks);


        int bbc=333333;
        transformService.transformCSV(true);
        ForkJoinPool forkJoinPool = new ForkJoinPool(5);
        ArrayList<Integer> integers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            integers.add(i);
        }
        int a=1;
        int b=2;
    }

    @Test
    void testForkJoin() {
        ForkJoinPool forkJoinPool = new ForkJoinPool(5);
        long[] arrays = new long[400042];


        long[] longs = Longs.toArray(Arrays.stream(arrays).parallel()
                .map(operand -> (long) (new Random().nextInt(10000)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));

        forkJoinPool.invoke(new SortTask(longs, 0, longs.length - 1));

        System.out.println("longs = " + longs);
    }

}
