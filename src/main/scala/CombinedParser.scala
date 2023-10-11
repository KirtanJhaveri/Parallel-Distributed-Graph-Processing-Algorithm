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
//    val accessKey = "AKIAVA5OWDT4WMUP5XGN"
//    val secretKey = "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"

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

    var nodesModified, nodesRemoved = List.empty[Int]
    var edgesRemoved, edgesAdded, edgesModified, nodesAdded = Map.empty[Int, Int]
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
        case "edgesAdded" =>
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
          nodesAdded = items.map { edge =>
            val values = edge.trim.split(":")
            (values(0).toInt, values(1).toInt)
          }.toMap
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
//      println(edges)

    val modifiedNodesYaml: List[Int] = Option(nodes.get("Modified")).map(_.asInstanceOf[java.util.List[Int]].asScala.toList).getOrElse(List.empty)
    val removedNodesYaml: List[Int] = Option(nodes.get("Removed")).map(_.asInstanceOf[java.util.List[Int]].asScala.toList).getOrElse(List.empty)
    val addedNodesYaml: Map[Int, Int] = Option(nodes.get("Added")).map(_.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap).getOrElse(Map.empty)
    val modifiedEdgesYaml: Map[Int, Int] = Option(edges.get("Modified")).map(_.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap).getOrElse(Map.empty)
    val addedEdgesYaml: Map[Int, Int] = Option(edges.get("Added")).map(_.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap).getOrElse(Map.empty)
    val removedEdgesYaml: Map[Int, Int] = Option(edges.get("Removed")).map(_.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap).getOrElse(Map.empty)

    // Print the parsed data
//      println("Edges Removed: " + edgesRemoved)
//      println("Edges Added: " + edgesAdded)
//      println("Edges Modified: " + edgesModified)
//      println("Nodes Modified: " + nodesModified)
//      println("Nodes Removed: " + nodesRemoved)
//      println("Nodes Added: " + nodesAdded)
//
//      println("Modified Nodes: " + modifiedNodes)
//      println("Removed Nodes: " + removedNodes)
//      println("Added Nodes: " + addedNodes)
//      println("Modified Edges: " + modifiedEdges)
//      println("Added Edges: " + addedEdges)
//      println("Removed Edges: " + removedEdges)

    var atl, ctl, wtl = 0
    // Comparison for edgesRemoved
    edgesRemoved.foreach {
      case (source, target) =>
        if (removedEdgesYaml.contains(source) && removedEdgesYaml(source) == target) {
          atl += 1
          println(atl)
        } else  {
          wtl += 1
          println(wtl)
        }
    }

    // comparison for removed edges
    removedEdgesYaml.foreach{
        case (source, target) =>
          if (!(edgesRemoved.contains(source) && edgesRemoved(source) == target)) {
            ctl += 1
          }
  }

    // Comparison for edgesAdded
    edgesAdded.foreach {
      case (source, target) =>
        if (addedEdgesYaml.contains(source) && addedEdgesYaml(source) == target) {
          atl += 1
        } else  {
          wtl += 1
        }
    }
    addedEdgesYaml.foreach {
      case (source, target) =>
        if (!(edgesAdded.contains(source) && edgesAdded(source) == target)) {
          ctl += 1
        }
    }


    // Comparison for edgesModified
    edgesModified.foreach {
      case (source, target) =>
        if (modifiedEdgesYaml.contains(source) && modifiedEdgesYaml(source) == target) {
          atl += 1
          print("edgeAdded")
          println(atl)
        } else  {
          wtl += 1
        }
    }
    modifiedEdgesYaml.foreach {
      case (source, target) =>
        if (!(edgesModified.contains(source) && edgesModified(source) == target)) {
          ctl += 1
//            println("em ctl")
//            println(ctl)
        }
    }

    // Comparison for nodesRemoved
    nodesRemoved.foreach { node =>
      if (removedNodesYaml.contains(node)) {
        atl += 1
//          print("noderemoved")
//          println(atl)
      } else {
        wtl += 1
      }
    }
    removedNodesYaml.foreach { node =>
      if (!nodesRemoved.contains(node)) {
        ctl += 1
      }
    }


    // Comparison for nodesAdded
    nodesAdded.foreach {
      case (node, value) =>
        if (addedNodesYaml.contains(node) && addedNodesYaml(node) == value) {
          atl += 1
        } else  {
          wtl += 1
        }
    }
    addedNodesYaml.foreach {
      case (node, value) =>
        if (!(nodesAdded.contains(node) && nodesAdded(node) == value)) {
          ctl += 1
          //              println(atl)
        }
    }


    // Comparison for nodesModified
    nodesModified.foreach { node =>
      if (modifiedNodesYaml.contains(node)) {
        atl += 1
//          print("NodeModified")
//          println(atl)
      } else  {
        wtl += 1
      }
    }
    modifiedNodesYaml.foreach { node =>
      if (!nodesModified.contains(node)) {
        ctl += 1
      }
    }

    val combinedEdge = (addedEdgesYaml ++ removedEdgesYaml ++ modifiedEdgesYaml).toSet
//      println("cn")
//      println(combinedEdge)
    val unchangedEdgeCalc = edgeUnchaged.intersect(combinedEdge)
//      println(edgeUnchaged)
//      println(unchangedEdgeCalc)
    if(unchangedEdgeCalc.isEmpty)
      {
        atl += edgeUnchaged.size
      }
    else
      atl += edgeUnchaged.size - unchangedEdgeCalc.size

    val addedNodesSet: List[Int] = addedNodesYaml.values.toList
//      println(addedNodesSet)
    val combinedNodes = (addedNodesSet ++ removedNodesYaml ++ modifiedNodesYaml).toSet
//      println(nodeUnchanged)
//      println("cn")
//      println(combinedNodes)
    val unchangedNodeCalc = nodesUnchanged.intersect(combinedNodes)
//      println(unchangedNodeCalc)
    if (unchangedNodeCalc.isEmpty) {
//        println(nodesUnchanged.size)
      atl += nodesUnchanged.size
    }
    else
      atl += nodesUnchanged.size - unchangedNodeCalc.size

    // Print the combined comparison results
    println(s"ATL: $atl")
    println(s"CTL: $ctl")
    println(s"WTL: $wtl")

    val dtl  = 0
    val GTL = (atl + dtl).toFloat
    val BTL = (ctl + wtl).toFloat
    val RTL = GTL + BTL
    println(RTL)
    println(GTL)
    println(BTL)
    val VPR: Double = ((GTL - BTL)/(2*RTL)) + 0.5
    logger.info(s"$VPR")
    println(VPR)
    val ACC: Double = GTL/RTL
    logger.info(s"$ACC")
    println(ACC)
    val BTLR: Double = wtl/RTL
    println(BTLR)
    logger.info(s"$BTLR")


    } catch {
      case e: Exception =>
        println("Error processing the files: " + e.getMessage)
    }


  }
}



