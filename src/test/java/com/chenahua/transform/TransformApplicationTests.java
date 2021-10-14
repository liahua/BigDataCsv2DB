package com.chenahua.transform;

import com.chenahua.transform.service.TransformService;
import com.chenahua.transform.service.impl.ReadTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@SpringBootTest
class TransformApplicationTests {

    @Autowired
    private TransformService transformService;

    @Test
    void contextLoads() throws SQLException, IOException, InterruptedException, ExecutionException {
//        ArrayList<ReadTask> readTasks = transformService.providerMulti();
//        System.out.println(readTasks);
        transformService.transformCSV(true);
    }


}
