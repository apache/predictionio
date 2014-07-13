package itemrank.test

import org.scalatest._

trait EngineBehaviors { this: FlatSpec =>

  def canCleanse()
  def canPredict()

}
