package dev.fp4.echo

import cats.effect.{Effect, IO, Timer}
import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.ExecutionContext

object HelloWorldServer extends StreamApp[IO] {
import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) =
    ServerStream.stream[IO]
}

object ServerStream {

  def helloWorldService[F[_]: Effect](implicit ec: ExecutionContext) = {
    implicit val timer = Timer.derive[F]
    new EchoService[F].service
  }


  def stream[F[_]: Effect](implicit ec: ExecutionContext) =
    BlazeBuilder[F]
      .bindHttp(Option(System.getenv("PORT")).map(_.toInt).getOrElse(8080), "0.0.0.0")
      .mountService(helloWorldService, "/")
      .serve
}
