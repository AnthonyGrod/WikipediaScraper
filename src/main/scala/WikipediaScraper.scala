import scala.annotation.tailrec
import scala.concurrent.{Future, Await, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import scala.util.matching.Regex
import scala.collection.immutable.Queue

import java.net.{URLDecoder}

case class ArticleLink(language: String, title: String) {
  override def toString: String = s"https://$language.wikipedia.org/wiki/${URLDecoder.decode(title, "UTF-8")}"
  def decodedTitle: String = URLDecoder.decode(title, "UTF-8")
}

case class ArticlePath(path: List[ArticleLink], depth: Int)

object WikipediaScraper {

  def main(args: Array[String]): Unit = {
    val handler = new IOHandler()
    args.length match {
      case 2 =>
        val searchQueries = handler.readFile(args(0))
        if (searchQueries.isEmpty) {
          println("Invalid file content. File must contain non empty rows of tuples of the form (language, articleNameStart, articleNameEnd). Each ending with new line.")
          return
        }
        val searchResults = searchQueries.get.map {
          case List(start, end) =>
            val shortestPath = findShortestPath(start, end)
            shortestPath match {
              case Some(path) => path
              case None       => List.empty
            }
          case _ => List.empty // Should never happen since we check it but we just do not want to get warnings(;
        }
        handler.writeIntoFile(args(1), searchResults)
      case _ =>
        println("Usage: run <absolutePathToInputFile> <absolutePathToOutputFile>")
    }
  }

  def findShortestPath(
      start: ArticleLink,
      end: ArticleLink
  ): Option[List[ArticlePath]] = {
    var timeTotal: Long = 0L
    val linksScraper = new JsoupScraper()

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
          val length: Int = result match {
            case Some(list) => list.length
            case None => 0
          }
          if (path.length > result.toList.flatten.headOption.map(_.depth).getOrElse(0) && length > 0) {
            // Return result withuot duplicates
            val resultWithoutDuplicates = result match {
              case Some(list) => Some(list.distinct)
              case None => None
            }
            return resultWithoutDuplicates
          }

          if (current == end) {
            // Append to result
            val updatedResult = result match {
              case Some(paths) => Some(List(ArticlePath(path, path.length)) ++ paths)
              case None        => Some(List(ArticlePath(path, path.length)))
            }
            findShortestPathRec(remainingQueue, visited, requestsCount, lastRequestTime, updatedResult)
          } else if (visited.contains(current)) {
            findShortestPathRec(remainingQueue, visited, requestsCount, lastRequestTime, result)
          } else {
            val links = linksScraper.getLinks(current).filterNot(visited.contains)

            val updatedQueue =
              remainingQueue.enqueueAll(links.map(link => link :: path))

            // Check if the maximum number of requests per second has been reached
            val currentTime = System.currentTimeMillis()
            val elapsedMillis = currentTime - lastRequestTime
            val delayMillis = 1000 / 200 // 200 requests per second
            val updatedRequestsCount =
              // make sure that we don't surpass Wikipedia rate limit
              if (elapsedMillis < delayMillis && requestsCount >= 200) {
                Thread.sleep(delayMillis - elapsedMillis)
                1
              } else {
                requestsCount + 1
              }

            findShortestPathRec(updatedQueue, visited + current, updatedRequestsCount, currentTime, result)
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
