package com.chenahua.transform;

import com.chenahua.transform.service.TransformService;
import com.chenahua.transform.service.impl.ReadTask;
import com.google.common.primitives.Longs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
class TransformApplicationTests {

    @Autowired
    private TransformService transformService;

    @Test
    void contextLoads() throws SQLException, IOException, InterruptedException, ExecutionException {
//        ArrayList<ReadTask> readTasks = transformService.providerMulti();
//        System.out.println(readTasks);
        transformService.transformCSV(true);
        ForkJoinPool forkJoinPool = new ForkJoinPool(5);
        ArrayList<Integer> integers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            integers.add(i);
        }
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
