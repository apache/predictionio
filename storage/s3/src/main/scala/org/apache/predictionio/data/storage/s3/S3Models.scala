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

package org.apache.predictionio.data.storage.s3

import java.io.ByteArrayInputStream

import org.apache.predictionio.data.storage.Model
import org.apache.predictionio.data.storage.Models
import org.apache.predictionio.data.storage.StorageClientConfig

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.S3Object
import com.google.common.io.ByteStreams

import grizzled.slf4j.Logging

class S3Models(s3Client: AmazonS3, config: StorageClientConfig, prefix: String)
    extends Models with Logging {

  def insert(i: Model): Unit = {
    def getModel(bucketName: String, key: String): Option[Model] = {
      val data = i.models
      val metadata: ObjectMetadata = new ObjectMetadata()
      metadata.setContentLength(data.length)
      val req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(data), metadata)
      try {
        s3Client.putObject(req)
      } catch {
        case e: Throwable => error(s"Failed to insert a model to s3://${bucketName}/${key}", e)
      }
      None
    }
    doAction(i.id, getModel)
  }

  def get(id: String): Option[Model] = {
    def getModel(bucketName: String, key: String): Option[Model] = {
      val s3object: S3Object = s3Client.getObject(new GetObjectRequest(
        bucketName, key));
      val is = s3object.getObjectContent
      try {
        Some(Model(
          id = id,
          models = ByteStreams.toByteArray(is)))
      } catch {
        case e: Throwable =>
          error(s"Failed to get a model from s3://${bucketName}/${key}", e)
          None
      } finally {
        is.close()
      }
    }
    doAction(id, getModel)
  }

  def delete(id: String): Unit = {
    def deleteModel(bucketName: String, key: String): Option[Model] = {
      try {
        s3Client.deleteObject(new DeleteObjectRequest(bucketName, key))
      } catch {
        case e: Throwable => error(s"Failed to delete s3://${bucketName}/${key}", e)
      }
      None
    }
    doAction(id, deleteModel)
  }

  def doAction(id: String, action: (String, String) => Option[Model]): Option[Model] = {
    config.properties.get("BUCKET_NAME") match {
      case Some(bucketName) =>
        val key = config.properties.get("BASE_PATH") match {
          case Some(basePath) => s"${basePath}/${prefix}${id}"
          case None => s"${prefix}${id}"
        }
        action(bucketName, key)
      case None =>
        error("S3 bucket is empty.")
        None
    }
  }

}
