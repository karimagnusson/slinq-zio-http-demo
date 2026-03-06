scalaVersion := "3.3.7"

version := "0.4"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

// Uncomment below to use the published Slinq from GitHub Packages instead of the bundled JARs in lib /
// resolvers += "GitHub Packages" at "https://maven.pkg.github.com/karimagnusson/slinq"
// credentials += Credentials(
//   "GitHub Package Registry",
//   "maven.pkg.github.com",
//   "<username>",
//   sys.env.getOrElse("GITHUB_TOKEN", "<token>")
// )

lazy val root = (project in file("."))
  .settings(
    name := "slinq-zhttp-demo",
    libraryDependencies ++= Seq(
      "com.typesafe"   % "config"      % "1.4.1",
      "dev.zio"       %% "zio"         % "2.1.22",
      "dev.zio"       %% "zio-streams" % "2.1.22",
      "dev.zio"       %% "zio-http"    % "3.7.0",
      "dev.zio"       %% "zio-json"    % "0.7.45",
      "org.postgresql" % "postgresql"  % "42.7.5"
      // Uncomment below if using published Slinq instead of bundled JARs
      // "io.github.karimagnusson" %% "slinq-pg-zio" % "0.9.6-RC2"
    ),
    run / fork         := true,
    run / connectInput := true
  )
