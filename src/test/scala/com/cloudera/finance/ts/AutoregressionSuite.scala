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

import Autoregression._

import java.util.Random

import org.apache.commons.math3.random.MersenneTwister

import org.scalatest.FunSuite
import org.scalatest.Matchers._

class AutoregressionSuite extends FunSuite {
  test("lagMatTrimBoth") {
    val expected = Array(Array(2.0, 1.0), Array(3.0, 2.0))
    Lag.lagMatTrimBoth(Array(1.0, 2.0, 3.0, 4.0), 2) should be (expected)
    val expected2 = Array(Array(1.0), Array(2.0), Array(3.0))
    Lag.lagMatTrimBoth(Array(1.0, 2.0, 3.0, 4.0), 1) should be (expected2)
  }

  test("fit AR(1) model") {
    val model = new ARModel(1.5, Array(.2))
    val ts = model.sample(5000, new MersenneTwister(10L))
    val fittedModel = Autoregression.fitModel(ts, 1)
    assert(fittedModel.coefficients.length == 1)
    assert(math.abs(fittedModel.c - 1.5) < .07)
    assert(math.abs(fittedModel.coefficients(0) - .2) < .03)
  }

  test("fit AR(2) model") {
    val model = new ARModel(1.5, Array(.2, .3))
    val ts = model.sample(5000, new MersenneTwister(10L))
    val fittedModel = Autoregression.fitModel(ts, 2)
    assert(fittedModel.coefficients.length == 2)
    assert(math.abs(fittedModel.c - 1.5) < .15)
    assert(math.abs(fittedModel.coefficients(0) - .2) < .03)
    assert(math.abs(fittedModel.coefficients(1) - .3) < .03)
  }

  test("add and remove time dependent effects") {
    val rand = new Random()
    val ts = Array.fill(1000)(rand.nextDouble())
    val model = new ARModel(1.5, Array(.2, .3))
    val added = model.addTimeDependentEffects(ts, new Array[Double](ts.length))
    val removed = model.removeTimeDependentEffects(added, new Array[Double](ts.length))
    assert(ts.zip(removed).forall(x => math.abs(x._1 - x._2) < .001))
  }
}
