/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.domain;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public abstract class Object<T> implements Serializable, Cloneable {
  private static final long serialVersionUID = -3046420753132281222L;

  protected Object() {
    super();
  }

  public static <T extends Object<?>> T fromBytes(byte[] bytes, Class<T> type) {
    return type.cast(SerializationUtils.deserialize(bytes));
  }

  public String asString() {
    return toString();
  }

  @Override
  public abstract T clone();

  @Override
  public abstract boolean equals(java.lang.Object obj);

  public final byte[] getBytes() {
    return SerializationUtils.serialize(this);
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract String toString();
}
