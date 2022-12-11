/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.signal;

import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.proof.Beacon;

import java.util.LinkedList;
import java.util.Objects;

public final class Signal extends Object<Signal> {
  private static final long serialVersionUID = 7478185755664365466L;
  private final Beacon beacon;
  private final LinkedList<Fragment> fragments = new LinkedList<>();

  public Signal(Beacon beacon) {
    this.beacon = beacon.clone();
  }

  public void addFragment(Fragment fragment) {
    fragments.add(fragment.clone());
  }

  @Override
  public Signal clone() {
    Signal signal = new Signal(beacon);
    for (Fragment fragment : fragments) signal.addFragment(fragment);
    return signal;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof Signal)) return false;
    Signal signal = (Signal) o;
    return beacon.equals(signal.beacon) && fragments.equals(signal.fragments);
  }

  public Beacon getBeacon() {
    return beacon.clone();
  }

  public LinkedList<Fragment> getFragments() {
    LinkedList<Fragment> fragments = new LinkedList<>();
    for (Fragment fragment : this.fragments) fragments.add(fragment.clone());
    return fragments;
  }

  @Override
  public int hashCode() {
    return Objects.hash(beacon, fragments);
  }

  @Override
  public String toString() {
    return "Signal{" + "beacon=" + beacon + ", fragments=" + fragments + '}';
  }
}
