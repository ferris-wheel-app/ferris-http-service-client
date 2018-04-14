package com.ferris.service.client

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Path._
import akka.stream.ActorMaterializer
import com.ferris.json.FerrisJsonSupport
import fommil.sjs.FamilyFormats
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Message(uuid: UUID, content: String)

trait TestJsonSupport extends FerrisJsonSupport {
  implicit val messageFormat: RootJsonFormat[Message] = jsonFormat2(Message)
}

class TestClient(val server: HttpServer, implicit val mat: ActorMaterializer) extends ServiceClient with DefaultJsonProtocol with TestJsonSupport {

  def this(server: HttpServer) = this(server, server.mat)

  def getMessage(id: UUID): Future[Message] = makeGetRequest(Uri(path = /("messages")/id.toString))

  def badMessage: Future[Message] = makeGetRequest(Uri(path = /("bad")))

  def createMessage(message: Message): Future[Message] = makePostRequest(Uri(path = /("messages")), message)

  def updateMessage(id: UUID, message: Message): Future[Message] = makePutRequest(Uri(path = /("messages")/id.toString), message)

  def deleteMessage(id: UUID): Future[String] = makeDeleteRequest[String](Uri(path = /("messages")/id.toString))
}

class ServiceClientTest extends FunSpec with Matchers with ScalaFutures with MockitoSugar with DefaultJsonProtocol with TestJsonSupport with FamilyFormats {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val mockServer: HttpServer = mock[HttpServer]
  val client = new TestClient(mockServer)

  val message = Message(uuid = UUID.randomUUID(), content = "Hello world!")

  when(mockServer.uri).thenReturn(Uri("http://mock-service.com/api"))

  describe("a service client") {
    it("should be able to make GET requests to existing resources") {
      val id = UUID.randomUUID

      when(mockServer.sendGetRequest(s"/messages/$id"))
        .thenReturn(Future.successful(HttpResponse(entity = Marshal(Envelope("OK", message)).to[ResponseEntity].futureValue)))

      whenReady(client.getMessage(id)) { response =>
        response shouldBe message
      }
    }

    it("should respond with a failed future if the resource is not available") {
      when(mockServer.sendGetRequest("/bad"))
        .thenReturn(Future.successful(HttpResponse(status = StatusCodes.InternalServerError)))

      whenReady(client.badMessage.failed) { exception =>
        exception.isInstanceOf[ServiceClientException] shouldBe true
        val serviceClientException = exception.asInstanceOf[ServiceClientException]
        assert(serviceClientException.message.startsWith("Unexpected response from server http://mock-service.com/api/bad [ status code 500 ]: There was an internal server error."))
        serviceClientException.httpStatus shouldBe 500
      }
    }

    it("should be able to make POST requests to existing resources") {
      when(mockServer.sendPostRequest(any(), any()))
        .thenReturn(Future.successful(HttpResponse(status = StatusCodes.Created, entity = Marshal(Envelope("OK", message)).to[ResponseEntity].futureValue)))

      whenReady(client.createMessage(message)) { response =>
        response shouldBe message
      }
    }

    it("should be able to make PUT requests to existing resources") {
      val id = UUID.randomUUID

      when(mockServer.sendPutRequest(eqTo(s"/messages/$id"), any()))
        .thenReturn(Future.successful(HttpResponse(status = StatusCodes.OK, entity = Marshal(Envelope("OK", message)).to[ResponseEntity].futureValue)))

      whenReady(client.updateMessage(id, message)) { response =>
        response shouldBe message
      }
    }

    it("should be able to make DELETE requests to existing resources") {
      val id = UUID.randomUUID

      when(mockServer.sendDeleteRequest(s"/messages/$id"))
        .thenReturn(Future.successful(HttpResponse(status = StatusCodes.OK, entity = Marshal(Envelope("OK", "Whoosh!")).to[ResponseEntity].futureValue)))

      whenReady(client.deleteMessage(id)) { r =>
        r shouldBe "Whoosh!"
      }
    }
  }

  describe("a server") {
    it("should make requests to the correct path") {
      val mockHttp = mock[HttpExt]
      val s = new DefaultServer("http://localhost:8080/api") {
        override val http: HttpExt = mockHttp
        when(http.singleRequest(any(), any(), any(), any())(any())).thenReturn(Future.successful(HttpResponse(StatusCodes.OK)))
      }
      s.sendGetRequest("/messages")
      val captor = ArgumentCaptor.forClass(classOf[HttpRequest])
      verify(mockHttp, times(1)).singleRequest(captor.capture(), any(), any(), any())(any())
      captor.getValue.uri shouldBe Uri("http://localhost:8080/api/messages")
    }
  }
}
