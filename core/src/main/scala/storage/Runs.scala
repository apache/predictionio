package io.prediction.storage

import com.github.nscala_time.time.Imports._

/**
 * Run object.
 *
 * Stores parameters, model, and evaluation results for each run.
 *
 * @param id Run ID.
 * @param starttime Start time of the run.
 * @param endtime End time of the run.
 * @param crossvalidationresults Cross validation results.
 */
case class Run(
  id: String,
  startTime: DateTime,
  endTime: DateTime,
  engineManifestId: String,
  engineManifestVersion: String,
  batch: String,
  evaluationDataParams: String,
  validationParams: String,
  cleanserParams: String,
  algoParamsList: String,
  serverParams: String,
  models: Array[Byte],
  crossValidationResults: String)

/**
 * Base trait for implementations that interact with Runs in the
 * backend app data store.
 */
trait Runs {
  /** Insert a new Run. */
  def insert(run: Run): String

  /** Get a Run by ID. */
  def get(id: String): Option[Run]

  /** Delete a Run. */
  def delete(id: String): Unit
}
