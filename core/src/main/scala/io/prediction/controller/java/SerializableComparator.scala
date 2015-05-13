package io.prediction.controller.java

import java.util.Comparator

trait SerializableComparator[T] extends Comparator[T] with java.io.Serializable
