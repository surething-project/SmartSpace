/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.location;

import org.jetbrains.annotations.NotNull;
import pt.ulisboa.tecnico.surespace.common.domain.Object;

import java.util.Objects;

import static pt.ulisboa.tecnico.surespace.common.location.LocationProximity.ProximityCode.FAR;
import static pt.ulisboa.tecnico.surespace.common.location.LocationProximity.ProximityCode.NEAR;

public final class LocationProximity extends Object<LocationProximity>
    implements Comparable<LocationProximity> {
  private static final long serialVersionUID = 1746130431140620358L;
  private ProximityCode code = FAR;
  private double confidence = 0;

  @Override
  public LocationProximity clone() {
    return this;
  }

  @Override
  public int compareTo(@NotNull LocationProximity o) {
    if (this.code == o.code)
      return this.confidence - o.confidence == 0.0 ? 0 : (this.confidence > o.confidence ? 1 : -1);
    else
      switch (this.code) {
        case NEAR:
          return 1;
        case MID:
          return o.code == NEAR ? -1 : 1;
        case FAR:
          return -1;
      }

    throw new IllegalArgumentException();
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof LocationProximity)) return false;
    LocationProximity that = (LocationProximity) o;
    return Double.compare(that.confidence, confidence) == 0 && code == that.code;
  }

  public ProximityCode getCode() {
    return code;
  }

  public void setCode(ProximityCode code) {
    this.code = code;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double confidence) {
    if (confidence < 0 || confidence > 1)
      throw new IllegalArgumentException(
          "Invalid confidence '" + confidence + "': must be in range [0, 1]");
    this.confidence = confidence;
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, confidence);
  }

  public boolean inRange(ProximityCode... codes) {
    for (ProximityCode code : codes) if (this.code.equals(code)) return true;
    return false;
  }

  @Override
  public String toString() {
    return "LocationProximity{" + "code=" + code + ", confidence=" + confidence + '}';
  }

  public enum ProximityCode {
    NEAR,
    MID,
    FAR
  }
}
