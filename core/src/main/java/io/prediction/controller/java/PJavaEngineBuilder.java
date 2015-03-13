package io.prediction.controller.java;

import io.prediction.controller.Params;

import java.util.Map;
import java.util.HashMap;

/**
 * A convenient builder class for linking up Data Source, Preparator, Algorithm,
 * and Serving classes as an Engine.
 *
 * @param <TD> Training Data
 * @param <EI> Evaluation Info
 * @param <PD> Prepared Data
 * @param <Q> Input Query
 * @param <P> Output Prediction
 * @param <A> Actual Value
 */
public class PJavaEngineBuilder<TD, EI, PD, Q, P, A> {
  /** Data Source class. Default to null. */
  protected Class<? extends PJavaDataSource<TD, EI, Q, A>>
      dataSourceClass = null;
  /** Preparator class. Default to null. */
  protected Class<? extends PJavaPreparator<TD, PD>>
      preparatorClass = null;
  /** Map of Algorithm names to respective classes. Default to empty Map. */
  protected Map<String, Class<? extends PJavaAlgorithm<PD, ?, Q, P>>>
      algorithmClassMap = new HashMap <> ();
  /** Serving class. Default to null. */
  protected Class<? extends LJavaServing<Q, P>>
      servingClass = null;

  /**
   * Instantiate an empty Java-based Engine builder.
   */
  public PJavaEngineBuilder() {}

  /**
   * Set the Data Source class of this Engine.
   */
  public PJavaEngineBuilder<TD, EI, PD, Q, P, A> dataSourceClass(
      Class<? extends PJavaDataSource<TD, EI, Q, A>> cls) {
    dataSourceClass = cls;
    return this;
  }

  /**
   * Set the Preparator class of this Engine.
   */
  public PJavaEngineBuilder<TD, EI, PD, Q, P, A> preparatorClass(
      Class<? extends PJavaPreparator<TD, PD>> cls) {
    preparatorClass = cls;
    return this;
  }

  /**
   * Add an Algorithm class to this Engine.
   */
  public PJavaEngineBuilder<TD, EI, PD, Q, P, A> addAlgorithmClass(
      String name, Class<? extends PJavaAlgorithm<PD, ?, Q, P>> cls) {
    algorithmClassMap.put(name, cls);
    return this;
  }

  /**
   * Set the Serving class of this Engine.
   */
  public PJavaEngineBuilder<TD, EI, PD, Q, P, A> servingClass(
      Class<? extends LJavaServing<Q, P>> cls) {
    servingClass = cls;
    return this;
  }

  /**
   * Build and return an Engine instance.
   */
  public PJavaEngine<TD, EI, PD, Q, P, A> build() {
    return new PJavaEngine<> (dataSourceClass, preparatorClass, algorithmClassMap, servingClass);
  }

  @Override public String toString() {
    return "PJavaEngineBuilder ds=" + dataSourceClass + " p=" + preparatorClass + " algo=" +
      algorithmClassMap + " s=" + servingClass;
  }

}
