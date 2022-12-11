/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;

import java.util.Objects;

public interface PropertyManagerInterface extends ManagerInterface<PropertyManagerException> {
  String DELIMITER = ".";

  default String composePath(String... keyPath) {
    return String.join(DELIMITER, keyPath);
  }

  Property get(String... keyPath);

  boolean has(String... keyPath);

  Property set(String key, String value);

  void unset(String... keyPath);

  final class Property {
    private final String key;
    private final Object value;

    protected Property(String key, Object value) {
      this.key = key;
      this.value = value;
    }

    public char[] asCharArray() {
      return asString().toCharArray();
    }

    public float asFloat() {
      return Float.parseFloat(asString());
    }

    public int asInt() {
      return Integer.parseInt(asString());
    }

    public long asLong() {
      return Long.parseLong(asString());
    }

    public String asString() {
      return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Property)) return false;
      Property that = (Property) o;
      return value.equals(that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Property{" + "key='" + key + '\'' + ", value=" + value + '}';
    }
  }
}
