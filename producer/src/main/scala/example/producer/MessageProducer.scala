package example.producer

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import example.api.{UserCreated, UserEvent, UserEventProto, UserUpdated}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import java.time.Instant
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object MessageProducer {
  def main(args: Array[String]): Unit = {
    ActorSystem(
      Behaviors.setup[Unit] { ctx =>
        implicit val system: ActorSystem[Void] = ctx.getSystem
        implicit val executionContext: ExecutionContextExecutor = system.executionContext

        val config = system.settings.config.getConfig("akka.kafka.producer")
        val producerSettings =
          ProducerSettings(config, new StringSerializer, new ByteArraySerializer)
            .withBootstrapServers(bootstrapServers = "localhost:9092")
        val topicName = UserEventProto.topicName
          .get(UserEventProto.scalaDescriptor.getOptions)
          .getOrElse("")

        val key = "1"

        val e1 = UserCreated(id = key, createdAt = Instant.now.toString)
        val e2 = UserUpdated(id = key, updatedAt = Instant.now.toString)
        val events: Seq[UserEvent] = Seq(e1, e2)

        val done: Future[Done] =
          Source(events)
            .map { e => e.asMessage.toByteArray }
            // 这里调用asMessage
            // 是因为UserEvent是一个one_of message，scalapb生成的代码会复杂些
            // 如果是一个简单的message可以省略这步
            .map(value => new ProducerRecord[String, Array[Byte]](topicName, key, value))
            .runWith(Producer.plainSink(producerSettings))

        done.onComplete {
          case Failure(exception) =>
            print(exception)
            system.terminate()
          case Success(value) =>
            print(value)
            system.terminate()
        }

        Behaviors.empty
      },
      "example-system"
    )
  }
}
