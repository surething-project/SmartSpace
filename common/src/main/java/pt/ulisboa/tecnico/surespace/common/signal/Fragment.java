/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.signal;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.proof.Device;
import pt.ulisboa.tecnico.surespace.common.signal.property.Property;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

public final class Fragment extends Object<Fragment> {
  private static final long serialVersionUID = -8168884574114321764L;
  private final int identifier;
  private final HashMap<Pair<Device, Property>, LinkedList<Pair<Long, String>>> readings =
      new HashMap<>();

  public Fragment(int identifier) {
    if (identifier < 1) throw new IllegalArgumentException("Identifier must not be less than 1");
    this.identifier = identifier;
  }

  public void addReading(Reading reading) {
    // Check if <device, property> already exists.
    Pair<Device, Property> key = new ImmutablePair<>(reading.getDevice(), reading.getProperty());
    readings.putIfAbsent(key, new LinkedList<>());

    // Create <time, value> pair.
    ImmutablePair<Long, String> pair = new ImmutablePair<>(reading.getTime(), reading.getValue());
    readings.get(key).add(pair);
  }

  @Override
  public Fragment clone() {
    Fragment fragment = new Fragment(identifier);
    for (Pair<Device, Property> key : readings.keySet())
      for (Pair<Long, String> pair : readings.get(key))
        fragment.addReading(
            new Reading(key.getLeft(), key.getRight(), pair.getLeft(), pair.getRight()));

    return fragment;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof Fragment)) return false;
    Fragment fragment = (Fragment) o;
    return identifier == fragment.identifier && readings.equals(fragment.readings);
  }

  public int getIdentifier() {
    return identifier;
  }

  public HashMap<Pair<Device, Property>, LinkedList<Pair<Long, String>>> getReadings() {
    HashMap<Pair<Device, Property>, LinkedList<Pair<Long, String>>> readings = new HashMap<>();
    for (Pair<Device, Property> key : this.readings.keySet()) {
      LinkedList<Pair<Long, String>> pairs = new LinkedList<>();
      for (Pair<Long, String> pair : this.readings.get(key))
        pairs.add(new ImmutablePair<>(pair.getLeft(), pair.getRight()));
      readings.put(key, pairs);
    }

    return readings;
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, readings);
  }

  @Override
  public String toString() {
    return "Fragment{" + "identifier=" + identifier + ", readings=" + readings + '}';
  }
}
