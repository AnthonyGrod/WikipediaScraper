import org.jsoup.Jsoup
import scala.util.matching.Regex
import scala.jdk.CollectionConverters._
import scala.collection.immutable.Queue
import scala.annotation.tailrec
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import sttp.client3._
import sttp.model.Uri

import java.io.PrintWriter

import java.io.IOException

case class ArticleLink(language: String, title: String) {
  override def toString: String = s"https://$language.wikipedia.org/wiki/$title"
}

case class ArticlePath(path: List[ArticleLink], depth: Int)

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

  def findShortest(paths: List[ArticlePath]): Int = {
    if (paths.isEmpty) {
      0
    } else {
      // return depth of the first element on the list
      paths.head.depth
    }

  }

  def findShortestPath(
      start: ArticleLink,
      end: ArticleLink
  ): Option[List[ArticlePath]] = {
    var timeTotal: Long = 0L

    @tailrec
    def findShortestPathRec(
        queue: Queue[List[ArticleLink]],
        visited: Set[ArticleLink],
        requestsCount: Int,
        lastRequestTime: Long,
        result: Option[List[ArticlePath]]
    ): Option[List[ArticlePath]] = {
      queue.dequeueOption match {
        case Some((path, remainingQueue)) =>
          val current = path.head
          println(s"currentLength: ${path.length}")
          println(s"path: $path")
          val length: Int = result match {
            case Some(list) => list.length
            case None => 0
          }
          if (path.length > findShortest(result.toList.flatten) && length > 0) {
            println(s"==================Pruned==================")
            // Return result withuot duplicates
            val resultWithoutDuplicates = result match {
              case Some(list) => Some(list.distinct)
              case None => None
            }
            return resultWithoutDuplicates
          }

          if (current == end) {
            println(s"==================Found==================")
            println(s"path: ${path}")
            new PrintWriter("output.txt") { write(s"${path}"); close }
            // Append to result
            val updatedResult = result match {
              case Some(paths) => Some(List(ArticlePath(path, path.length)) ++ paths)
              case None        => Some(List(ArticlePath(path, path.length)))
            }
            findShortestPathRec(remainingQueue, visited, requestsCount, lastRequestTime, updatedResult)
          } else if (visited.contains(current)) {
            findShortestPathRec(remainingQueue, visited, requestsCount, lastRequestTime, result)
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
                // println(s"Time: ${time2 - time1}")
                // println(s"Time total: $timeTotal")
                // println(s"Current: $current")
                // println(s"requestsCount: $requestsCount")
                val updatedQueue =
                  remainingQueue.enqueueAll(links.map(link => link :: path))

                // Check if the maximum number of requests per second has been reached
                val currentTime = System.currentTimeMillis()
                val elapsedMillis = currentTime - lastRequestTime
                val delayMillis = 1000 / 200 // 200 requests per second
                if (elapsedMillis < delayMillis && requestsCount >= 200) {
                  println("==============Sleeping================")
                }
                val updatedRequestsCount =
                  if (elapsedMillis < delayMillis && requestsCount >= 200) {
                    Thread.sleep(delayMillis - elapsedMillis)
                    1
                  } else {
                    requestsCount + 1
                  }

                findShortestPathRec(updatedQueue, visited + current, updatedRequestsCount, currentTime, result)
            }
          }

        case None => None
      }
    }

    findShortestPathRec(
      Queue(List(start)),
      Set.empty,
      0,
      System.currentTimeMillis(),
      Some(List.empty)
    )
  }

}

object JsoupScraper {

  def getLinks(currLink: ArticleLink): List[ArticleLink] = {
    val time1: Long = System.currentTimeMillis()

    val articleLinksPattern: Regex =
      "/wiki/([a-zA-Z0-9%_()]+)".r

    val urlWiki = s"https://${currLink.language}.wikipedia.org/wiki/${currLink.title}"
    val backend = HttpURLConnectionBackend()
    val request = basicRequest.get(Uri.unsafeParse(urlWiki))
    val response = request.send(backend)
    val document = Jsoup.parse(response.body.fold(_ => "", identity))

    val time2: Long = System.currentTimeMillis()
    // println(s"TimeGet: ${time2 - time1}")

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