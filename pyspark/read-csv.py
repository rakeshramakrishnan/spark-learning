import pyspark

spark = pyspark.sql.SparkSession.builder.appName("read-csv-1").getOrCreate()

flightData = spark\
    .read\
    .option("inferSchema", "true")\
    .option("header", "true") \
    .csv("/Users/rakeshramakrishnan/Personal/Learning/Spark-The-Definitive-Guide/data/flight-data/csv/2015-summary.csv")

print(flightData.take(3))

flightData\
    .sort("count")\
    .explain()