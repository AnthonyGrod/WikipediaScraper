import org.scalatest.funsuite.AnyFunSuite
import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.JavaConverters._

class ScrapFunSuite extends AnyFunSuite {

  test("Simple test") {
    testBlueprint("input1.txt", "expected1.txt", "output1.txt")
  }

	test("Foreign characters in name test") {
		testBlueprint("input2.txt", "expected2.txt", "output2.txt")
	}

	test("Multiple requests with different language codes test") {
		testBlueprint("input3.txt", "expected3.txt", "output3.txt")
	}

	test("Complex test") {
		testBlueprint("input4.txt", "expected4.txt", "output4.txt")
	}

	private def testBlueprint(inputFileName: String, expectedFileName: String, outputFileName: String): Unit = {
    val inputFile = new File("src/test/scala/iofiles/input/" + inputFileName)
    val expectedOutputFile = new File("src/test/scala/iofiles/expected/" + expectedFileName)
		val outputFile = new File("src/test/scala/iofiles/output/" + outputFileName)

    WikipediaScraper.main(Array(inputFile.getAbsolutePath, outputFile.getAbsolutePath))

    val actualOutput = readFile(outputFile)

    val expectedOutput = readFile(expectedOutputFile)

    assert(actualOutput == expectedOutput)
	}

  private def readFile(file: File): String = {
		val lines = Files.readAllLines(Paths.get(file.getAbsolutePath)).asScala
		lines.mkString("\n")
	}
}
