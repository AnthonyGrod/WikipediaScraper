import scala.annotation.tailrec
import scala.concurrent.{Future, Await, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import scala.util.matching.Regex
import scala.io.Source
import scala.collection.immutable.Queue

import sttp.client3._
import sttp.model.{Uri, StatusCode}

import java.io.{PrintWriter, File}
import java.net.{URLDecoder, URLEncoder}

case class ArticleLink(language: String, title: String) {
  override def toString: String = s"https://$language.wikipedia.org/wiki/${URLDecoder.decode(title, "UTF-8")}"
  def decodedTitle: String = URLDecoder.decode(title, "UTF-8")
}

case class ArticlePath(path: List[ArticleLink], depth: Int)

object WikipediaScraper {

  def main(args: Array[String]): Unit = {
    args.length match {
      case 2 =>
        val searchQueries = readFile(args(0))
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
        writeIntoFile(args(1), searchResults)
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
          println(s"currentLength: ${path.length}")
          println(s"path: $path")
          val length: Int = result match {
            case Some(list) => list.length
            case None => 0
          }
          if (path.length > findShortestResult(result.toList.flatten) && length > 0) {
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
                linksScraper.getLinks(current).filterNot(visited.contains)
              promise.success(links)
            }

            Await.result(future, Duration.Inf) match {
              case links =>
                val time2: Long = System.currentTimeMillis()
                val resTime: Long = time2 - time1
                timeTotal += resTime
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

    def checkIfLinkExists(link: ArticleLink): Boolean = {
    val urlWiki = s"https://${link.language}.wikipedia.org/wiki/${link.title}"
    val backend = HttpURLConnectionBackend()
    val request = basicRequest.get(Uri.unsafeParse(urlWiki))
    val response = request.send(backend)
    response.code == StatusCode.Ok
  }

  def encodeAlphanumericOnly(str: String): String = {
    val alphanumericPattern = "[\\p{Alnum}\\p{L}]+".r

    val encodedParts = alphanumericPattern.replaceAllIn(str, { m =>
      URLEncoder.encode(m.group(0), "UTF-8")
    })

    encodedParts
  } 

  def readFile(filename: String): Option[List[List[ArticleLink]]] = {
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
            if (!checkIfLinkExists(ArticleLink(lang, srcName)) || !checkIfLinkExists(ArticleLink(lang, destName))) {
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
      val sortedPaths = result.sortBy(_.path.map(_.decodedTitle).mkString(", "))
      val line = sortedPaths.map { path =>
        val strippedLinks = path.path.reverse.map(_.decodedTitle)
        strippedLinks.mkString(", ")
      }.mkString("), (")

      pw.write(s"($line)\n")
    }
    pw.flush()
    pw.close()
  }

  def findShortestResult(paths: List[ArticlePath]): Int = {
    if (paths.isEmpty) {
      0
    } else {
      // return depth of the first element on the list
      paths.head.depth
    }

  }

}
