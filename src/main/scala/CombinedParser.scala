package com.lsc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.yaml.snakeyaml.Yaml
import java.io._
import java.io.File
import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.*
import java.util.logging.{Level, Logger}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{PutObjectRequest, PutObjectResponse}
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}


object CombinedParser {
  private val logger = Logger.getLogger(getClass.getName)

  private def getBucketName(s3Path: String): String =
    s3Path.drop(5).takeWhile(_ != '/')

  private def getKey(s3Path: String): String =
    s3Path.drop(5 + getBucketName(s3Path).length + 1)

  def main(args: Array[String]): Unit = {
//    val dataFilePath = "src/main/resources/input/part-00000"
//    val yamlFilePath = "src/main/resources/input/40_nodes.ngs.yaml"
    val inputPath = args(0)
    val inputPath_Yaml = args(1)
//    logger.info(s"$inputPath_Yaml")


    // Parsing data from the first file
    try {
//        val s3Client = S3Client.builder()
//          .region(Region.US_EAST_1) // Specify the region of your S3 bucket
//          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
//          .build()
//
//        val getObjectRequest = GetObjectRequest.builder()
//          .bucket(getBucketName(inputPath))
//          .key(getKey(inputPath))
//          .build()
//
//        val response = s3Client.getObject(getObjectRequest)
//        val temp = response
//        val data = new String(temp.readAllBytes()).trim
//        println(s"Data from S3: $data")
    val data = new String(Files.readAllBytes(Paths.get(args(0)))).trim

    val categories = data.split("\n")

    var nodesModified, nodesRemoved,nodesAdded = List.empty[Int]
    var edgesRemoved, edgesAdded, edgesModified = Map.empty[Int, Int]
    var edgeUnchaged = Set.empty[(Int, Int)]
    var nodesUnchanged = Set.empty[Int]


    categories.foreach { category =>
      val parts = category.trim.split(" : ")
      val categoryType = parts(0).trim
      val items = parts(1).trim.split(";")

      categoryType match {
        case "edgesRemoved" =>
          edgesRemoved = items.map { edge =>
            val values = edge.trim.split(":")
            (values(0).toInt, values(1).toInt)
          }.toMap

        case "newEdgesAdded" =>
          edgesAdded = items.map { edge =>
            val values = edge.trim.split(":")
            (values(0).toInt, values(1).toInt)
          }.toMap

        case "edgesModified" =>
          edgesModified = items.map { edge =>
            val values = edge.trim.split(":")
            (values(0).toInt, values(1).toInt)
          }.toMap

        case "edgesUnchanged" =>
          edgeUnchaged = items.map { edge =>
            val values = edge.trim.split(":")
            (values(0).toInt, values(1).toInt)
          }.toSet

        case "newNodesAdded" =>
          nodesAdded = items.map(_.toInt).toList

        case "nodesModified" =>
          nodesModified = items.map(_.toInt).toList
        case "nodesRemoved" =>
          nodesRemoved = items.map(_.toInt).toList
        case "nodesUnchanged" =>
          nodesUnchanged = items.map(_.toInt).toSet
        case _ =>
          println(s"Unknown category: $categoryType")
      }
    }

    // Parsing data from the second YAML file
//    val s3Client_Yaml = S3Client.builder()
//      .region(Region.US_EAST_1) // Specify the region of your S3 bucket
//      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
//      .build()
//
//    val getObjectRequest_Yaml = GetObjectRequest.builder()
//      .bucket(getBucketName(inputPath_Yaml))
//      .key(getKey(inputPath_Yaml))
//      .build()

//    val responseYaml = s3Client_Yaml.getObject(getObjectRequest_Yaml)
//    val yamlData = new String(responseYaml.readAllBytes()).replaceAll("\t", "  ")
//    println(s"Data from S3yaml: $yamlData")
    val yamlData = new String(Files.readAllBytes(Paths.get(args(1)))).replaceAll("\t", "  ")
    val yaml = new Yaml()
    val yamlParsedData = yaml.load(yamlData).asInstanceOf[java.util.Map[String, Any]]

    val nodes = yamlParsedData.get("Nodes").asInstanceOf[java.util.Map[String, Any]]
    val edges = yamlParsedData.get("Edges").asInstanceOf[java.util.Map[String, Any]]


    val modifiedNodesYaml: List[Int] = Option(nodes.get("Modified")).map(_.asInstanceOf[java.util.List[Int]].asScala.toList).getOrElse(List.empty)
    val removedNodesYaml: List[Int] = Option(nodes.get("Removed")).map(_.asInstanceOf[java.util.List[Int]].asScala.toList).getOrElse(List.empty)
    val addedNodesYaml: Map[Int, Int] = Option(nodes.get("Added")).map(_.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap).getOrElse(Map.empty)
    val modifiedEdgesYaml: Map[Int, Int] = Option(edges.get("Modified")).map(_.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap).getOrElse(Map.empty)
    val addedEdgesYaml: Map[Int, Int] = Option(edges.get("Added")).map(_.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap).getOrElse(Map.empty)
    val removedEdgesYaml: Map[Int, Int] = Option(edges.get("Removed")).map(_.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap).getOrElse(Map.empty)



    val removedATL = removedNodesYaml.intersect(nodesRemoved).length
    val addedATL = addedNodesYaml.values.toList.intersect(nodesAdded).length
    val modifiedATL = modifiedNodesYaml.intersect(nodesModified).length
    val tempun = nodesUnchanged.toList.intersect(nodesAdded ::: nodesRemoved ::: nodesModified)
    val unchangedATL = nodesUnchanged.toList.diff(tempun).length

    val removedWTL = nodesRemoved.diff(removedNodesYaml).length
    val removedCTL = removedNodesYaml.diff(nodesRemoved).length
    val addedWTL =   nodesAdded.toList.diff(addedNodesYaml.toList).length
    val addedCTL =   addedNodesYaml.toList.diff(nodesAdded.toList).length
    val modifiedWTL = nodesModified.diff(modifiedNodesYaml).length
    val modifiedCTL = modifiedNodesYaml.diff(nodesModified).length

    val nodesATL = removedATL + addedATL + modifiedATL + unchangedATL
    val nodesWTL = removedWTL + addedWTL + modifiedWTL
    val nodesCTL = removedCTL + addedCTL + modifiedCTL

    val removedATL1 = removedEdgesYaml.toList.intersect(edgesRemoved.toList).length
    val addedATL1 = addedEdgesYaml.toList.intersect(edgesAdded.toList).length
    val modifiesATL1 = modifiedEdgesYaml.toList.intersect(edgesModified.toList).length
    val tempun1 = edgeUnchaged.toList.intersect(edgesAdded.toList ::: edgesRemoved.toList ::: edgesModified.toList)
    val unchangedATL1 = edgeUnchaged.toList.diff(tempun1).length

    val removedWTL1 = edgesRemoved.toList.diff(removedEdgesYaml.toList).length
    val removedCTL1 = removedEdgesYaml.toList.diff(edgesRemoved.toList).length
    val addedWTL1 = edgesAdded.toList.diff(addedEdgesYaml.toList).length
    val addedCTL1 = addedEdgesYaml.toList.diff(edgesAdded.toList).length
    val modifiedWTL1 = edgesModified.toList.diff(modifiedEdgesYaml.toList).length
    val modifiedCTL1 = modifiedEdgesYaml.toList.diff(edgesModified.toList).length


    val edgesATL = removedATL1 + addedATL1 + modifiesATL1 + unchangedATL1
    val edgesWTL = removedWTL1 + addedWTL1 + modifiedWTL1
    val edgesCTL = removedCTL1 + addedCTL1 + modifiedCTL1

    val CTL = nodesCTL + edgesCTL
    val WTL = nodesWTL + edgesWTL
    val DTL = 0
    val ATL = nodesATL + edgesATL
    val GTL = (ATL + DTL).toFloat
    val BTL = (CTL + WTL).toFloat
    val RTL = GTL + BTL


    println(s"ATL: $ATL")
    println(s"CTL: $CTL")
    println(s"WTL: $WTL")
    val VPR: Double = ((GTL - BTL) / (2 * RTL)) + 0.5
    logger.info(s"VPR:$VPR")
    println(VPR)
    val ACC: Double = GTL / RTL
    logger.info(s"ACC:$ACC")
    println(ACC)
    val BTLR: Double = WTL / RTL
    println(BTLR)
    logger.info(s"BTLR:$BTLR")


    } catch {
      case e: Exception =>
        println("Error processing the files: " + e.getMessage)
    }


  }
}




