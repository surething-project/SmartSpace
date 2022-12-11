/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.signal;

import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.proof.Device;
import pt.ulisboa.tecnico.surespace.common.signal.property.Property;

import java.util.Objects;

public final class Reading extends Object<Reading> {
  private static final long serialVersionUID = -311346376866554088L;
  private final Device device;
  private final Property property;
  private final long time;
  private final String value;

  public Reading(Device device, Property property, long time, String value) {
    this.device = device.clone();
    this.property = property.clone();
    this.time = time;
    this.value = value;
  }

  @Override
  public Reading clone() {
    return new Reading(device, property, time, value);
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof Reading)) return false;
    Reading reading = (Reading) o;
    return time == reading.time
        && device.equals(reading.device)
        && property.equals(reading.property)
        && value.equals(reading.value);
  }

  public Device getDevice() {
    return device.clone();
  }

  public Property getProperty() {
    return property.clone();
  }

  public long getTime() {
    return time;
  }

  public String getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(device, property, time, value);
  }

  @Override
  public String toString() {
    return "Reading{"
        + "device="
        + device
        + ", property="
        + property
        + ", time="
        + time
        + ", value='"
        + value
        + '\''
        + '}';
  }
}
