import com.lsc.MapReduceCheck

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.nio.file.{Files, Paths}

class MapReduceCheckSpec extends AnyFlatSpec with Matchers {

  "runMapReduceCheck" should "generate a non-empty output file at the specified output path" in {
    val outputFilePath = "src/main/resources/final/part-00000"
    // Call your map function that generates the output file
    // Map.mapFunction(/* Pass required parameters here */)

    // Check if the file exists
    val fileExists = Files.exists(Paths.get(outputFilePath))
    fileExists should be(true) // Check if the file exists

    // Read the content of the file and check if it is not empty
    val fileContentBytes = Files.readAllBytes(Paths.get(outputFilePath))
    val fileContent = new String(fileContentBytes)
    fileContent.trim.isEmpty should be(false) // Check if the file is not empty
  }
}

