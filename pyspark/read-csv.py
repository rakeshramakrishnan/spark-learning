import pyspark
import time

spark = pyspark.sql.SparkSession.builder.appName("read-csv-1").getOrCreate()

flightData = spark\
    .read\
    .option("inferSchema", "true")\
    .option("header", "true") \
    .csv("/Users/rakeshramakrishnan/Personal/Learning/Spark-The-Definitive-Guide/data/flight-data/csv/2015-summary.csv")

# print(flightData.take(3))
#
# flightData\
#     .sort("count")\
#     .explain()

# spark SQL
# flightData.createOrReplaceTempView("flight_data")
#
# maxCount = spark.sql("""
# SELECT DEST_COUNTRY_NAME, sum(count) as destination_total FROM flight_data
# GROUP BY DEST_COUNTRY_NAME
# ORDER BY sum(count) DESC
# LIMIT 5
# """)
#
# maxCount.show()

flightData \
    .groupBy("DEST_COUNTRY_NAME") \
    .sum("count") \
    .withColumnRenamed("sum(count)", "destination_total")\
    .sort("destination_total", ascending=False) \
    .limit(5) \
    .show()

time.sleep(100)