# Echo HTTP Server

Mirrors back the request's headers and user info, also doing geo-ip
via [Maxmind's GeoLite2](https://dev.maxmind.com/geoip/geoip2/geolite2/).

**[echo.i64.app](https://echo.i64.app)**

To deploy on Heroku:

```
sbt stage deployHeroku
```