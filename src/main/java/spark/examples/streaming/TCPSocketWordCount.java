package spark.examples.streaming;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Tuple2;

import java.util.Arrays;

public class TCPSocketWordCount {
 public static void main(String[] args) throws Exception {
   SparkSession spark = SparkSession
     .builder()
     .appName("TCPSockWordCount")
     .getOrCreate();
   JavaSparkContext jc = new JavaSparkContext(spark.sparkContext());

   JavaStreamingContext jsc = new JavaStreamingContext(jc, Durations.seconds(30));

   // Create a DStream that will connect to hostname:port, like localhost:9999
   JavaReceiverInputDStream<String> lines = jsc.socketTextStream("localhost", 9999);

   // Split each line into words
   JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(x.split(" ")).iterator());

   // Count each word in each batch
   JavaPairDStream<String, Integer> pairs = words.mapToPair(s -> new Tuple2<>(s, 1));
   JavaPairDStream<String, Integer> wordCounts = pairs.reduceByKey((i1, i2) -> i1 + i2);

   // Print the first ten elements of each RDD generated in this DStream to the console
   wordCounts.print();

   jsc.start();              // Start the computation
   jsc.awaitTermination();
 }
}
