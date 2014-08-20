package io.prediction.controller.java;

import io.prediction.controller.WorkflowParams;

public class WorkflowParamsBuilder {
  private String batch = "";
  private int verbose = 2;
  private boolean saveModel = false;

  public WorkflowParamsBuilder() {}

  public WorkflowParamsBuilder batch(String batch) {
    this.batch = batch;
    return this;
  }

  public WorkflowParamsBuilder verbose(int verbose) {
    this.verbose = verbose;
    return this;
  }

  public WorkflowParamsBuilder saveModel(boolean saveModel) {
    this.saveModel = saveModel;
    return this;
  }

  public WorkflowParams build() {
    return new WorkflowParams(batch, verbose, saveModel);
  }
}
