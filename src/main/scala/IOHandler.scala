import scala.util.{Try, Success, Failure}
import scala.util.matching.Regex
import scala.io.Source

import java.io.{PrintWriter, File}
import java.net.{URLDecoder, URLEncoder}

class IOHandler {
  def encodeAlphanumericOnly(str: String): String = {
    val alphanumericPattern = "[\\p{Alnum}\\p{L}]+".r

    val encodedParts = alphanumericPattern.replaceAllIn(str, { m =>
      URLEncoder.encode(m.group(0), "UTF-8")
    })

    encodedParts
  } 

  def readFile(filename: String): Option[List[List[ArticleLink]]] = {
    val linksScraper = new JsoupScraper()
    val bufferedSource: Try[Source] = Try(Source.fromFile(filename))
    bufferedSource match {
      case Success(source) =>
        // TODO: Check if file is empty and TRANSLATE TO UTF8 ALL POLISH CHARACTERS
        if (source.isEmpty) {
          return None
        }
        val lines = (for (line <- source.getLines()) yield line).toList
        val validLine: Regex = """\(([a-z]+), ([\p{L}0-9%_()]+), ([\p{L}0-9%_()]+)\)\n?""".r
        val searchQueries: List[List[ArticleLink]] = lines.flatMap {
          case validLine(lang, srcName, destName) =>
            // check if all links exist
            if (!linksScraper.checkIfLinkExists(ArticleLink(lang, srcName)) || !linksScraper.checkIfLinkExists(ArticleLink(lang, destName))) {
              println(s"Invalid tuple: $lang, $srcName, $destName. One of the links does not exist.")
              return None
            }
            Some(List(ArticleLink(lang, encodeAlphanumericOnly(srcName)), ArticleLink(lang, encodeAlphanumericOnly(destName))))
          case _ =>
            source.close()
            return None
        }

        source.close()
        Some(searchQueries)
      case Failure(exception) =>
        println(s"Error opening file: ${exception.getMessage}")
        None
    }
  }

  def writeIntoFile(filePath: String, output: List[List[ArticlePath]]): Unit = {
    val outputFile = new File(filePath)
    val pw = new PrintWriter(outputFile)
    output.foreach { result =>
      val line = result.sortBy(_.path.map(_.decodedTitle).mkString(", ")).map { path =>
        val strippedLinks = path.path.reverse.map(_.decodedTitle)
        strippedLinks.mkString(", ")
      }.mkString("), (")

      pw.write(s"($line)\n")
    }
    pw.close()
  }
}
