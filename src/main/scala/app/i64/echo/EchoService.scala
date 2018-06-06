package app.i64.echo

import cats.effect.Effect
import io.circe.syntax._
import io.circe.{Json, Printer}
import org.http4s.{HttpService, Request, Response, StaticFile}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.slf4j.{Logger, LoggerFactory}

class EchoService[F[_]: Effect] extends Http4sDsl[F] {

  import IPUtils._

  val logger: Logger = LoggerFactory.getLogger("MainService")
  val circe = CirceInstances.withPrinter(Printer.spaces2)

  import circe._

  val service: HttpService[F] =
    HttpService[F] {
      case request @ GET -> Root =>
        StaticFile.fromResource("/public/index.html", Some(request))
          .getOrElseF(NotFound())

      case request @ GET -> Root / "public" / path =>
        StaticFile.fromResource(s"/public/$path", Some(request))
          .getOrElseF(NotFound())

      case request @ GET -> Root / "favicon.ico" =>
        StaticFile.fromResource("/public/favicon.ico", Some(request))
          .getOrElseF(NotFound())

      case request @ GET -> Root / "all" =>
        getAll(request)

      case request @ GET -> Root / "ip" =>
        getIP(request)

      case request @ GET -> Root / "geoip" =>
        getGeoIP(request)
    }

  def getAll(request: Request[F]): F[Response[F]] = {
    val ip = request.params.get("ip") match {
      case ip@Some(value) if publicIP(value) => ip
      case _ => getRealIP(request)
    }

    val headers = request.headers.toList.map { h =>
      (h.name.value, Json.fromString(h.value))
    }

    Ok(Json.obj(
      "user" -> Json.obj(
        "ip" -> ip.asJson,
        "forwardedFor" -> getHeader(request, "X-Forwarded-For").asJson,
        "via" -> getHeader(request, "Via").asJson,
        "agent" -> getHeader(request, "User-Agent").asJson,
        "geoIP2" -> ip.map(getGeoIPInfo).getOrElse(Json.Null)
      ),
      "headers" -> Json.obj(headers: _*)
    ))
  }

  def getIP(request: Request[F]): F[Response[F]] =
    getRealIP(request) match {
      case Some(ip) => Ok(ip)
      case None => NoContent()
    }

  def getGeoIP(request: Request[F]): F[Response[F]] = {
    val ip = request.params.get("ip") match {
      case ip@Some(value) if publicIP(value) => ip
      case _ => getRealIP(request)
    }
    Ok(ip.map(getGeoIPInfo).getOrElse(Json.Null))
  }
}