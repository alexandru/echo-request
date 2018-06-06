package app.i64.echo

import java.net.InetAddress

import cats.effect.Effect
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.record.{City, Country, Location}
import io.circe.Json
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpService, Request}
import org.slf4j.{Logger, LoggerFactory}
import scala.util.control.NonFatal

class EchoService[F[_]: Effect] extends Http4sDsl[F] {
  lazy val logger: Logger = LoggerFactory.getLogger("MainService")

  lazy val geoIP: DatabaseReader = new DatabaseReader
    .Builder(getClass.getResourceAsStream("/GeoLite2/GeoLite2-City.mmdb"))
    .build()

  val service: HttpService[F] = {
    HttpService[F] {
      case request @ GET -> Root =>
        val ip = request.params.get("ip") match {
          case ip @ Some(value) if publicIP(value) => ip
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
          "headers" -> Json.obj(headers:_*)
        ))
    }
  }

  def publicIP(ip: String): Boolean = {
    try {
      val parsed = InetAddress.getByName(ip)
      !(parsed.isLoopbackAddress || parsed.isSiteLocalAddress)
    } catch {
      case NonFatal(_) => false
    }
  }

  def getHeader(request: Request[F], name: String): Option[String] = {
    request.headers.get(CaseInsensitiveString(name)).map(_.value)
  }

  def getRealIP(request: Request[F]): Option[String] =
    request.headers.get(CaseInsensitiveString("X-Forwarded-For")) match {
      case Some(header) =>
        header.value.split("\\s*,\\s*").find(publicIP) match {
          case ip @ Some(_) => ip
          case None =>
            request.remoteAddr
        }
      case None =>
        request.remoteAddr
    }

  def getGeoIPInfo(ip: String): Json = {
    def country(ref: Country) = {
      if (ref != null && ref.getIsoCode != null)
        Json.obj(
          "isoCode" -> Option(ref.getIsoCode).fold(Json.Null)(Json.fromString),
          "name" -> Option(ref.getName).fold(Json.Null)(Json.fromString),
          "isInEuropeanUnion" -> Json.fromBoolean(ref.isInEuropeanUnion)
        )
      else
        Json.Null
    }

    def city(ref: City) =
      if (ref != null && ref.getName != null)
        Json.obj(
          "name" -> Option(ref.getName).fold(Json.Null)(Json.fromString)
        )
      else
        Json.Null

    def location(ref: Location) =
      if (ref != null)
        Json.obj(
          "latitude" -> Json.fromDoubleOrNull(ref.getLatitude),
          "longitude" -> Json.fromDoubleOrNull(ref.getLongitude)
        )
      else
        Json.Null

    try {
      val address = InetAddress.getByName(ip)
      if (address.isSiteLocalAddress || address.isLoopbackAddress)
        Json.Null
      else {
        val info = geoIP.city(address)
        Json.obj(
          "country" -> country(info.getCountry),
          "registeredCountry" -> country(info.getRegisteredCountry),
          "representedCountry" -> country(info.getRepresentedCountry),
          "city" -> city(info.getCity),
          "location" -> location(info.getLocation)
        )
      }
    } catch {
      case NonFatal(e) =>
        logger.error("Unexpected error", e)
        Json.Null
    }
  }
}
