import com.lsc.MapReduceEdges

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.nio.file.{Files, Paths}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.nio.file.{Files, Paths}

class MapReduceEdgesSpec extends AnyFlatSpec with Matchers {
  "Map" should "generate a non-empty file at the specified output path" in {
//    val outputFilePath = "C:\\Users\\kirta\\Desktop\\CS441\\NetGameSim\\src\\main\\resources\\output_edges\\part-00000"
    val outputFilePath = "src/main/resources/output_edges/part-00000"
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