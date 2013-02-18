package io.prediction.commons.scalding.appdata.file

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

//import io.prediction.commons.scalding.AppDataFile
import io.prediction.commons.scalding.appdata.ItemsSource
import io.prediction.commons.scalding.appdata.ItemsSource.FIELD_SYMBOLS

class FileItemsSource(path: String, appId: Int, itypes: Option[List[String]]) extends Tsv (
  p = path + "items.tsv" //AppDataFile(appId, None, None, None, "items.tsv")
) with ItemsSource {
  
  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL
  
  override def getSource: Source = this
  
  // file format, TAB separated file with following field for each line
  // iid
  // itypes: comma separated string
  //
  // Example,
  // u0<tab>t1,t2,t3
  
  override def readData(iidField: Symbol, itypesField: Symbol)(implicit fd: FlowDef): Pipe = {
    this.read
      .mapTo((0, 1) -> (iidField, itypesField)) { fields: (String, String) =>
        val (iid, itypes) = fields
        
        (iid, itypes.split(",").toList)
           
      }.then( filterItypes('itypes, itypes) _ )
    
  }
  
  private def filterItypes(itypesField: Symbol, queryItypes: Option[List[String]])(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val dataPipe = 
      if (queryItypes == None) p
      else p.filter(itypesField) { x: List[String] =>
        val orgSize = x.size
        val diffList = x diff queryItypes.get
        // diff return a new list WITHOUT element appearing in queryItypes.
        // if diffList is shorter than original, it means itypes has elements in queryItypes
        // if diffList is the same, it means itypes has no element in queryItypes.
        // Since we want items which is one of queryItypes, we only want item has diffList smaller.
        diffList.size < orgSize
      }
    
    dataPipe
  }

  override def readStarttime(iidField: Symbol, itypesField: Symbol, starttimeField: Symbol)(implicit fd: FlowDef): Pipe = {
    this.read
      .mapTo((0, 1, 2) -> (iidField, itypesField, starttimeField)) { fields: (String, String, String) =>
        val (iid, itypes, starttime) = fields
        
        (iid, itypes.split(",").toList, starttime)
           
      }.then( filterItypes('itypes, itypes) _ )
  }
  
  override def writeData(iidField: Symbol, itypesField: Symbol, appid: Int)(p: Pipe)(implicit fd: FlowDef): Pipe = {
    val writtenData = p.mapTo((iidField, itypesField) ->
      (FIELD_SYMBOLS("id"), FIELD_SYMBOLS("itypes"), FIELD_SYMBOLS("appid"))) {
        fields: (String, List[String]) => 
          val (iid, itypes) = fields
                    
          (iid, itypes.mkString(","), appid)
     }.write(this)
    
     writtenData
  }
  
  
}