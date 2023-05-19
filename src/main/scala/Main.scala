import org.jsoup.Jsoup
import scala.util.matching.Regex
import scala.jdk.CollectionConverters._
import scala.collection.immutable.Queue
import scala.annotation.tailrec
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import java.io.IOException

case class ArticleLink(language: String, title: String) {
  override def toString: String = s"https://$language.wikipedia.org/wiki/$title"
}

object WikipediaScraper {

  def main(args: Array[String]): Unit = {
    if (args.length < 4) {
      println(
        "Usage: JsoupScraper <languageCodeStart> <articleNameStart> <languageCodeEnd> <articleNameEnd"
      )
      return
    }
    val startArticle = ArticleLink(args(0), args(1))
    val endArticle = ArticleLink(args(2), args(3))
    // Check if user's input is a valid Wikipedia article.
    try {
      val doc = Jsoup.connect(startArticle.toString).get()
    } catch {
      case e: IOException =>
        println("Site does not exist.")
        return
    }

    val shortestPath = findShortestPath(startArticle, endArticle)
    shortestPath match {
      case Some(path) => path.foreach(println)
      case None       => println("No path found.")
    }
  }

  def findShortestPath(
      start: ArticleLink,
      end: ArticleLink
  ): Option[List[ArticleLink]] = {
    var timeTotal: Long = 0L

    @tailrec
    def findShortestPathRec(
        queue: Queue[List[ArticleLink]],
        visited: Set[ArticleLink],
        requestsCount: Int,
        lastRequestTime: Long
    ): Option[List[ArticleLink]] = {
      queue.dequeueOption match {
        case Some((path, remainingQueue)) =>
          val current = path.head

          if (current == end) {
            Some(path.reverse)
          } else if (visited.contains(current)) {
            findShortestPathRec(
              remainingQueue,
              visited,
              requestsCount,
              lastRequestTime
            )
          } else {
            val time1: Long = System.currentTimeMillis()

            val promise = Promise[List[ArticleLink]]()
            val future = promise.future

            Future {
              val links =
                JsoupScraper.getLinks(current).filterNot(visited.contains)
              promise.success(links)
            }

            Await.result(future, Duration.Inf) match {
              case links =>
                val time2: Long = System.currentTimeMillis()
                val resTime: Long = time2 - time1
                timeTotal += resTime
                println(s"Time: ${time2 - time1}")
                println(s"Time total: $timeTotal")
                println(s"Current: $current")
                println(s"requestsCount: $requestsCount")
                val updatedQueue =
                  remainingQueue.enqueueAll(links.map(link => link :: path))

                // Check if the maximum number of requests per second has been reached
                val currentTime = System.currentTimeMillis()
                val elapsedMillis = currentTime - lastRequestTime
                val delayMillis = 1000 / 200 // 200 requests per second
                val updatedRequestsCount =
                  if (elapsedMillis < delayMillis && requestsCount >= 200) {
                    Thread.sleep(delayMillis - elapsedMillis)
                    1
                  } else {
                    requestsCount + 1
                  }

                findShortestPathRec(
                  updatedQueue,
                  visited + current,
                  updatedRequestsCount,
                  currentTime
                )
            }
          }

        case None => None
      }
    }

    findShortestPathRec(
      Queue(List(start)),
      Set.empty,
      0,
      System.currentTimeMillis()
    )
  }

}

object JsoupScraper {

  def getLinks(currLink: ArticleLink): List[ArticleLink] = {
    // Wikipedia uses % to encode foreign characters in article names. Also, spaces are replaced with underscores.
    val baseArticleLinkPattern: String =
      "https://$language\\.wikipedia.org/wiki/([a-zA-Z0-9%_()]+)"
    val articleLinksPattern: Regex =
      baseArticleLinkPattern.replace("$language", currLink.language).r

    val time1: Long = System.currentTimeMillis()
    val doc = Jsoup.connect(currLink.toString).get()
    val time2: Long = System.currentTimeMillis()
    println(s"TimeGet: ${time2 - time1}")

    doc
      .select("a[href]")
      .asScala
      .map(link => link.attr("abs:href"))
      .collect { case articleLinksPattern(url) =>
        ArticleLink(currLink.language, url)
      }
      .toList
  }
}
