package spark.examples.introduction;

import org.apache.spark.sql.SparkSession;

import java.util.List;

public class DataFrameExample1 {
  public static void main() {
    SparkSession spark = SparkSession
      .builder()
      .appName("example-1")
      .getOrCreate();

    List df = spark
      .range(100)
      .toDF()
      .collectAsList();

    System.out.println(String.format("Hello world", df.toString()));
  }
}
