package io.prediction.controller.java;

import io.prediction.controller.WorkflowParams;

/**
 * Builder for WorkflowParams.
 */
public class WorkflowParamsBuilder {
  private String batch = "";
  private int verbose = 2;
  private boolean saveModel = false;

  /**
   * Initializes a builder with default values.
   */
  public WorkflowParamsBuilder() {}

  /**
   * Sets the batch label.
   */
  public WorkflowParamsBuilder batch(String batch) {
    this.batch = batch;
    return this;
  }

  /**
   * Sets the verbosity level.
   */
  public WorkflowParamsBuilder verbose(int verbose) {
    this.verbose = verbose;
    return this;
  }

  /**
   * Sets whether trained models are persisted.
   */
  public WorkflowParamsBuilder saveModel(boolean saveModel) {
    this.saveModel = saveModel;
    return this;
  }

  /**
   * Build a WorkflowParams object.
   */
  public WorkflowParams build() {
    return new WorkflowParams(batch, verbose, saveModel);
  }
}
