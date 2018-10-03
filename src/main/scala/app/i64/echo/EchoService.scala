package app.i64.echo

import cats.implicits._
import cats.effect.Effect
import cats.effect.Timer
import io.circe.syntax._
import io.circe.{Json, Printer}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.duration._

class EchoService[F[_]: Effect](implicit timer: Timer[F])
  extends Http4sDsl[F] {

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

      case request @ GET -> Root / "timeout" / Duration(d, unit)  =>
        val timespan = FiniteDuration(d, unit)
        simulateTimeout(timespan)
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
        "geoip" -> ip.map(getGeoIPInfo).getOrElse(Json.Null)
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

  def simulateTimeout(ts: FiniteDuration): F[Response[F]] = {    
    for {
      start <- timer.clockMonotonic(MILLISECONDS)
      _ <- timer.sleep(ts)
      now <- timer.clockMonotonic(MILLISECONDS)
      r <- Response[F](Status.fromInt(408).right.get).withBody(
        Json.obj(
          "status" -> "408 Timeout".asJson,
          "slept" -> (now - start).asJson
        ))
    } yield r
  }
}