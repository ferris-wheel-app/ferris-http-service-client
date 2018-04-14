package com.ferris.service.client

import akka.http.scaladsl.model._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json._

import scala.concurrent.Future

class ServiceClientException(val httpStatus: Int, val message: String) extends RuntimeException(message)

trait ServiceClient {
  this: BasicFormats with ProductFormats =>

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import scala.concurrent.ExecutionContext.Implicits.global

  val server: HttpServer
  implicit val mat: ActorMaterializer

  private implicit def envFormat[T](implicit jf: JsonFormat[T]) = jsonFormat2(Envelope[T])

  protected def checkSuccess(response: HttpResponse, path: Uri): HttpResponse = response match {
    case r if r.status.isSuccess() => r
    case bad =>
      val errorCode = bad.status.intValue()
      val resolvedUri = server.uri.toString() + path.toString()
      throw new ServiceClientException(errorCode, s"Unexpected response from server $resolvedUri [ status code $errorCode ]: ${bad.status.defaultMessage()}\n$response")
  }

  protected def makeGetRequest[T](path: Uri)(implicit f: JsonFormat[T]): Future[T] = {
    for {
      resp <- server.sendGetRequest(path) map { checkSuccess(_, path) }
      data <- Unmarshal(resp.entity).to[Envelope[T]]
    } yield data.data
  }

  protected def makePutRequest[T, U](path: Uri, obj: T)(implicit tf: JsonFormat[T], uf: JsonFormat[U]): Future[U] = {
    for {
      ent <- Marshal(obj.toJson).to[RequestEntity]
      resp <- server.sendPutRequest(path, ent) map { checkSuccess(_, path) }
      data <- Unmarshal(resp.entity).to[Envelope[U]]
    } yield data.data
  }

  protected def makePostRequest[T, U](path: Uri, obj: T)(implicit tf: JsonFormat[T], uf: JsonFormat[U]): Future[U] = {
    for {
      ent <- Marshal(obj.toJson).to[RequestEntity]
      resp <- server.sendPostRequest(path, ent) map { checkSuccess(_, path) }
      data <- Unmarshal(resp.entity).to[Envelope[U]]
    } yield data.data
  }

  protected def makeDeleteRequest[T](path: Uri)(implicit f: JsonFormat[T]): Future[T] = {
    for {
      resp <- server.sendDeleteRequest(path) map { checkSuccess(_, path) }
      data <- Unmarshal(resp.entity).to[Envelope[T]]
    } yield data.data
  }
}
