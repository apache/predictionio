/** Copyright 2015 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.e2.fixture

import scala.collection.immutable.HashMap
import scala.collection.immutable.HashSet

trait PropertiesToBinaryFixture {

  def base = {
    new {
      val maps : Seq[HashMap[String, String]] = Seq(
        HashMap("food" -> "orange", "music" -> "rock", "hobby" -> "scala"),
        HashMap("food" -> "orange", "music" -> "pop", "hobby" ->"running"),
        HashMap("food" -> "banana", "music" -> "rock", "hobby" -> "guitar"),
        HashMap("food" -> "banana", "music" -> "rock", "hobby" -> "guitar")
      )

      val properties = HashSet("food", "hobby")
    }
  }


  def testArrays = {
    new {
      // Test case for checking food value not listed in base.maps.
      val one = Array(("food", "burger"), ("music", "rock"), ("hobby", "scala"))

      // Test case for making sure indices are preserved.
      val twoA = Array(("food", "banana"), ("hobby", "scala"))
      val twoB = Array(("food", "orange"), ("hobby", "scala"))
    }
  }

}
