package io.prediction.data.storage.elasticsearch

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.json4s.Formats
import org.json4s.native.Serialization.read

import scala.collection.mutable.ArrayBuffer

object ESUtils {
  val scrollLife = new TimeValue(60000)

  def getAll[T : Manifest](
      client: Client,
      builder: SearchRequestBuilder)(
      implicit formats: Formats): Seq[T] = {
    val results = ArrayBuffer[T]()
    var response = builder.setScroll(scrollLife).get
    var hits = response.getHits().hits()
    results ++= hits.map(h => read[T](h.getSourceAsString))
    while (hits.size > 0) {
      response = client.prepareSearchScroll(response.getScrollId).
        setScroll(scrollLife).get
      hits = response.getHits().hits()
      results ++= hits.map(h => read[T](h.getSourceAsString))
    }
    results
  }
}
