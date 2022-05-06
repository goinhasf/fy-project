import com.typesafe.sbt.packager.docker.CmdLike
import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
import scala.sys.process.Process
import sbt.io.RichFile

lazy val compileCompose =
  taskKey[Int]("Compile and update docker-compose")

lazy val compileBuild = taskKey[Int]("Runs docker-compose build")

lazy val dockerComposeBuild = Def.task {
  Process(
    s"docker-compose -f ${(ThisBuild / baseDirectory).value.toPath()}/backend/docker-compose.yml build"
  ).!
}

lazy val dockerComposeUp = Def.task {
  Process(
    s"docker-compose -f ${(ThisBuild / baseDirectory).value.toPath()}/backend/docker-compose.yml up -d"
  ).!
}

lazy val allProjects = Seq(
  frontend,
  backend,
  fileUploadService,
  authorizationService,
  playApiClients
).map(proj => (proj / Compile / compile).toTask)

def getCompileTasks = Def.sequential(allProjects)

lazy val app = project
  .in(file("."))
  .aggregate(
    frontend,
    backend,
    fileUploadService,
    authorizationService,
    playApiClients
  )
  .settings(
    (ivyLoggingLevel := UpdateLogging.Quiet),
    compileBuild := Def
      .sequential(
        Compile / compile,
        Docker / publishLocal,
        dockerComposeBuild,
        dockerComposeUp
      )
      .value
  )

lazy val frontend = project
  .in(file("frontend"))
  .enablePlugins(ScalaJSBundlerPlugin)
  .settings(sharedSettings)
  .settings(frontendSettings)
  .dependsOn(shared.js)

lazy val backend = project
  .in(file("backend"))
  .enablePlugins(
    WebScalaJSBundlerPlugin,
    PlayScala,
    DockerPlugin,
    AshScriptPlugin
  )
  .settings(sharedSettings)
  .settings(backendSettings)
  .dependsOn(playApiClients)
  .dependsOn(shared.jvm)

lazy val ngnix = project
  .in(file("nginx"))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(name := "api-gateway", version := "0.1")
  .settings(dockerSettings(8080))

lazy val fileUploadService = project
  .in(file("file-upload-service"))
  .enablePlugins(
    PlayScala,
    SbtWeb,
    DockerPlugin,
    JavaAppPackaging,
    AshScriptPlugin
  )
  .settings(fileUploadServiceSettings, sharedSettings)
  .dependsOn(shared.jvm)
  .dependsOn(playApiClients)

lazy val authorizationService = project
  .in(file("authorization-service"))
  .settings(authorizationServiceSettings, sharedSettings)
  .dependsOn(shared.jvm)
  .enablePlugins(DockerPlugin, JavaAppPackaging, AshScriptPlugin)

lazy val performanceTesting = project
  .in(file("performance-testing"))
  .settings(
    sharedSettings,
    scalacOptions := Seq(
      "-encoding",
      "UTF-8",
      "-target:jvm-1.8",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-language:postfixOps"
    ),
    libraryDependencies += "io.gatling.highcharts"         % "gatling-charts-highcharts" % "3.5.0"  % "test,it",
    libraryDependencies += "io.gatling"                    % "gatling-test-framework"    % "3.5.0"  % "test,it",
    libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.12.2" % "test,it"
  )
  .dependsOn(shared.jvm)
  .enablePlugins(GatlingPlugin)
lazy val shared = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(sharedSettings)
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "4.2.0",
      JwtNative,
      Endpoints4sPlayServer,
      Endpoints4sPlayClient.value
    )
  )
  .jsSettings(
    libraryDependencies +=
      "ru.pavkin" %%% "scala-js-momentjs" % "0.10.5"
  )

lazy val playApiClients = project
  .settings(
    playApiClientsSettings,
    sharedSettings
  )
  .dependsOn(shared.jvm)

lazy val playApiClientsSettings = Seq(
  name := "play-api-clients",
  libraryDependencies ++= Seq(
    ws,
    guice,
    Endpoints4sPlayClient.value,
    JwtNative
  )
)

lazy val backendSettings = {
  Seq(
    name := "society-management",
    scalaJSProjects := Seq(frontend),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    Compile / compile := (Compile / compile).dependsOn(scalaJSPipeline).value,
    compileCompose := Def
      .sequential(
        Compile / compile,
        Docker / publishLocal,
        dockerComposeUp
      )
      .value,
    javaOptions in Test += "-Dconfig.file=test/conf/application.test.conf",
    javaOptions in Test += "-Dwebdriver.gecko.driver=/home/asavy/Applications/geckodriver",
    libraryDependencies ++= Seq(
      ws,
      guice,
      ScalaMock.value,
      Endpoints4sPlayServer,
      Endpoints4sPlayClient.value,
      "com.vmunier"       %% "scalajs-scripts" % "1.1.4",
      "com.lihaoyi"      %%% "scalatags"       % "0.9.2",
      "org.scalatestplus" %% "selenium-3-141"  % "3.2.7.0" % "test",
      JwtNative
    )
  ) ++ dockerSettings(9000)
}

