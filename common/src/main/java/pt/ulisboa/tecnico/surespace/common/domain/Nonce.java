/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.domain;

import java.util.Objects;

public final class Nonce extends Object<Nonce> {
  private static final long serialVersionUID = 1168293628948107898L;
  private final long value;

  public Nonce(long value) {
    this.value = value;
  }

  @Override
  public Nonce clone() {
    return this;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof Nonce)) return false;
    Nonce stNonce = (Nonce) o;
    return value == stNonce.value;
  }

  public long getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "Nonce{" + "value=" + value + '}';
  }
}
