/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.domain;

import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

public final class TimeInterval extends Object<TimeInterval> {
  private static final long serialVersionUID = 1560403971156781406L;
  private final Timestamp notAfter;
  private final Timestamp notBefore;

  public TimeInterval(Timestamp notBefore, Timestamp notAfter) {
    if (notBefore == null) throw new IllegalArgumentException("Invalid not before time");
    if (notAfter == null) throw new IllegalArgumentException("Invalid not after time");

    this.notBefore = notBefore.clone();
    this.notAfter = notAfter.clone();
  }

  public TimeInterval(long amountToAdd, TemporalUnit temporalUnit) {
    if (amountToAdd < 0) throw new IllegalArgumentException("Negative amount: " + amountToAdd);
    if (temporalUnit == null) throw new IllegalArgumentException("Null temporal unit provided");

    Instant now = Instant.now();
    this.notBefore = new Timestamp(now.minus(amountToAdd, temporalUnit));
    this.notAfter = new Timestamp(now.plus(amountToAdd, temporalUnit));
  }

  @Override
  public TimeInterval clone() {
    return this;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof TimeInterval)) return false;
    TimeInterval that = (TimeInterval) o;
    return notAfter.equals(that.notAfter) && notBefore.equals(that.notBefore);
  }

  public Timestamp getNotAfter() {
    return notAfter.clone();
  }

  public Timestamp getNotBefore() {
    return notBefore.clone();
  }

  @Override
  public int hashCode() {
    return Objects.hash(notAfter, notBefore);
  }

  public boolean isValid(Timestamp timestamp) {
    if (timestamp == null) throw new IllegalArgumentException("Provided a null timestamp");
    return !timestamp.isBefore(timestamp) && !timestamp.isAfter(timestamp);
  }

  public boolean isValid() {
    return isValid(new Timestamp(Instant.now()));
  }

  @Override
  public String toString() {
    return "TimeInterval{" + "notAfter=" + notAfter + ", notBefore=" + notBefore + '}';
  }
}
