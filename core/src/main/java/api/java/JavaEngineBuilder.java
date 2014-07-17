package io.prediction.controller.java;

import java.lang.Iterable;
import java.util.Map;
import java.util.HashMap;
import io.prediction.controller.Params;

public class JavaEngineBuilder<TD, DP, PD, Q, P, A> {
  private Class<? extends LJavaDataSource<? extends Params, DP, TD, Q, A>> dataSourceClass = null;
  private Class<? extends LJavaPreparator<? extends Params, TD, PD>> preparatorClass = null;
  private Map<String, Class<? extends LJavaAlgorithm<? extends Params, PD, ?, Q, P>>> 
    algorithmClassMap = new HashMap <> ();
  private Class<? extends LJavaServing<? extends Params, Q, P>> servingClass = null;
  
  public JavaEngineBuilder() {}

  public JavaEngineBuilder<TD, DP, PD, Q, P, A> dataSourceClass(
      Class<? extends LJavaDataSource<? extends Params, DP, TD, Q, A>> cls) {
    dataSourceClass = cls;
    return this;
  }
  
  public JavaEngineBuilder<TD, DP, PD, Q, P, A> preparatorClass(
      Class<? extends LJavaPreparator<? extends Params, TD, PD>> cls) {
    preparatorClass = cls;
    return this;
  }

  public JavaEngineBuilder<TD, DP, PD, Q, P, A> addAlgorithmClass(
      String name, Class<? extends LJavaAlgorithm<? extends Params, PD, ?, Q, P>> cls) {
    algorithmClassMap.put(name, cls);
    return this;
  }

  public JavaEngineBuilder<TD, DP, PD, Q, P, A> servingClass(
      Class<? extends LJavaServing<? extends Params, Q, P>> cls) {
    servingClass = cls;
    return this;
  }

  public JavaEngine<TD, DP, PD, Q, P, A> build() {
    return new JavaEngine<> (dataSourceClass, preparatorClass, algorithmClassMap, servingClass);
  }

  @Override public String toString() {
    return "JavaEngineBuilder ds=" + dataSourceClass + " p=" + preparatorClass + " algo=" +
      algorithmClassMap + " s=" + servingClass;
  }
}

