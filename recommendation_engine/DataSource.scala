import scala.io.Source;
import scala.collection.mutable.Map;
import scala.util.control.Breaks._;
object DataSource{
  val map: Map[Int, Int] = Map();
  def main(args: Array[String]){
    readData();
  }
  def readData(){
    println("reading data");
    val tmp = Source.fromFile("my_file").getLines().map(line => line.split("\\s+"))
    tmp.foreach(line => map += line(0).toInt -> line(1).toInt)
    println(map.mkString(", "))
    
  }
}
