/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

public final class Timestamp extends Object<Timestamp> {
  private static final long serialVersionUID = -293866688218803286L;
  private Instant instant;

  public Timestamp(Instant instant) {
    setInstant(instant);
  }

  public Timestamp() {
    this(Instant.now());
  }

  @Override
  public Timestamp clone() {
    return this;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof Timestamp)) return false;
    Timestamp that = (Timestamp) o;
    return instant.equals(that.instant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instant);
  }

  public boolean isAfter(Timestamp timestamp) {
    if (timestamp == null) throw new IllegalArgumentException("Provided a null timestamp");
    return instant.isAfter(timestamp.instant);
  }

  public boolean isBefore(Timestamp timestamp) {
    if (timestamp == null) throw new IllegalArgumentException("Provided a null timestamp");
    return instant.isBefore(timestamp.instant);
  }

  public void setInstant(Instant instant) {
    if (instant == null) throw new IllegalArgumentException("Provided a null instant");
    this.instant = Instant.from(instant);
  }

  public Date toDate() {
    return Date.from(instant);
  }

  public Instant toInstant() {
    return Instant.from(instant);
  }

  public LocalDate toLocalDate() {
    return LocalDate.from(instant);
  }

  public LocalDateTime toLocalDateTime() {
    return LocalDateTime.from(instant);
  }

  @Override
  public String toString() {
    return "Timestamp{" + "instant=" + instant + '}';
  }
}
