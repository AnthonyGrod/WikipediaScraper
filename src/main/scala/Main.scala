import org.jsoup._
import scala.util.matching.Regex
import scala.jdk.CollectionConverters._
import scala.collection.immutable.Queue
import scala.annotation.tailrec

import java.io.IOException

case class ArticleLink(language: String, title: String) {
  override def toString: String = s"https://$language.wikipedia.org/wiki/$title"
}

object WikipediaScraper {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Usage: JsoupScraper <languageCode> <articleName>")
      return
    }
    val startArticle = ArticleLink.apply(args(0), args(1))
		// Check if user's input is a valid Wikipedia article.
    try {
      val doc = Jsoup.connect(startArticle.toString).get()
    } catch {
      case e: IOException =>
        println("Site does not exist.")
        return
    }

    val links = JsoupScraper.getLinks(startArticle)
		links.foreach(println)
  }

}

object JsoupScraper {

  def getLinks(currLink: ArticleLink): List[ArticleLink] = {
    // Wikipedia uses % to encode foreign characters in article names. Also, spaces are replaced with underscores.
    val articleLinksPattern: Regex =
      s"https://${currLink.language}\\.wikipedia.org/wiki/([a-zA-Z0-9%_()]+)".r
    val doc = Jsoup.connect(currLink.toString).get()
		doc
			.select("a[href]")
			.asScala
			.map(link => link.attr("abs:href"))
			.collect(
				{ case articleLinksPattern(url) =>
					ArticleLink.apply(currLink.language, url)
				}
			)
			.toList
  }
}
