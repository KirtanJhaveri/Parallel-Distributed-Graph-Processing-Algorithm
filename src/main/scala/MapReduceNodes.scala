package com.lsc

import org.apache.hadoop.conf.*
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.*
import org.apache.hadoop.mapred.*
import org.apache.hadoop.util.*

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.collection.mutable.ListBuffer
import java.io.IOException
import java.util
import java.util.logging.{Level, Logger}
import scala.collection.mutable.ListBuffer
object MapReduceNodes:
  private val logger = Logger.getLogger(getClass.getName)
  class SimMap extends MapReduceBase with Mapper[LongWritable, Text, IntWritable, Text] {
    private val nodeId1 = new IntWritable()
    private val nodeId2WithSimilarity = new Text()

    override def map(key: LongWritable, value: Text, output: OutputCollector[IntWritable,Text], reporter: Reporter): Unit = {
      try {
        logger.info("Map Started")
        // Split input data into nodes
        val nodes = value.toString.split(";")

        if (nodes.length == 2) {
          val originalNode = nodes(0).trim.drop(11).dropRight(1).split(",").drop(1).map(_.toDouble) // Extract attributes from the first NodeObject
          val perturbedNode = nodes(1).trim.drop(11).dropRight(1).split(",").drop(1).map(_.toDouble) // Extract attributes from the second NodeObject


          val intersectionSize = originalNode.zip(perturbedNode).count { case (a, b) => a == b }
          val unionSize = originalNode.length + perturbedNode.length - intersectionSize
          // Calculating Jaccard Similarity

          val jaccardIndex = if (unionSize > 0) intersectionSize.toDouble / unionSize else 0.0
          val nodeId1String = nodes(0).trim.drop(11).takeWhile(_ != ',').toInt // First index of the first NodeObject
          val nodeId2String = nodes(1).trim.drop(11).takeWhile(_ != ',').toInt // First index of the second NodeObject
          nodeId1.set(nodeId1String)
          nodeId2WithSimilarity.set(s"$nodeId2String,$jaccardIndex")
          output.collect(nodeId1, nodeId2WithSimilarity)
          logger.log(Level.FINE, s"Processed nodes: ${nodeId1.get()} and ${nodeId2WithSimilarity.toString}")
        }
      } catch {
        case e: Exception =>
          // Log error in case of an exception
          logger.log(Level.SEVERE, "Error occurred in the map function for Nodes", e)
      }
    }
  }


  class Reduce extends MapReduceBase with Reducer[IntWritable, Text, IntWritable, Text] {
    private val reducerLogger = Logger.getLogger(classOf[Reduce].getName)
    override def reduce(key: IntWritable, values: java.util.Iterator[Text], output: OutputCollector[IntWritable, Text], reporter: Reporter): Unit = {
      try {

        val jaccardList = ListBuffer[(String, Double)]()

        // Iterate through the values and store in ListBuffer as (String, Double)
        values.asScala.foreach { value =>
          val pairs = value.toString.split(" ; ")

          // Iterate through the pairs and split nodeId and jaccardIndex using ","
          pairs.foreach { pair =>
            val Array(nodeId, jaccardIndex) = pair.split(",")
            jaccardList += ((nodeId, jaccardIndex.toDouble))
          }
        }


        // Sort the list in descending order based on the Jaccard Index values
        val sortedList = jaccardList.sortBy(-_._2)

        // Concatenate the sorted values with semicolons
        val formattedValues = sortedList.map { case (nodeId, jaccardIndex) => s"$nodeId,$jaccardIndex" }.mkString(" ; ")

        // Output
        output.collect(key, new Text(formattedValues))
      }catch
        case e: Exception =>
          // Log error in case of an exception
          reducerLogger.log(Level.SEVERE, "Error occurred in the reduce Node function", e)
    }
  }






  @main def runMapReduceNodes(inputPath: String, outputPath: String): Boolean = {
    val conf: JobConf = new JobConf(this.getClass)
    conf.setJobName("GraphSimilarity")
//    conf.set("fs.defaultFS", "file:///") // Use local file system
    conf.set("mapreduce.job.maps", "1")
    conf.set("mapreduce.job.reduces", "1")
    conf.setOutputKeyClass(classOf[IntWritable])
    conf.setOutputValueClass(classOf[Text])
    conf.setMapperClass(classOf[SimMap])
    conf.setCombinerClass(classOf[Reduce])
    conf.setReducerClass(classOf[Reduce])
    conf.setInputFormat(classOf[TextInputFormat])
    conf.setOutputFormat(classOf[TextOutputFormat[Text, Text]])
    conf.set("mapreduce.output.textoutputformat.separator", " : ")
    logger.info("before input file")
    FileInputFormat.setInputPaths(conf, new Path(inputPath))
    logger.info("before output file")
    FileOutputFormat.setOutputPath(conf, new Path(outputPath))
    try {
      // Run the job and check if it's successful
      val jobComplete = JobClient.runJob(conf)
      val isSuccess = jobComplete.isSuccessful

      // Log success or failure
      if (isSuccess) {
        logger.info("MapReduce job completed successfully.")
      } else {
        logger.warning("MapReduce job did not complete successfully.")
      }

      // Return the job status
      isSuccess
    } catch {
      case e: Exception =>
        // Log error in case of an exception during job execution
        logger.log(Level.SEVERE, "Error occurred while running the MapReduce job", e)
        false
    }
  }

