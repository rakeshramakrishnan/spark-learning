package spark.examples.computation;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

import java.util.ArrayList;
import java.util.List;

public class PiEstimation {
  public static void main(String[] args) throws Exception {
    SparkSession spark = SparkSession
      .builder()
      .appName("PiEstimation")
      .getOrCreate();

    JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());

    int slices = 8;
    int n = 100000 * slices;
    List<Integer> l = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      l.add(i);
    }

    JavaRDD<Integer> dataSet = jsc.parallelize(l, slices);

    int count = dataSet.map(integer -> {
      double x = Math.random() * 2 - 1;
      double y = Math.random() * 2 - 1;
      return (x * x + y * y <= 1) ? 1 : 0;
    }).reduce((integer, integer2) -> integer + integer2);

    System.out.println("Computed Pi!!! It is: " + 4.0 * count / n);
    System.out.println("About to sleep");

    Thread.sleep(300000);
    System.out.println("Sleep done");
    spark.stop();
  }
}
