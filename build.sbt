val Http4sVersion = "0.18.24"
val LogbackVersion = "1.2.3"
val GeoIP2Version = "2.12.0"

lazy val root = (project in file("."))
  .settings(
    organization := "app.i64",
    name := "echo",
    version := "0.2.0",
    scalaVersion := "2.12.8",
    scalacOptions += "-deprecation",
    libraryDependencies ++= Seq(
      "org.http4s"        %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"        %% "http4s-circe"        % Http4sVersion,
      "org.http4s"        %% "http4s-dsl"          % Http4sVersion,
      "org.http4s"        %% "http4s-twirl"        % Http4sVersion,
      "ch.qos.logback"     % "logback-classic"     % LogbackVersion,
      "com.maxmind.geoip2" % "geoip2" % GeoIP2Version
    ),

    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),

    // For Heroku deployment
    herokuAppName in Compile := "echorequest"
  )
  .enablePlugins(JavaAppPackaging)

