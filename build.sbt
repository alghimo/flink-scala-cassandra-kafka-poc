import com.websudos.phantom.sbt.EmbeddedCassandra

resolvers in ThisBuild ++= Seq(
    "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
    Resolver.mavenLocal,
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

val flinkVersion = "1.1.3"

val flinkDependencies = Seq(
    "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
    "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided",
    "org.apache.flink" %% "flink-connector-kafka-0.9" % flinkVersion excludeAll(
        ExclusionRule(organization = "org.slf4j"),
        ExclusionRule(organization = "log4j")
    ),
    "com.google.code.gson" % "gson" % "2.7" excludeAll(
        ExclusionRule(organization = "org.slf4j"),
        ExclusionRule(organization = "log4j")
    ),
    "com.websudos"  % "phantom-dsl_2.11" % "1.29.5" excludeAll(
        ExclusionRule(organization = "org.slf4j"),
        ExclusionRule(organization = "log4j"),
        ExclusionRule(organization = "org.scalatest")
    ),
    "com.typesafe" % "config" % "1.3.0" excludeAll(
        ExclusionRule(organization = "org.slf4j"),
        ExclusionRule(organization = "log4j")
        ),
    "org.slf4j" % "slf4j-api" % "1.7.12",
    "org.slf4j" % "slf4j-log4j12" % "1.7.12",
    "log4j" % "log4j" % "1.2.17"
)

val testDependencies = Seq(
    "org.scalactic" %% "scalactic" % "3.0.0" % "test",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "info.batey.kafka" % "kafka-unit" % "0.6" % "test"

    /*"org.apache.kafka" % "kafka_2.11" % "0.10.1.0" % "test",
    "org.apache.curator" % "curator-test" % "3.2.1" % "test"*/
    //"com.github.sakserv" % "hadoop-mini-clusters-kafka" % "0.1.8" % "test"
    //"net.manub" %% "scalatest-embedded-kafka" % "0.10.0" % "test"
)

lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++= flinkDependencies ++ testDependencies
  )

mainClass in assembly := Some("org.alghimo.Job")

//unmanagedBase in Test := baseDirectory.value / "lib"
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

val cassandraLogger = sbt.Logger.Null

// Manually setting the PhantomSbtPlugin
Seq(
    phantomCassandraConfig := None,
    phantomCleanupEmbeddedCassandra := EmbeddedCassandra.cleanup(streams.value.log),
    test in Test <<= (test in Test).dependsOn(phantomStartEmbeddedCassandra),
    testQuick in Test <<= (testQuick in Test).dependsOn(phantomStartEmbeddedCassandra),
    testOnly in Test <<= (testOnly in Test).dependsOn(phantomStartEmbeddedCassandra),
    phantomCassandraTimeout := Some(60000),
    phantomStartEmbeddedCassandra := EmbeddedCassandra.start(
        cassandraLogger,
        phantomCassandraConfig.value,
        phantomCassandraTimeout.value
    ),
    fork := true
)

logLevel in Test := Level.Error
parallelExecution in Test := false
//phantomCassandraTimeout := Some(20000)
//PhantomSbtPlugin.projectSettings