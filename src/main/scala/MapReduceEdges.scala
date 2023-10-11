package com.lsc

import org.apache.hadoop.conf.*
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.*
import org.apache.hadoop.mapred.*
import org.apache.hadoop.util.*

import java.io.IOException
import java.util
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import java.util.logging.{Level, Logger}
object MapReduceEdges:
  private val logger = Logger.getLogger(getClass.getName)
  class ActionComparisonMap extends MapReduceBase with Mapper[LongWritable, Text, Text, Text] {
    private val outputKey = new Text()
    private val outputValue = new Text()

    override def map(key: LongWritable, value: Text, output: OutputCollector[Text, Text], reporter: Reporter): Unit = {
      try {
        val actions = value.toString.split(";")

        if (actions.length == 2) {
          val action1 = actions(0).trim
          val action2 = actions(1).trim

          // Extract values from action object (excluding NodeObjects)
          val action1Values = extractActionValues(action1)
          val action2Values = extractActionValues(action2)

          //Calculating Similarity score between edges using Jaccard Index
          val intersectionSize = action1Values.zip(action2Values).count { case (a, b) => a == b }
          val unionSize = action1Values.length + action2Values.length - intersectionSize
          val jaccardIndex = if (unionSize > 0) intersectionSize.toDouble / unionSize else 0.0

          outputKey.set(extractnodeId(action1)) // get Node Ids of the two nodes for key
          outputValue.set(extractnodeId(action2) + "," + s"$jaccardIndex") // get node IDs from 2nd object for value and also adding jaccard index in the value

          output.collect(outputKey, outputValue)
          logger.log(Level.FINE, s"Processed nodes: ${outputKey.toString} and ${outputValue.toString}")
        }
      } catch{
        case e: Exception =>
          // Log error in case of an exception
          logger.log(Level.SEVERE, "Error occurred in the map function for Edges", e)
      }
    }

    def extractActionValues(action: String): List[String] = {
      val actionString =
        action.substring(action.indexOf("(") + 1, action.lastIndexOf(")"))
      val modifiedActionString =
        actionString.replaceAll("NodeObject\\([^\\)]+\\)", "")

      // Split the Modified String using commas, trim spaces, and convert to  list
      val partsList = modifiedActionString.split(",").toList.map(_.trim)

      // Delete empty strings and return the list with all attributes inside action object
      partsList.filter(_.nonEmpty)
    }

    def extractnodeId(action: String): String = {
      val nodeObjectsPattern = """NodeObject\((\d+),""".r
      val firstElements = nodeObjectsPattern.findAllMatchIn(action).map(_.group(1)).toSeq
      if (firstElements.length >= 2) {
        s"${firstElements.head}:${firstElements(1)}"
      } else {
        "none,none"
      }
    }
  }


  class Reduce extends MapReduceBase with Reducer[Text, Text, Text, Text] {
    private val reducerLogger = Logger.getLogger(classOf[Reduce].getName)
    override def reduce(key: Text, values: java.util.Iterator[Text], output: OutputCollector[Text, Text], reporter: Reporter): Unit = {
      try {
        val jaccardList = ListBuffer[(String, Double)]()

        values.asScala.foreach { value =>
          val pairs = value.toString.split(";")

          // Iterate through the pairs and split nodeId and jaccardIndex using ","
          pairs.foreach { pair =>
            val Array(nodeId1, jaccardIndex) = pair.split(",")
            jaccardList += ((nodeId1, jaccardIndex.toDouble))
          }
        }

        // Sort the list in descending order based on the Jaccard Index values
        val sortedList = jaccardList.sortBy(-_._2)

        // Concatenate the sorted values with semicolons
        val concatenatedValues = sortedList.map { case (firstElement, jaccardIndex) =>
          s"$firstElement,$jaccardIndex"
        }.mkString(";")

        // Output the formatted values
        output.collect(new Text(key), new Text(concatenatedValues))
      }catch{
        case e: Exception =>
          // Log error in case of an exception
          reducerLogger.log(Level.SEVERE, "Error occurred in the reduce Node function", e)
      }
    }
  }

  @main def runMapReduceEdges(inputPath: String, outputPath: String): Boolean = {
    val conf: JobConf = new JobConf(this.getClass)
    conf.setJobName("MapReduceCheck")
//    conf.set("fs.defaultFS", "file:///") // Use local file system
    conf.set("mapreduce.job.maps", "1")
    conf.set("mapreduce.job.reduces", "1")
    conf.setOutputKeyClass(classOf[Text])
    conf.setOutputValueClass(classOf[Text])
    conf.setMapperClass(classOf[ActionComparisonMap])
    conf.setCombinerClass(classOf[Reduce])
    conf.setReducerClass(classOf[Reduce])
    conf.setInputFormat(classOf[TextInputFormat])
    conf.setOutputFormat(classOf[TextOutputFormat[Text, Text]])
    conf.set("mapreduce.output.textoutputformat.separator", " : ")
    FileInputFormat.setInputPaths(conf, new Path(inputPath))
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

