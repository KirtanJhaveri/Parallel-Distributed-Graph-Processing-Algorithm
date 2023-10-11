package com.lsc

import NetGraphAlgebraDefs.GraphPerturbationAlgebra.ModificationRecord
//import NetGraphAlgebraDefs.NetModelAlgebra.{actionType, outputDirectory}
import NetGraphAlgebraDefs.{GraphPerturbationAlgebra, NetGraph, NetModelAlgebra, NodeObject}
import NetModelAnalyzer.Analyzer
import Randomizer.SupplierOfRandomness
import Utilz.{CreateLogger, NGSConstants}
//import com.google.common.graph.ValueGraph

import java.util.concurrent.{ThreadLocalRandom, TimeUnit}
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}
import com.typesafe.config.ConfigFactory
import guru.nidi.graphviz.engine.Format
import org.slf4j.Logger

import java.net.{InetAddress, NetworkInterface, Socket}
import scala.util.{Failure, Success}
import scala.util.matching.Regex
import scala.jdk.CollectionConverters.*
import java.io.PrintWriter
import scala.jdk.CollectionConverters.*
import java.io.PrintWriter
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{PutObjectRequest, PutObjectResponse}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
object Main:
  val logger:Logger = CreateLogger(classOf[Main.type])
  val ipAddr: InetAddress = InetAddress.getLocalHost
  val hostName: String = ipAddr.getHostName
  val hostAddress: String = ipAddr.getHostAddress

  private def getBucketName(s3Path: String): String =
    s3Path.drop(5).takeWhile(_ != '/')
  private def getKey(s3Path: String): String =
    s3Path.drop(5 + getBucketName(s3Path).length + 1)

//  val accessKey = "AKIAVA5OWDT4WMUP5XGN"
//  val secretKey = "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"
  def main(args: Array[String]): Unit =
    def processNodes(originalFilePath: String, perturbedFilePath: String): Unit = {
      val originalGraph = NetGraph.load(originalFilePath)
      val perturbedGraph = NetGraph.load(perturbedFilePath)
//      val outputFile = new PrintWriter(s"$outputDirectory/output_nodes.txt")
      val outputPath = s"${args(9)}/output_nodes.txt"


      val originalNodes: Set[NodeObject] = originalGraph match {
        case Some(graph) => graph.sm.nodes().asScala.toSet
        case None => Set.empty[NodeObject]
      }

      val perturbedNodes: Set[NodeObject] = perturbedGraph match {
        case Some(graph) => graph.sm.nodes().asScala.toSet
        case None => Set.empty[NodeObject]
      }
      if(args(9).startsWith("s3://")){
//        val s3Client = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))).build()
        val s3Client = S3Client.builder().region(Region.US_EAST_1).build()
        val content = originalNodes.flatMap { originalNodes =>
          perturbedNodes.map { perturbedNodes =>
            s"$originalNodes;$perturbedNodes"
          }
        }.mkString("\n")
        logger.info("node combination in process")
//        println(content)
        val contentString = content
//        println("hello")
//        println(contentString)
        val request = PutObjectRequest
          .builder()
          .bucket(getBucketName(outputPath))
          .key(getKey(outputPath))
          .contentType("text/plain")
          .build()

        val inputStream = new java.io.ByteArrayInputStream(contentString.getBytes("UTF-8"))
        val requestBody = RequestBody.fromInputStream(inputStream, contentString.length())
        s3Client.putObject(request, requestBody)
        logger.info("successfully wrote nodes in S3")
      }
      else{
        val outputFile = new PrintWriter(outputPath)
        // Generate and write all combinations of nodes from original and perturbed graphs
        originalNodes.foreach { originalNode =>
          perturbedNodes.map { perturbedNode =>
            val line = s"${originalNode.toString};${perturbedNode.toString}"
            outputFile.println(line)
          }
        }
        // Close the output file
        outputFile.close()
      }



    }

    // Call the function for the 2 input og and perturbed files
    processNodes(args(0), args(1))




    def extractActionObjects(filePath: String): Seq[String] = {
      val graph = NetGraph.load(filePath)
      val actionPattern: Regex = """Action\(\d+,(NodeObject\([^)]+\)),(NodeObject\([^)]+\)),\d+,\d+,(?:Some\(\d+\)|None),(\d+\.\d+)\)""".r

      // Extract Action objects from the data
      val actionObjects: Seq[String] = actionPattern.findAllIn(graph.toString).toList
      actionObjects
    }

    def processActionObjects(actionObjects1: Seq[String], actionObjects2: Seq[String], outputDirectory: String): Unit = {
      val outputPath = s"${args(9)}/output_edges.txt"


      if(outputDirectory.startsWith("s3://")){
//        val s3Client = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))).build()
        val s3Client = S3Client.builder().region(Region.US_EAST_1).build()
        logger.info("in write to s3 edges")
//        val content = actionObjects1.foreach { actionObject1 =>
//          actionObjects2.foreach { actionObject2 =>
//            val line = s"$actionObject1;$actionObject2"
//            line
//          }
//        }
        val content = actionObjects1.flatMap { actionObject1 =>
          actionObjects2.map { actionObject2 =>
            s"$actionObject1;$actionObject2"
          }
        }.mkString("\n")
//        println(content)
        val contentString = content
//        println("hello")
//        println(contentString)
        val request = PutObjectRequest
          .builder()
          .bucket(getBucketName(outputPath))
          .key(getKey(outputPath))
          .contentType("text/plain")
          .build()

        val inputStream = new java.io.ByteArrayInputStream(contentString.getBytes("UTF-8"))
        val requestBody = RequestBody.fromInputStream(inputStream, contentString.length())
        s3Client.putObject(request, requestBody)
        logger.info("successfully wrote edges")
      }
      else{
        val outputFile = new PrintWriter(outputPath)
        // Generate all possible combinations of action objects from 2 files
        actionObjects1.foreach { actionObject1 =>
          actionObjects2.foreach { actionObject2 =>
            val line = s"$actionObject1;$actionObject2"
            outputFile.println(line)

          }
        }
        outputFile.close()
      }

    }




    // Extracting action objects from both files
    val actionObjects1: Seq[String] = extractActionObjects(args(0))
    val actionObjects2: Seq[String] = extractActionObjects(args(1))

    // Process the action objects
    processActionObjects(actionObjects1, actionObjects2, args(9))

    logger.info(s"Starting Map/Reduce Job to calculate Nodes")
    val nodesJobState = MapReduceNodes.runMapReduceNodes(args(2), args(3)) //Functionality 1
    logger.info(s"Starting Map/Reduce Job to calculate Edges")
    val edgesJobState = MapReduceEdges.runMapReduceEdges(args(4), args(5)) //Functionality 2
    logger.info(s"Starting Map/Reduce Job to combine node and edge outputs")
    if(nodesJobState && edgesJobState) {
//      println("Inside if")
      val checkJobState = MapReduceCheck.runMapReduceCheck(args(3), args(5),args(6)) //Functionality 3
      if(checkJobState){
//        println("Inside checkjobstate")
        CombinedParser.main(Array(args(7), args(8)))
      }
    }

