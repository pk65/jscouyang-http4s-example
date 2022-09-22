val Http4sVersion = "0.21.6"
val MunitVersion = "1.0.0-M6"
val TwitterVersion = "20.8.0"
val DoobieVersion = "0.13.3"
val CirceVersion = "0.13.0"
val MysqlVersion = "8.0.30"
val PureConfigVersion = "0.17.1"

ThisBuild / scalaVersion := "2.13.8"
lazy val root = (project in file("."))
  .settings(
    organization := "com.your.domain",
    name := "jscouyang-http4s",
    maintainer := "pawel.kuszynski@nokia.com",
    scalacOptions ++= {
      Seq(
        // "-encoding", "UTF-8",
        "-feature",
        "-language:implicitConversions",
        "-target:jvm-1.8",
        // "-Ylog-classpath",
        // "-new-syntax",
        // "-source:future-migration",
        // "-rewrite",
        // "-explain", "explain-types",
        // "-Xfatal-warnings",
        // "-Yexplicit-nulls",
        // "-Ysafe-init",
        "-deprecation"
      )
    },
    libraryDependencies ++= Seq(
      "org.http4s"                 %% "http4s-blaze-client"            % Http4sVersion withJavadoc(),
      "org.http4s"                 %% "http4s-circe"                   % Http4sVersion withJavadoc(),
      "org.http4s"                 %% "http4s-dsl"                     % Http4sVersion withJavadoc(),
      "org.http4s"                 %% "http4s-finagle"                 % "0.21.25-21.6.0",
      "io.circe"                   %% "circe-generic"                  % CirceVersion withJavadoc(),
      "io.circe"                   %% "circe-literal"                  % CirceVersion withJavadoc(),
      "mysql"                       % "mysql-connector-java"           % MysqlVersion,
//      "org.tpolecat"               %% "doobie-postgres"                % DoobieVersion,
      "org.tpolecat"               %% "doobie-quill"                   % DoobieVersion withJavadoc(),
      "org.tpolecat"               %% "doobie-hikari"                  % DoobieVersion withJavadoc(),
      "us.oyanglul"                %% "finagle-prometheus"             % "0.2.0" withJavadoc(),
      "com.twitter"                %% "twitter-server-logback-classic" % "21.6.0",
      "io.zipkin.finagle2"         %% "zipkin-finagle-http"            % "21.6.0" withJavadoc(),
      "com.github.pureconfig"      %% "pureconfig"                     % PureConfigVersion withJavadoc(),
      "ch.qos.logback"              % "logback-classic"                % "1.2.11",
      "org.scalameta"              %% "munit"                          % MunitVersion % Test,
      "org.scalameta"              %% "munit-scalacheck"               % MunitVersion % Test,
      "org.mockito"                %% "mockito-scala"                  % "1.17.12"     % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14"      % "1.2.5"      % Test,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin(scalafixSemanticdb),
    addCommandAlias(
      "rmUnused",
      """set scalacOptions -= "-Xfatal-warnings";scalafix RemoveUnused;set scalacOptions += "-Xfatal-warnings"""",
    ),
  )

lazy val db = project
  .settings(
    organization := "com.your.domain",
    name := "jscouyang-http4s-db-migration",
    libraryDependencies ++= Seq(
      "mysql"         % "mysql-connector-java" % MysqlVersion,
      "org.flywaydb"  % "flyway-mysql"    % "9.2.0",
      "org.tpolecat" %% "doobie-core"     % DoobieVersion,
//      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "com.github.pureconfig"      %% "pureconfig"                     % PureConfigVersion withJavadoc(),
    ),
  )

enablePlugins(JavaAppPackaging)
