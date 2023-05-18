import org.jsoup._
import scala.util.matching.Regex
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

import java.io.IOException


object WikipediaScraper {
  
	def main(args: Array[String]): Unit = {
		try {
			val links = JsoupScraper.getLinks(args)
			println(links)


		} catch {
			case e: IOException => println("Site does not exist.")
		}
	}
}


object JsoupScraper {

	@throws(classOf[IOException])
  def getLinks(args: Array[String]): Buffer[String] = {
		// TODO: better handling of command line arguments
		if (args.length < 2) {
      println("Usage: JsoupScraper <languageCode> <articleName>")
      return Buffer.empty[String]
    }

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

		return links
  }
}