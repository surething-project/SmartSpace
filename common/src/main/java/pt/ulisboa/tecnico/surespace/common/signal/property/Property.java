/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.signal.property;

import pt.ulisboa.tecnico.surespace.common.domain.Object;

import java.util.Objects;

public abstract class Property extends Object<Property> {
  private static final long serialVersionUID = -8662673590725290175L;
  private final String identifier;

  public Property(String identifier) {
    this.identifier = identifier;
  }

  @Override
  public final boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof Property)) return false;
    Property property = (Property) o;
    return identifier.equals(property.identifier);
  }

  public final String getIdentifier() {
    return identifier;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(identifier);
  }

  @Override
  public final String toString() {
    return "Property{" + "identifier='" + identifier + '\'' + '}';
  }
}
