package spark.examples.streaming;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class KafkaWordCount {
 public static void main(String[] args) throws Exception {
   SparkSession spark = SparkSession
     .builder()
     .appName("KafkaWordCount")
     .getOrCreate();
   JavaSparkContext jc = new JavaSparkContext(spark.sparkContext());

   JavaStreamingContext jsc = new JavaStreamingContext(jc, Durations.seconds(30));

   Map<String, Object> kafkaParams = new HashMap<>();
   kafkaParams.put("bootstrap.servers", "kafka.default.svc.cluster.local:9092");
   kafkaParams.put("key.deserializer", StringDeserializer.class);
   kafkaParams.put("value.deserializer", StringDeserializer.class);
   kafkaParams.put("group.id", "testing-1");
   kafkaParams.put("auto.offset.reset", "latest");
   kafkaParams.put("enable.auto.commit", false);

   Collection<String> topics = Arrays.asList("word-count-input");

   JavaInputDStream<ConsumerRecord<String, String>> stream =
     KafkaUtils.createDirectStream(
       jsc,
       LocationStrategies.PreferConsistent(),
       ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams)
     );

   JavaDStream<String> lines = stream.map(record -> (String) record.value());

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
