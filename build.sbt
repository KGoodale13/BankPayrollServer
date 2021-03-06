name := "BankPayrollServer"
 
version := "1.0"

lazy val `bankpayrollserver` = (project in file(".")).enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  jdbc, ehcache, evolutions, specs2 % Test, guice,
  "com.typesafe.play" %% "play-slick" % "3.0.1",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.github.t3hnar" %% "scala-bcrypt" % "3.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % "test"
)

libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "5.0.0",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.0",
  "com.mohiva" %% "play-silhouette-testkit" % "5.0.0" % "test",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.13.0-play26",
  "org.mongodb" %% "casbah" % "3.1.1" % "test",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.4" % "test"
)


      