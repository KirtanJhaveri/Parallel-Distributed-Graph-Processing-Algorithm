package com.lsc

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io._
import org.apache.hadoop.mapred._

import java.io.IOException
import scala.collection.mutable

object MapReduceCheck {
  class Map extends MapReduceBase with Mapper[LongWritable, Text, Text, Text] {
    private val modificationStatus = new Text()
    private val ID = new Text()

    @throws[IOException]
    override def map(
                      key: LongWritable,
                      value: Text,
                      output: OutputCollector[Text, Text],
                      reporter: Reporter): Unit = {
      val values = value.toString.split(" : ")

      if (values(0).contains(":")) {
        output.collect(new Text("matchedEdges"), new Text(values(0).trim))
      } else {
        output.collect(new Text("matchedNodes"), new Text(values(0)))
      }

      val intValueOption: Option[Int] = values(0).toString.toIntOption

      intValueOption match {
        case Some(intValue) =>
          processNodes(intValue, values(1), output)
        case None =>
          processEdges(values(1), output)
      }
    }

    private def processNodes(nodeId: Int, nodeValues: String, output: OutputCollector[Text, Text]): Unit = {
      val nodeValuePairs = nodeValues.split(" ; ")
      val firstValuePair = nodeValuePairs(0).split(",")
      val status = getNodeModificationStatus(firstValuePair)
      val unmatchedNodes = nodeValuePairs.drop(1).map(pair => pair.split(",")(0).trim)

      output.collect(status, new Text(nodeId.toString))
      emitUnmatchedNodes(unmatchedNodes, output)
    }

    private def getNodeModificationStatus(pair: Array[String]): Text = {
      val value = pair(1).toDouble
      if (value == 1.0) new Text("nodesUnchanged")
      else if (value < 0.3) new Text("nodesRemoved")
      else new Text("nodesModified")
    }

    private def processEdges(edgeValues: String, output: OutputCollector[Text, Text]): Unit = {
      val edgeValuePairs = edgeValues.split(";")
      val firstValuePair = edgeValuePairs(0).split(",")
      val status = getEdgeModificationStatus(firstValuePair)
      val unmatchedEdges = edgeValuePairs.drop(1).map(pair => pair.split(",")(0).trim)

      output.collect(status, new Text(firstValuePair(0)))
      emitUnmatchedEdges(unmatchedEdges, output)
    }

    private def getEdgeModificationStatus(pair: Array[String]): Text = {
      val value = pair(1).toDouble
      if (value == 1.0) new Text("edgesUnchanged")
      else if (value < 0.3) new Text("edgesRemoved")
      else new Text("edgesModified")
    }

    private def emitUnmatchedNodes(unmatchedNodes: Array[String], output: OutputCollector[Text, Text]): Unit = {
      for (node <- unmatchedNodes) {
        output.collect(new Text("unmatchedNodes"), new Text(node))
      }
    }

    private def emitUnmatchedEdges(unmatchedEdges: Array[String], output: OutputCollector[Text, Text]): Unit = {
      for (edge <- unmatchedEdges) {
        output.collect(new Text("unmatchedEdges"), new Text(edge))
      }
    }
  }

  class Reduce extends MapReduceBase with Reducer[Text, Text, Text, Text] {
    override def reduce(key: Text, values: java.util.Iterator[Text], output: OutputCollector[Text, Text], reporter: Reporter): Unit = {
      println(s"Reducer called with key: $key")

      val result = new mutable.HashSet[String]()
      while (values.hasNext) {
        result.add(values.next().toString)
      }

      MapReduceCheck.Reduce.collections.synchronized {
        MapReduceCheck.Reduce.collections.put(key.toString, result.toSet)
      }

      processResults(key, result, output)
    }

    private def processResults(key: Text, result: mutable.HashSet[String], output: OutputCollector[Text, Text]): Unit = {
      if (key.toString == "matchedNodes" || key.toString == "unmatchedNodes") {
        handleNodes(key, result, output)
      } else if (key.toString == "matchedEdges" || key.toString == "unmatchedEdges") {
        handleEdges(key, result, output)
      } else if (result.nonEmpty) {
        output.collect(key, new Text(result.mkString(";")))
      }
    }

    private def handleNodes(key: Text, nodes: mutable.HashSet[String], output: OutputCollector[Text, Text]): Unit = {
      if (MapReduceCheck.Reduce.collections.contains("matchedNodes") && MapReduceCheck.Reduce.collections.contains("unmatchedNodes")) {
        val matchedNodes = MapReduceCheck.Reduce.collections("matchedNodes")
        val unmatchedNodes = MapReduceCheck.Reduce.collections("unmatchedNodes")
        val newNodesAdded = unmatchedNodes.diff(matchedNodes)

        if (newNodesAdded.nonEmpty) {
          output.collect(new Text("newNodesAdded"), new Text(newNodesAdded.mkString(";")))
        }
      }
    }

    private def handleEdges(key: Text, edges: mutable.HashSet[String], output: OutputCollector[Text, Text]): Unit = {
      if (MapReduceCheck.Reduce.collections.contains("matchedEdges") && MapReduceCheck.Reduce.collections.contains("unmatchedEdges")) {
        val matchedEdges = MapReduceCheck.Reduce.collections("matchedEdges")
        val unmatchedEdges = MapReduceCheck.Reduce.collections("unmatchedEdges")
        val newEdgesAdded = unmatchedEdges.diff(matchedEdges)

        if (newEdgesAdded.nonEmpty) {
          output.collect(new Text("newEdgesAdded"), new Text(newEdgesAdded.mkString(";")))
        }
      }
    }
  }

  object Reduce {
    val collections: mutable.Map[String, Set[String]] = mutable.Map()
  }

  def runMapReduceCheck(inputPath1: String, inputPath2: String, outputPath: String): Boolean = {
    val conf: JobConf = new JobConf(this.getClass)
    conf.setJobName("NodeEdgesResult")
    conf.set("mapreduce.job.maps", "1")
    conf.set("mapreduce.job.reduces", "1")
    conf.setOutputKeyClass(classOf[Text])
    conf.setOutputValueClass(classOf[Text])
    conf.setMapperClass(classOf[Map])
    conf.setReducerClass(classOf[Reduce])
    conf.setInputFormat(classOf[TextInputFormat])
    conf.setOutputFormat(classOf[TextOutputFormat[Text, Text]])
    conf.set("mapreduce.output.textoutputformat.separator", " : ")

    val inputPaths = Array(inputPath1, inputPath2)
    FileInputFormat.setInputPaths(conf, inputPaths.map(new Path(_)): _*)
    FileOutputFormat.setOutputPath(conf, new Path(outputPath))

    val jobStatus = JobClient.runJob(conf)
    jobStatus.isSuccessful
  }
}
