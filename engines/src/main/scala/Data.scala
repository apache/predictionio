//import org.saddle.Series
import org.saddle._

import com.twitter.chill.MeatLocker

class X(val x : Int) {}


class Y(val data: MeatLocker[Frame[Int, String, Int]]) extends Serializable {
  override def toString(): String = {
    s"Y: ${data.get.rowIx.first.get}"
  }
}

