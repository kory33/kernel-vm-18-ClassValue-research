ThisBuild / scalaVersion := "3.7.2"
ThisBuild / organization := "io.github.kory33"

lazy val root = (project in file("."))
  .enablePlugins(JmhPlugin)
  .settings(
    name := "xuwei-k-bench",
    libraryDependencies ++= Seq(
      "org.openjdk.jmh" % "jmh-core" % "1.37",
      "org.openjdk.jmh" % "jmh-generator-annprocess" % "1.37"
    ),
    Jmh / sourceDirectory := (Compile / sourceDirectory).value,
    Jmh / classDirectory := (Compile / classDirectory).value,
    Jmh / dependencyClasspath := (Compile / dependencyClasspath).value,
    Jmh / compile := (Jmh / compile).dependsOn(Compile / compile).value,
    Jmh / run := (Jmh / run).dependsOn(Jmh / compile).evaluated,
    Jmh / javaOptions ++= Seq(
      "-XX:+UnlockDiagnosticVMOptions",
      "-XX:PrintAssemblyOptions=intel"
    )
  )
