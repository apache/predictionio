package io.prediction.controller.java;

import java.lang.Iterable;
import java.util.Map;
import java.util.HashMap;
import io.prediction.controller.Params;
import io.prediction.controller.EmptyParams;

public class JavaSimpleEngineBuilder<TD, DP, Q, P, A>
  extends JavaEngineBuilder<TD, DP, TD, Q, P, A> {

  @Override
  public JavaSimpleEngineBuilder<TD, DP, Q, P, A> dataSourceClass(
      Class<? extends LJavaDataSource<? extends Params, DP, TD, Q, A>> cls) {
    super.dataSourceClass = cls;
    return this;
  }

  @Override
  public JavaSimpleEngineBuilder<TD, DP, Q, P, A> preparatorClass(
      Class<? extends LJavaPreparator<? extends Params, TD, TD>> cls) {
    super.preparatorClass = cls;
    return this;
  }

  @Override
  public JavaSimpleEngineBuilder<TD, DP, Q, P, A> addAlgorithmClass(
      String name, Class<? extends LJavaAlgorithm<? extends Params, TD, ?, Q, P>> cls) {
    if (super.preparatorClass == null) {
      super.preparatorClass = LJavaIdentityPreparator.apply(super.dataSourceClass);
    }
    super.algorithmClassMap.put(name, cls);
    return this;
  }

  @Override
  public JavaSimpleEngineBuilder<TD, DP, Q, P, A> servingClass(
      Class<? extends LJavaServing<? extends Params, Q, P>> cls) {
    super.servingClass = cls;
    return this;
  }

  /** Add FirstServing as default */
  public JavaSimpleEngineBuilder<TD, DP, Q, P, A> servingClass() {
    super.servingClass = LJavaFirstServing.apply(this);
    return this;
  }

  @Override
  public JavaSimpleEngine<TD, DP, Q, P, A> build() {
    return new JavaSimpleEngine<TD, DP, Q, P, A> (
      super.dataSourceClass, super.preparatorClass, super.algorithmClassMap, super.servingClass);
  }

}
