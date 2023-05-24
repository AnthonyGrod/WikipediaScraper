import scala.util.matching.Regex
import scala.jdk.CollectionConverters._

import sttp.client3._
import sttp.model.{Uri, StatusCode}

import org.jsoup.Jsoup

class JsoupScraper {

  def getLinks(currLink: ArticleLink): List[ArticleLink] = {
    val time1: Long = System.currentTimeMillis()

    val articleLinksPattern: Regex = "/wiki/([a-zA-Z0-9%_()]+)".r

    val urlWiki = s"https://${currLink.language}.wikipedia.org/wiki/${currLink.title}"
    val backend = HttpURLConnectionBackend()
    val request = basicRequest.get(Uri.unsafeParse(urlWiki))
    val response = request.send(backend)
    val document = Jsoup.parse(response.body.fold(_ => "", identity))

    val time2: Long = System.currentTimeMillis()
    println(s"TimeGet: ${time2 - time1}")

    document
      .select("a[href]")
      .asScala
      .map(link => link.attr("href"))
      .collect { case articleLinksPattern(url) =>
        ArticleLink(currLink.language, url)
      }
      .toList
  }
}