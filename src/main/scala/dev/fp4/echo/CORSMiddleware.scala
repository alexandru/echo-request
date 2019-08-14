package dev.fp4.echo

import org.http4s._
import org.http4s.util.CaseInsensitiveString
import cats.effect._
import cats.data.Kleisli

object CORSMiddleware {
  /**
    * An HTTP middleware that adds CORS headers to requests.
    */
  def apply[F[_]](service: HttpService[F], header: Header)
    (implicit F: Sync[F]) = {

    Kleisli { req: Request[F] =>
      val allow = req.headers
        .get(CaseInsensitiveString("Origin"))
        .fold("*")(_.value)

      service(req).map { resp =>
        resp.putHeaders(
          Header("Access-Control-Allow-Methods", "GET, POST, OPTIONS"),
          Header("Access-Control-Allow-Credentials", "true"),
          Header("Access-Control-Allow-Origin", "*"),
          Header("Access-Control-Allow-Headers", "Content-Type, *")
        )
      }
    }
  }
}
