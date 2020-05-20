package spark.examples.streaming;

import com.google.common.io.Files;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.*;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.apache.spark.util.LongAccumulator;
import scala.Tuple2;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

class JavaWordBlacklist {

  private static volatile Broadcast<List<String>> instance = null;

  public static Broadcast<List<String>> getInstance(JavaSparkContext jsc) {
    if (instance == null) {
      synchronized (JavaWordBlacklist.class) {
        if (instance == null) {
          List<String> wordBlacklist = Arrays.asList("a", "b", "c");
          instance = jsc.broadcast(wordBlacklist);
        }
      }
    }
    return instance;
  }
}

class JavaDroppedWordsCounter {

  private static volatile LongAccumulator instance = null;

  public static LongAccumulator getInstance(JavaSparkContext jsc) {
    if (instance == null) {
      synchronized (JavaDroppedWordsCounter.class) {
        if (instance == null) {
          instance = jsc.sc().longAccumulator("WordsInBlacklistCounter");
        }
      }
    }
    return instance;
  }
}

public final class RecoverableKafkaWordCount {
  private static final Pattern SPACE = Pattern.compile(" ");

  private static JavaStreamingContext createContext(String checkpointDirectory, String outputPath) {

    // If you do not see this printed, that means the StreamingContext has been loaded
    // from the new checkpoint
    System.out.println("Creating new context");
    File outputFile = new File(outputPath);
    if (outputFile.exists()) {
      outputFile.delete();
    }
    SparkConf sparkConf = new SparkConf().setAppName("JavaRecoverableNetworkWordCount");
    JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(30));
    ssc.checkpoint(checkpointDirectory);

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
            ssc,
            LocationStrategies.PreferConsistent(),
            ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams)
        );

    JavaDStream<String> lines = stream.map(record -> (String) record.value());
    JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(SPACE.split(x)).iterator());
    JavaPairDStream<String, Integer> wordCounts = words.mapToPair(s -> new Tuple2<>(s, 1))
        .reduceByKey((i1, i2) -> i1 + i2);

    wordCounts.foreachRDD((rdd, time) -> {
      // Get or register the blacklist Broadcast
      Broadcast<List<String>> blacklist =
          JavaWordBlacklist.getInstance(new JavaSparkContext(rdd.context()));
      // Get or register the droppedWordsCounter Accumulator
      LongAccumulator droppedWordsCounter =
          JavaDroppedWordsCounter.getInstance(new JavaSparkContext(rdd.context()));
      // Use blacklist to drop words and use droppedWordsCounter to count them
      String counts = rdd.filter(wordCount -> {
        if (blacklist.value().contains(wordCount._1())) {
          droppedWordsCounter.add(wordCount._2());
          return false;
        } else {
          return true;
        }
      }).collect().toString();
      String output = "Counts at time " + time + " " + counts;
      System.out.println(output);
      System.out.println("Dropped " + droppedWordsCounter.value() + " word(s) totally");
      System.out.println("Appending to " + outputFile.getAbsolutePath());
      Files.append(output + "\n", outputFile, Charset.defaultCharset());
    });

    return ssc;
  }

  public static void main(String[] args) throws Exception {
    String checkpointDirectory = "/data";
    String outputPath = "/data/output";

    JavaStreamingContext ssc = JavaStreamingContext.getOrCreate(checkpointDirectory,
        () -> createContext(checkpointDirectory, outputPath));
    ssc.start();
    ssc.awaitTermination();
  }
}
