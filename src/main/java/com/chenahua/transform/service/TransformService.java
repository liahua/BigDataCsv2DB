package com.chenahua.transform.service;

import com.chenahua.transform.service.impl.ReadTask;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public interface TransformService {
    void transformCSV(boolean highPerformance) throws IOException, SQLException, InterruptedException, ExecutionException;

}