lazy val fileUploadServiceSettings = {

  val copyFormsTask = taskKey[Unit]("Copies forms to docker stage")

  val copyInitialFiles = Def.task {
    val file = (Compile / baseDirectory).value / "public" / "forms"
    IO.copyDirectory(file, (Docker / stagingDirectory).value / "forms")
  }
  val baseSettings = Seq(
    name := "file-upload-service",
    pipelineStages in Assets := Seq(digest, gzip),
    libraryDependencies ++= Seq(
      guice,
      Endpoints4sPlayServer
    ),
    compile := Def
      .sequential(copyInitialFiles, Compile / compile)
      .dependsOn(Docker / publishLocal)
      .value
  )

  val filesLocation = "/opt/docker/file-uploads"

  val makeFilesDir = Seq(
    dockerEnvVars := Map("FILES_LOCATION" -> filesLocation),
    dockerExposedVolumes := Seq(filesLocation)
  )

  baseSettings ++ dockerSettings(9001) ++ makeFilesDir ++ Seq(
    dockerCommands += new CmdLike {
      def makeContent: String =
        s"COPY forms/* $filesLocation/"
    }
  )
}

lazy val authorizationServiceSettings = {
  val baseSettings = Seq(
    name := "authorization-service",
    scalacOptions += "-language:implicitConversions",
    libraryDependencies ++= Seq(
      guice,
      Mockito,
      Endpoints4sPlayServer,
      JwtNative,
      logback,
      nettyServer
    ),
    javaOptions in Test += "-Dconfig.file=src/test/resources/application.conf",
    javaOptions += "-Dio.netty.leakDetection.level=advanced",
    compile := (Compile / compile)
      .dependsOn(Docker / publishLocal)
      .value,
    fork in Test := true
  )

  baseSettings ++ dockerSettings(9002)
}

val MDC_VERSION = "9.0.0"

lazy val frontendSettings = Seq(
  name := "frontend",
  scalaJSUseMainModuleInitializer := true,
  scalacOptions := Seq("-Ymacro-annotations"),
  webpackBundlingMode := BundlingMode.LibraryOnly(),
  scalaJSLinkerConfig in (Compile, fastOptJS) ~= { _.withSourceMap(false) },
  scalaJSLinkerConfig ~= { _.withESFeatures(_.withUseECMAScript2015(false)) },
  version in installJsdom := "16.4.0",
  requireJsDomEnv in Test := true,
  npmDependencies in Compile ++= Seq(
    "material-components-web" -> MDC_VERSION,
    "jwt-simple"              -> "0.5.6",
    "snabbdom"                -> "0.5.3"
  ),
  resolvers += Resolver.githubPackages("uosis"),
  libraryDependencies ++= Seq(
    "com.raquo"        %%% "laminar"                         % "0.12.1",
    "com.raquo"        %%% "waypoint"                        % "0.3.0",
    "org.endpoints4s"  %%% "xhr-client-circe"                % "2.0.0",
    "be.doeraene"      %%% "url-dsl"                         % "0.3.2",
    "com.github.uosis" %%% "laminar-web-components-material" % "0.1.0",
    "org.scala-js"     %%% "scalajs-dom"                     % "1.0.0",
    ScalaTest.value
  )
)

lazy val sharedSettings = Seq(
  version := "0.1",
  scalaVersion := "2.13.3",
  scalacOptions := Seq("-Ymacro-annotations"),
  libraryDependencies ++= Seq(
    Endpoints4sAlgebraCirce.value,
    Endpoints4sJsonSchemaCirce.value,
    "io.circe" %%% "circe-core"           % "0.13.0",
    "io.circe" %%% "circe-generic"        % "0.13.0",
    "io.circe" %%% "circe-generic-extras" % "0.13.0",
    "io.circe" %%% "circe-parser"         % "0.13.0",
    ScalaTest.value
  )
)

def dockerSettings(
    ports: Int*
) = Seq(
  dockerBaseImage := "openjdk:15-jdk-alpine AS Base",
  dockerExposedPorts := ports,
  dockerChmodType := DockerChmodType.UserGroupWriteExecute,
  dockerPermissionStrategy := DockerPermissionStrategy.CopyChown
)

lazy val Endpoints4sPlayServer =
  "org.endpoints4s" %% "play-server-circe" % "2.0.0"
lazy val Endpoints4sPlayClient = Def.setting(
  "org.endpoints4s" %%% "play-client" % "2.0.0"
)
lazy val Endpoints4sAlgebraCirce = Def.setting(
  "org.endpoints4s" %%% "algebra-circe" % "1.3.0"
)
lazy val Endpoints4sJsonSchemaCirce = Def.setting(
  "org.endpoints4s" %%% "json-schema-circe" % "1.3.0"
)

lazy val JwtNative = "com.github.jwt-scala" %% "jwt-core" % "7.0.0"

lazy val ScalaTest = Def.setting(
  "org.scalatest" %%% "scalatest" % "3.2.6" % Test
)
lazy val Mockito = "org.mockito" % "mockito-scala_2.13" % "1.16.25" % Test

lazy val ScalaMock = Def.setting(
  "org.scalamock" %%% "scalamock" % "5.1.0" % Test
)

onChangedBuildSource in Global := ReloadOnSourceChanges
