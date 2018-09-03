package com.ferris.service.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

trait HttpServer {

  val uri: Uri

  implicit val mat: ActorMaterializer
  implicit val system: ActorSystem

  def sendGetRequest(requestUri: Uri): Future[HttpResponse]

  def sendPostRequest(requestUri: Uri, entity: RequestEntity): Future[HttpResponse]

  def sendPutRequest(requestUri: Uri, entity: RequestEntity): Future[HttpResponse]

  def sendPutRequest(requestUri: Uri): Future[HttpResponse]

  def sendDeleteRequest(requestUri: Uri): Future[HttpResponse]

}

case class Envelope[T](status: String, data: T)

class DefaultServer(val uri: Uri)(implicit val mat: ActorMaterializer, val system: ActorSystem) extends HttpServer with LazyLogging {

  val basePath: Path = uri.path

  val http = Http()

  override def sendGetRequest(requestUri: Uri): Future[HttpResponse] = {
    val finalUri = resolveUri(requestUri)
    http.singleRequest(HttpRequest(method = HttpMethods.GET, uri = finalUri))
  }

  override def sendPostRequest(requestUri: Uri, entity: RequestEntity): Future[HttpResponse] = {
    val finalUri = resolveUri(requestUri)
    http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = finalUri, entity = entity))
  }

  override def sendPutRequest(requestUri: Uri, entity: RequestEntity): Future[HttpResponse] = {
    val finalUri = resolveUri(requestUri)
    http.singleRequest(HttpRequest(method = HttpMethods.PUT, uri = finalUri, entity = entity))
  }

  override def sendPutRequest(requestUri: Uri): Future[HttpResponse] = {
    val finalUri = resolveUri(requestUri)
    http.singleRequest(HttpRequest(method = HttpMethods.PUT, uri = finalUri))
  }

  override def sendDeleteRequest(requestUri: Uri): Future[HttpResponse] = {
    val finalUri = resolveUri(requestUri)
    http.singleRequest(HttpRequest(method = HttpMethods.DELETE, uri = finalUri))
  }

  private def resolveUri(req: Uri) = req.withPath(basePath ++ req.path).resolvedAgainst(uri)

  system.registerOnTermination(logger.info("Actor system terminated"))
}
