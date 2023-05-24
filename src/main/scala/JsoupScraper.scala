import scala.util.matching.Regex
import scala.util.{Try, Success, Failure}
import scala.jdk.CollectionConverters._

import sttp.client3._
import sttp.model.{Uri, StatusCode}

import org.jsoup.Jsoup

class JsoupScraper {

  def getLinks(currLink: ArticleLink): List[ArticleLink] = {

    val articleLinksPattern: Regex = "/wiki/([a-zA-Z0-9%_()]+)".r

    val request = basicRequest.get(Uri.unsafeParse(s"https://${currLink.language}.wikipedia.org/wiki/${currLink.title}"))
    val response = request.send(HttpURLConnectionBackend())
    val document = Jsoup.parse(response.body.fold(_ => "", identity))

    document
      .select("a[href]")
      .asScala
      .map(link => link.attr("href"))
      .collect { case articleLinksPattern(url) =>
        ArticleLink(currLink.language, url)
      }
      .toList
  }

	def checkIfLinkExists(link: ArticleLink): Boolean = {
    val urlWiki = s"https://${link.language}.wikipedia.org/wiki/${link.title}"
    val request = basicRequest.get(Uri.unsafeParse(urlWiki))

    val response: Try[Response[Either[String, String]]] = Try {
      request.send(HttpURLConnectionBackend())
    }

    response match {
      case Success(res) =>
        res.code == StatusCode.Ok

      case Failure(ex) =>
        println(s"Error occurred while checking link: ${ex.getMessage}")
        false
    }
  }

}