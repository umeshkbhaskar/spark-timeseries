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

package com.cloudera.finance.ts

import scala.collection.mutable.ArrayBuffer

import breeze.linalg._

import com.github.nscala_time.time.Imports._

import org.joda.time.DateTime

private[ts] object UnivariateTimeSeries {
  def union(series: Array[Array[Double]]): Array[Double] = {
    val unioned = Array.fill(series.head.length)(Double.NaN)
    var i = 0
    while (i < unioned.length) {
      var j = 0
      while (java.lang.Double.isNaN(series(j)(i))) {
        j += 1
      }
      if (j < series.length) {
        unioned(i) = series(j)(i)
      }
    }
    i += 1
    unioned
  }

  def union(indexes: Array[UniformDateTimeIndex], series: Array[Array[Double]])
    : (UniformDateTimeIndex, Array[Double]) = {
    val freq = indexes.head.frequency
    if (!indexes.forall(_.frequency == freq)) {
      throw new IllegalArgumentException("Series must have conformed frequencies to be unioned")
    }

    throw new UnsupportedOperationException()
  }

  /**
   * Accepts a series of values indexed by the given source index and slices it to conform to a
   * target index. The target index need not fit inside the source index - non-overlapping regions
   * will be filled with NaNs.
   */
  def openSlice(sourceIndex: DateTimeIndex, targetIndex: DateTimeIndex, vec: Vector[Double])
    : Vector[Double] = {
    if (sourceIndex.isInstanceOf[UniformDateTimeIndex] &&
        targetIndex.isInstanceOf[UniformDateTimeIndex]) {
      openSliceUniformIndices(sourceIndex.asInstanceOf[UniformDateTimeIndex],
        targetIndex.asInstanceOf[UniformDateTimeIndex], vec)
    }
    throw new UnsupportedOperationException()
  }

  /**
   * Accepts a series of values indexed by the given uniform source index and slices it to conform
   * to a uniform target index. The target index need not fit inside the source index -
   * non-overlapping regions will be filled with NaNs.
   *
   * The source and target index must have the same frequencies.
   */
  private def openSliceUniformIndices(
      sourceIndex: UniformDateTimeIndex,
      targetIndex: UniformDateTimeIndex,
      vec: Vector[Double]): Vector[Double] = {
    val startLoc = sourceIndex.locOfDateTime(targetIndex.start, false)
    // TODO: there could be an off-by-one here because UniformDateTimeIndex.end is inclusive
    val endLoc = sourceIndex.locOfDateTime(targetIndex.end, false)
    if (startLoc >= 0 && endLoc <= vec.length) {
      vec(startLoc until endLoc)
    } else {
      val resultVec = DenseVector.fill(endLoc - startLoc) { Double.NaN }
      val safeStartLoc = math.max(startLoc, 0)
      val safeEndLoc = math.min(endLoc, vec.length)
      val resultStartLoc = if (startLoc < 0) -startLoc else 0
      val resultEndLoc = resultStartLoc + (safeEndLoc - safeStartLoc)
      resultVec(resultStartLoc until resultEndLoc) := vec(safeStartLoc until safeEndLoc)
    }
  }

  def samplesToTimeSeries(samples: Iterator[(DateTime, Double)], frequency: Period)
    : (UniformDateTimeIndex, DenseVector[Double]) = {
    val arr = new ArrayBuffer[Double]()
    val iter = iterateWithUniformFrequency(samples, frequency)
    val (firstDT, firstValue) = iter.next()
    arr += firstValue
    while (iter.hasNext) {
      arr += iter.next()._2
    }
    val index = new UniformDateTimeIndex(firstDT, arr.size, frequency)
    (index, new DenseVector[Double](arr.toArray))
  }

  def samplesToTimeSeries(samples: Iterator[(DateTime, Double)], index: UniformDateTimeIndex)
    : (DenseVector[Double]) = {
    val arr = new Array[Double](index.size)
    val iter = iterateWithUniformFrequency(samples, index.frequency)
    var i = 0
    while (i < arr.length) {
      arr(i) = iter.next()._2
      i += 1
    }
    new DenseVector[Double](arr)
  }

  /**
   * Takes an iterator over time samples not necessarily at uniform intervals and returns an
   * iterator of values at uniform intervals, with NaNs placed where there is no data.
   *
   * The input samples must be aligned on the given frequency.
   */
  def iterateWithUniformFrequency(samples: Iterator[(DateTime, Double)], frequency: Period)
    : Iterator[(DateTime, Double)] = {
    // TODO: throw exceptions for points with non-aligned frequencies
    new Iterator[(DateTime, Double)]() {
      var curUniformDT: DateTime = _
      var curSamplesDT: DateTime = _
      var curSamplesValue: Double = _

      def hasNext: Boolean = {
        samples.hasNext || (curUniformDT != null && curUniformDT < curSamplesDT)
      }

      def next(): (DateTime, Double) = {
        val value = if (curUniformDT == null) { // haven't started yet
          val (firstDT, firstValue) = samples.next()
          curSamplesDT = firstDT
          curUniformDT = firstDT
          firstValue
        } else {
          if (curUniformDT > curSamplesDT) {
            val next = samples.next()
            curSamplesDT = next._1
            curSamplesValue = next._2
          }
          if (curUniformDT < curSamplesDT) {
            Double.NaN
          } else {
            assert(curUniformDT == curSamplesDT)
            curSamplesValue
          }
        }
        val ret = (curUniformDT, value)
        curUniformDT += frequency
        ret
      }
    }
  }

  def differences(ts: Vector[Double]): Vector[Double] = {

  }

  def minMaxDateTimes(index: UniformDateTimeIndex, series: Array[Double]): (DateTime, DateTime) = {
    var min = Double.MaxValue
    var minDt: DateTime = null
    var max = Double.MinValue
    var maxDt: DateTime = null

    var i = 0
    while (i < series.length) {
      if (series(i) < min) {
        min = series(i)
        minDt = index(i)
      }
      if (series(i) > max) {
        max = series(i)
        maxDt = index(i)
      }
      i += 1
    }
    (minDt, maxDt)
  }
}
