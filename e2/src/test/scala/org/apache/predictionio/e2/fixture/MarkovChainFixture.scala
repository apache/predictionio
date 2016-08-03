/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.predictionio.e2.fixture

import org.apache.spark.mllib.linalg.distributed.MatrixEntry

trait MarkovChainFixture {
  def twoByTwoMatrix = {
    new {
      val matrixEntries = Seq(
        MatrixEntry(0, 0, 3),
        MatrixEntry(0, 1, 7),
        MatrixEntry(1, 0, 10),
        MatrixEntry(1, 1, 10)
      )
    }
  }
  
  def fiveByFiveMatrix = {
    new {
      val matrixEntries = Seq(
        MatrixEntry(0, 1, 12),
        MatrixEntry(0, 2, 8),
        MatrixEntry(1, 0, 3),
        MatrixEntry(1, 1, 3),
        MatrixEntry(1, 2, 9),
        MatrixEntry(1, 3, 2),
        MatrixEntry(1, 4, 8),
        MatrixEntry(2, 1, 10),
        MatrixEntry(2, 2, 8),
        MatrixEntry(2, 4, 10),
        MatrixEntry(3, 0, 2),
        MatrixEntry(3, 3, 3),
        MatrixEntry(3, 4, 4),
        MatrixEntry(4, 1, 7),
        MatrixEntry(4, 3, 8),
        MatrixEntry(4, 4, 10)
      )
    }
  }
}
