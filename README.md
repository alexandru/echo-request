# Echo HTTP Server

Mirrors back the request's headers and user info, also doing geo-ip
via [Maxmind's GeoLite2](https://dev.maxmind.com/geoip/geoip2/geolite2/).

**[echo.fp4.dev](https://echo.fp4.dev)**

To deploy on Heroku:

```
sbt stage deployHeroku
```
