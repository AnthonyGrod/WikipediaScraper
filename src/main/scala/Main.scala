import org.jsoup._
import scala.util.matching.Regex
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

import java.io.IOException


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

  def getLinks(args: Array[String]): Either[String, Buffer[String]] = {
		// TODO: better handling of command line arguments
		if (args.length < 2) {
      Left("Usage: JsoupScraper <languageCode> <articleName>")
    }

		try {
			val languageCode = args(0)
			val articleName = args(1)

			// Wikipedia uses % to encode foreign characters in article names. Also, spaces are replaced with underscores.
			val articleLinksPattern: Regex = s"(https://$languageCode\\.wikipedia.org/wiki/[a-zA-Z0-9%_()]+)".r

			val doc = Jsoup.connect(s"https://$languageCode.wikipedia.org/wiki/$articleName").get()
			val links = doc.select("a[href]")
				.asScala
				.map(link => link.attr("abs:href"))
				.collect(
					{
						case articleLinksPattern(url) => url
					}
				)
			Right(links)
		} catch {
			case e: IOException => Left("Site does not exist.")
		}
  }
}