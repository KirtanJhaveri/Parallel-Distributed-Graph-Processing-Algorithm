//package com.lsc
//
//import org.apache.hadoop.conf.*
//import org.apache.hadoop.fs.Path
//import org.apache.hadoop.io.*
//import org.apache.hadoop.mapred.*
//import org.apache.hadoop.util.*
//
//import java.io.IOException
//import java.util
//import scala.collection.mutable
//import scala.collection.mutable.ListBuffer
//import scala.jdk.CollectionConverters.*
//import java.util.logging.{Level, Logger}
//
//object MapReduceCheck_copy:
//  private val logger = Logger.getLogger(getClass.getName)
//  class Map extends MapReduceBase with Mapper[LongWritable, Text, Text, Text] {
//    private val modificationStatus = new Text()
//    private val ID = new Text()
//
//    override def map(key: LongWritable, value: Text, output: OutputCollector[Text,Text], reporter: Reporter): Unit = {
//      val values = value.toString.split(" : ")
//
//      val intValueOption: Option[Int] = values(0).toString.toIntOption
//
//      intValueOption match {
//        case Some(intValue) =>
//          val nodevaluepair = values(1).split(" ; ")
//          val getfirstvaluepair = nodevaluepair(0).split(",")
//          if (getfirstvaluepair(1).toDouble == 1.0) {
//            modificationStatus.set(new Text("nodesUnchanged"))
//            ID.set(values(0))
//          }
//          else if (getfirstvaluepair(1).toDouble < 0.3) {
//            modificationStatus.set(new Text("nodesRemoved"))
//            ID.set(values(0))
//            println(s"$modificationStatus,$ID")
//          }
//          else {
//            modificationStatus.set(new Text("nodesModified"))
//            ID.set(values(0))
//          }
//        case None =>
//          val edgevaluepair =  values(1).split(";")
//          val getfirstvaluepair = edgevaluepair(0).split(",")
//          if (getfirstvaluepair(1).toDouble == 1.0) {
//            modificationStatus.set(new Text("edgesUnchanged"))
//            ID.set(values(0))
//          }
//          else if (getfirstvaluepair(1).toDouble < 0.3) {
//            modificationStatus.set(new Text("edgesRemoved"))
//            ID.set(values(0))
//          }
//          else {
//            modificationStatus.set(new Text("edgesModified"))
//            ID.set(values(0))
//          }
//      }
//      output.collect(modificationStatus, ID)
//    }
//  }
//
//
//
//  class Reduce extends MapReduceBase with Reducer[Text, Text, Text, Text] {
//    override def reduce(key: Text, values: util.Iterator[Text],
//                        output: OutputCollector[Text, Text],
//                        reporter: Reporter): Unit = {
//      // Initialize a new variable to concatenate values with semicolons
//      val result = new StringBuilder()
//
//      // Iterate through values and append them, separated by semicolons
//      while (values.hasNext) {
//        result.append(values.next().toString)
//        if (values.hasNext) {
//          result.append(";")
//        }
//      }
//
//      output.collect(key, new Text(result.toString()))
//    }
//  }
//
//  @main def runMapReduceCheck(inputPath1: String, inputPath2: String, outputPath: String): Boolean = {
//      val conf: JobConf = new JobConf(this.getClass)
//      conf.setJobName("MapReduceCheck")
////      conf.set("fs.defaultFS", "file:///") // Use local file system
//      conf.set("mapreduce.job.maps", "1")
//      conf.set("mapreduce.job.reduces", "1")
//      conf.setOutputKeyClass(classOf[Text])
//      conf.setOutputValueClass(classOf[Text])
//      conf.setMapperClass(classOf[Map])
//      conf.setCombinerClass(classOf[Reduce])
//      conf.setReducerClass(classOf[Reduce])
//      conf.setInputFormat(classOf[TextInputFormat])
//      conf.setOutputFormat(classOf[TextOutputFormat[Text, Text]])
//      conf.set("mapreduce.output.textoutputformat.separator", " : ")
//      val inputPaths = Array(inputPath1.toString, inputPath2.toString)
//      FileInputFormat.setInputPaths(conf, inputPaths.map(new Path(_)): _*)
//      FileOutputFormat.setOutputPath(conf, new Path(outputPath))
//      try {
//        // Run the job and check if it's successful
//        val jobComplete = JobClient.runJob(conf)
//        val isSuccess = jobComplete.isSuccessful
//
//        // Log success or failure
//        if (isSuccess) {
//          logger.info("MapReduce job completed successfully.")
//        } else {
//          logger.warning("MapReduce job did not complete successfully.")
//        }
//
//        // Return the job status
//        isSuccess
//      } catch {
//        case e: Exception =>
//          // Log error in case of an exception during job execution
//          logger.log(Level.SEVERE, "Error occurred while running the MapReduce job", e)
//          false
//      }
//    }
//
