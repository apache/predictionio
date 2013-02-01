package io.prediction.commons.scalding

import org.specs2.mutable._

/*
 * just want to give some examples
 */
class DummyTest extends Specification {
  
  // examples
  "The 'Hello world' string" should {
      "contain 11 characters" in {
        "Hello world" must have size(11)
      }
      "start with 'Hello'" in {
        "Hello world" must startWith("Hello")
      }
      "end with 'world'" in {
        "Hello world" must endWith("world")
      }
    }
  
  "1 plus 1" should {
    "equal 2" in {
      val a = 1 + 1
      a must be_==(2)
    }
  }
  
  "Println(work!)" should {
    "print work!" in {
      println("work! printed by DummyTest")
    }
  }
  
}
