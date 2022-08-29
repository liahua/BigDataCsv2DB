//package com.chenahua.transform;
//
//import org.apache.spark.sql.Dataset;
//import org.apache.spark.sql.Row;
//import org.apache.spark.sql.SparkSession;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//public class SparkTest {
//    @Test
//    void testSpark() {
//        System.setProperty("hadoop.home.dir","D:\\hadoop-3.2.0");
//        SparkSession spark = SparkSession.builder().master("local[2]").getOrCreate();
//        spark.read().option("header","true").csv("C:\\Users\\a8517\\IdeaProjects\\BigDataCsv2DB\\src\\main\\resources\\market_quote.csv").createOrReplaceTempView("market_quote");
//        Dataset<Row> rowDataset = spark.sql("select * from market_quote limit 10");
//        List<Row> rows = rowDataset.takeAsList(10);
//        rows.forEach(row -> System.out.println("row = " + row));
//
//    }
//}
