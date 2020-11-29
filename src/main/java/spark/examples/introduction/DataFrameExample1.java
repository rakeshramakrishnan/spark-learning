package spark.examples.introduction;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

public class DataFrameExample1 {
  public static void main(String[] args) {
    SparkSession spark = SparkSession
      .builder()
      .appName("example-1")
      .getOrCreate();

    Dataset flightData = spark
      .read()
      .option("inferSchema", "true")
      .option("header", "true")
      .csv("/Users/rakeshramakrishnan/Personal/Learning/Spark-The-Definitive-Guide/data/flight-data/csv/2015-summary.csv");

    flightData
    .groupBy("DEST_COUNTRY_NAME")
    .sum("count")
    .withColumnRenamed("sum(count)", "destination_total")
    .sort(functions.desc("destination_total"))
    .limit(5)
    .show();
  }
}
