FROM gcr.io/spark-operator/spark:v2.4.5
RUN mkdir /opt/spark/custom-spark-job/
ADD build/libs/sample-spark-job-1.0-SNAPSHOT.jar /opt/spark/custom-spark-job/sample-spark-job.jar
ADD build/lib/*.jar $SPARK_HOME/jars/