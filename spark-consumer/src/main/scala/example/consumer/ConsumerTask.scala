package example.consumer

import example.api.{UserEventMessage, UserEventProto}
import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import scalapb.spark.Implicits._
import scalapb.spark.ProtoSQL

object ConsumerTask {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .appName("kafka-streaming-consumer")
      .master("local[2]")
      .getOrCreate()
    import spark.implicits.StringToColumn

    val topicName = UserEventProto.topicName
      .get(UserEventProto.scalaDescriptor.getOptions)
      .getOrElse("")

    val dataFrame: DataFrame = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:8090")
      .option("subscribe", topicName)
      .option("startingOffsets", "earliest")
      .load()

    // https://scalapb.github.io/docs/sparksql/
    val parseUserEvent: Column => Column = ProtoSQL.udf { bytes: Array[Byte] =>
      UserEventMessage.parseFrom(bytes)
      // 这里转换后其实没法直接match，得先调用toUserEvent
      // 但UserEvent本身没有spark Encoder，转换后编译不通过
      // 所以先不调，直到真正需要操作UserEvent的时候，比如更新数据库
    }

    // row: key, value
    dataFrame
      .select(parseUserEvent($"value"))
      .map { r => r.getAs[UserEventMessage](0) }
      .show()
  }
}
