import sbt._
import Keys._
import ls.Plugin._


object BuildSettings {
  val VERSION = "1.1-M7"

  lazy val basicSettings = seq(
    version               := NightlyBuildSupport.buildVersion(VERSION),
    homepage              := Some(new URL("http://spray.io")),
    organization          := "io.spray",
    organizationHomepage  := Some(new URL("http://spray.io")),
    description           := "A suite of lightweight Scala libraries for building and consuming RESTful " +
                             "web services on top of Akka",
    startYear             := Some(2011),
    licenses              := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaVersion          := "2.10.1",
    resolvers             ++= Dependencies.resolutionRepos,
    scalacOptions         := Seq(
      "-encoding", "utf8",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.6",
      "-language:_",
      "-Xlog-reflective-calls"
    )
  )

  lazy val sprayModuleSettings = basicSettings ++ NightlyBuildSupport.settings ++ seq(
    // scaladoc settings
    (scalacOptions in doc) <++= (name, version).map { (n, v) => Seq("-doc-title", n, "-doc-version", v) },

    // publishing
    crossPaths := false,
    publishMavenStyle := true,
    publishTo <<= version { version =>
      Some {
        "spray nexus" at {
          // public uri is repo.spray.io, we use an SSH tunnel to the nexus here
          "http://localhost:42424/content/repositories/" + {
            if (version.trim.endsWith("SNAPSHOT")) "snapshots/" else
              if (NightlyBuildSupport.isNightly) "nightlies/" else "releases/"
          }
        }
      }
    },

    // LS
    (LsKeys.tags in LsKeys.lsync) := Seq("http", "server", "client", "async"),
    (LsKeys.docsUrl in LsKeys.lsync) := Some(new URL("http://spray.github.com/spray/api/spray-can/")),
    (externalResolvers in LsKeys.lsync) := Seq("spray repo" at "http://repo.spray.io")
  )

  lazy val noPublishing = seq(
    publish := (),
    publishLocal := ()
  )

  lazy val generateSprayVersionConf = TaskKey[Seq[File]]("generate-spray-version-conf",
    "Create a reference.conf file in the managed resources folder that contains a spray.version = ... setting")

  lazy val sprayVersionConfGeneration = seq(
    (unmanagedResources in Compile) <<= (unmanagedResources in Compile).map(_.filter(_.getName != "reference.conf")),
    resourceGenerators in Compile <+= generateSprayVersionConf,
    generateSprayVersionConf <<= (unmanagedResourceDirectories in Compile, resourceManaged in Compile, version) map {
      (sourceDir, targetDir, version) => {
        val source = sourceDir / "reference.conf"
        val target = targetDir / "reference.conf"
        val conf = IO.read(source.get.head)
        IO.write(target, conf.replace("<VERSION>", version))
        Seq(target)
      }
    }
  )

  lazy val siteSettings = basicSettings ++ noPublishing ++ twirl.sbt.TwirlPlugin.Twirl.settings ++
    spray.revolver.RevolverPlugin.Revolver.settings ++ SiteSupport.settings


  lazy val docsSettings = basicSettings ++ noPublishing ++ seq(
    unmanagedSourceDirectories in Test <<= baseDirectory { _ ** "code" get }
  )

  lazy val exampleSettings = basicSettings ++ noPublishing

  import com.github.siasia.WebPlugin._
  lazy val jettyExampleSettings = exampleSettings ++ webSettings // ++ disableJettyLogSettings

  import com.github.siasia.PluginKeys._
  lazy val disableJettyLogSettings = inConfig(container.Configuration) {
    seq(
      start <<= (state, port, apps, customConfiguration, configurationFiles, configurationXml) map {
        (state, port, apps, cc, cf, cx) =>
          state.get(container.attribute).get.start(port, None, Utils.NopLogger, apps, cc, cf, cx)
      }
    )
  }

}