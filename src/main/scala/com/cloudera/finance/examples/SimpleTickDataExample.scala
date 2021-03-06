/**
 * Copyright (c) 2015, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */

package com.cloudera.finance.examples

import com.cloudera.finance.ts.TimeSeries
import com.cloudera.finance.ts.DateTimeIndex._
import com.cloudera.finance.ts.TimeSeries._
import com.cloudera.finance.ts.TimeSeriesRDD._

import com.github.nscala_time.time.Imports._

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

class SimpleTickDataExample {
  def main(args: Array[String]): Unit = {
    val inputDir = args(0)

    val conf = new SparkConf()
    val sc = new SparkContext(conf)

    val seriesByFile: RDD[TimeSeries[String]] =
      sc.wholeTextFiles(inputDir).map { case (path, text) =>
        yahooStringToTimeSeries(text, path)
      }
    seriesByFile.cache()

    val start = seriesByFile.map(_.index.start).reduce { case (a, b) => if (a < b) a else b }
    val end = seriesByFile.map(_.index.end).reduce { case (a, b) => if (a > b) a else b }
    val dtIndex = uniform(start, end)
    val tsRdd = timeSeriesRDD(dtIndex, seriesByFile)
  }

  def yahooStringToTimeSeries(text: String, keyPrefix: String): TimeSeries[String] = {
    val lines = text.split('\n')
    val labels = lines(0).split(',').tail.map(keyPrefix + _)
    val samples = lines.tail.map { line =>
      val tokens = line.split(',')
      val dt = new DateTime(tokens.head)
      (dt, line.tail.map(_.toDouble))
    }
    timeSeriesFromSamples(samples, labels)
  }
}
