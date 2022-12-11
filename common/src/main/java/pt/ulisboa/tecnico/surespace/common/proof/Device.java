/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.proof;

import pt.ulisboa.tecnico.surespace.common.domain.Object;

import java.util.Objects;

public abstract class Device extends Object<Device> {
  private static final long serialVersionUID = -1240604697387236722L;
  protected final String identifier;
  protected final String typedIdentifier;

  public Device(String identifier) {
    this.identifier = identifier;
    this.typedIdentifier = getType() + "_" + identifier;
  }

  @Override
  public final boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof Device)) return false;
    Device device = (Device) o;
    return identifier.equals(device.identifier) && typedIdentifier.equals(device.typedIdentifier);
  }

  public final String getIdentifier() {
    return identifier;
  }

  public abstract String getType();

  public final String getTypedIdentifier() {
    return typedIdentifier;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(identifier, typedIdentifier);
  }

  @Override
  public final String toString() {
    return "Device{"
        + "identifier='"
        + identifier
        + '\''
        + ", typedIdentifier='"
        + typedIdentifier
        + '\''
        + '}';
  }
}
