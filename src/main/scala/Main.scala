import org.jsoup._
import scala.util.matching.Regex
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

import java.io.IOException


case class ArticleLink(language: String, title: String) {
	override def toString: String = s"https://$language.wikipedia.org/wiki/$title"
}


object WikipediaScraper {
  
	def main(args: Array[String]): Unit = {
		val links = JsoupScraper.getLinks(args)
		
    links match {
      case Right(links) => links.foreach(println)
      case Left(errorMessage) => println(s"Error: $errorMessage")
    }
	}
}


object JsoupScraper {

  def getLinks(args: Array[String]): Either[String, List[ArticleLink]] = {
		// TODO: better handling of command line arguments
		if (args.length < 2) {
      Left("Usage: JsoupScraper <languageCode> <articleName>")
    }

		try {

			val article = ArticleLink.apply(args(0), args(1))

			// Wikipedia uses % to encode foreign characters in article names. Also, spaces are replaced with underscores.
			val articleLinksPattern: Regex = s"https://${args(0)}\\.wikipedia.org/wiki/([a-zA-Z0-9%_()]+)".r

			val doc = Jsoup.connect(article.toString).get()
			val links = doc.select("a[href]")
				.asScala
				.map(link => link.attr("abs:href"))
				.collect(
					{
						case articleLinksPattern(url) => ArticleLink.apply(args(0), url)
					}
				)
				.toList
			Right(links)
		} catch {
			case e: IOException => Left("Site does not exist.")
		}
  }
}
