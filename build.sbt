resolvers in ThisBuild ++= Seq(
    "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/", Resolver.mavenLocal,
    "Java.net Maven2 Repository"       at "http://download.java.net/maven/2/",
    "Twitter Repository"               at "http://maven.twttr.com",
    Resolver.typesafeRepo("releases"),
    Resolver.sonatypeRepo("releases"),
    Resolver.bintrayRepo("websudos", "oss-releases")
)

name := "flink-quickstart-scala"

version := "0.1-SNAPSHOT"

organization := "org.alghimo"

scalaVersion in ThisBuild := "2.11.8"

fork in run := true

val flinkVersion = "1.1.2"

val flinkDependencies = Seq(
    "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
    "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided",
    /*"org.apache.kafka" % "kafka-clients" % "0.9.0.1",
    ("org.apache.flink" %% "flink-connector-kafka-base" % flinkVersion).intransitive(),
    ("org.apache.flink" %% "flink-connector-kafka-0.9" % flinkVersion).intransitive(),*/
    "org.apache.flink" % "flink-connector-kafka-0.9_2.11" % flinkVersion excludeAll(
        ExclusionRule(organization = "org.slf4j"),
        ExclusionRule(organization = "log4j")
    ),
    "com.google.code.gson" % "gson" % "2.7" excludeAll(
        ExclusionRule(organization = "org.slf4j"),
        ExclusionRule(organization = "log4j")
    ),
    "com.websudos"  % "phantom-dsl_2.11" % "1.22.0" excludeAll(
        ExclusionRule(organization = "org.slf4j"),
        ExclusionRule(organization = "log4j")
    ),
    "com.typesafe" % "config" % "1.3.0" excludeAll(
        ExclusionRule(organization = "org.slf4j"),
        ExclusionRule(organization = "log4j")
        ),
    "org.slf4j" % "slf4j-api" % "1.7.12",
    "org.slf4j" % "slf4j-log4j12" % "1.7.12",
    "log4j" % "log4j" % "1.2.17"
)

lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++= flinkDependencies
  )

mainClass in assembly := Some("org.alghimo.Job")

// make run command include the provided dependencies
runMain in Compile <<= Defaults.runMainTask(fullClasspath in Compile, runner in (Compile, run))

// exclude Scala library from assembly
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

assemblyJarName in assembly := "flink-transaction-score"
assemblyShadeRules in assembly := Seq(
    ShadeRule.rename("org.slf4j.**" -> "logbacklog4j.@1").inLibrary("ch.qos.logback" % "logback-classic" % "1.1.3"),
    ShadeRule.rename("org.apache.log4j.**" -> "slf4jlog4j.@1").inLibrary("org.slf4j" % "slf4j-api" % "1.7.12", "org.slf4j" % "slf4j-log4j12" % "1.7.12")
)

assemblyMergeStrategy in assembly := {
    case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
    case PathList("org", "apache", "flink", "shaded", xs@_*) => MergeStrategy.first
    case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
}