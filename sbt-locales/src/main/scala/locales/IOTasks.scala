package locales

import better.files._
import better.files.Dsl._
import java.io.InputStream
import java.io.{ File => JFile }
import java.net.URL
import cats.effect.IO
import sbt.Logger
import sbt.io.{ IO => SbtIO }

object IOTasks {
  def downloadCLDR(
    coreZip:      JFile,
    log:          Logger,
    resourcesDir: JFile,
    cldrVersion:  CLDRVersion
  ): IO[Unit] = {
    val localesDir = resourcesDir.toScala / "locales"
    val zipFile    = localesDir / coreZip.getName
    if (!zipFile.exists) {
      val url =
        s"http://unicode.org/Public/cldr/${cldrVersion.id}/core.zip"
      for {
        _ <- IO(
          log
            .info(s"CLDR data missing. downloading ${cldrVersion.id} version to $localesDir...")
        )
        _ <- IO(log.info(s"downloading from $url"))
        _ <- IO(log.info(s"to file $coreZip"))
        _ <- IO(mkdirs(localesDir))
        _ <- IO(SbtIO.unzipURL(new URL(url), localesDir.toJava))
        _ <- IO(log.info(s"CLDR files expanded on $localesDir"))
      } yield ()
    } else {
      IO(log.debug("cldr files already available"))
    }
  }

  def generateCLDR(
    base:    JFile,
    data:    JFile,
    filters: Filters
  ): IO[Seq[JFile]] =
    IO(
      ScalaLocaleCodeGen
        .generateDataSourceCode(
          base,
          data,
          filters
        )
    )

  def providerFile(base: JFile, name: String): IO[File] = IO {
    val destinationPath = base.toScala
    destinationPath / name
  }

  def copyProvider(log: Logger, base: JFile, name: String, packageDir: String): IO[JFile] = IO {
    val pathSeparator       = JFile.separator
    val packagePath         = packageDir.replaceAll("\\.", pathSeparator)
    val stream: InputStream = getClass.getResourceAsStream("/" + name)
    val destinationPath     = base.toScala / packagePath
    mkdirs(destinationPath)
    val destinationFile = destinationPath / name
    rm(destinationFile)
    log.info(s"Copy $name to $destinationFile")
    SbtIO.transfer(stream, destinationFile.toJava)
    destinationFile.toJava
  }
}
