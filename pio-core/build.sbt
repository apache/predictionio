name := "predictionio"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.0.2",
  "org.json4s" %% "json4s-native" % "3.2.7",
  "org.json4s" %% "json4s-ext" % "3.2.7",
  "org.mongodb" %% "casbah" % "2.6.2",
  "org.scala-saddle" %% "saddle-core" % "1.3.+",
  "org.slf4j" % "slf4j-nop" % "1.6.0",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5" % "test",
  "com.github.nscala-time" %% "nscala-time" % "0.6.0",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "org.scalanlp" %% "breeze" % "0.6.1",
  // other dependencies here
  "org.scalanlp" % "breeze_2.10" % "0.7",
  "org.scalanlp" % "breeze-natives_2.10" % "0.7",
  "org.scalanlp" % "nak" % "1.2.1"
  )

resolvers ++= Seq(
  "Sonatype Snapshots" at
    "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at
    "http://oss.sonatype.org/content/repositories/releases"
)

// Apparently, I(yipjustin) have no idea how sbt works...
resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
    Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")
