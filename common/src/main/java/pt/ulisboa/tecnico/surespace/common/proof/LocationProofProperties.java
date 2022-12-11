/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.proof;

import pt.ulisboa.tecnico.surespace.common.domain.Object;

import java.util.Objects;

public class LocationProofProperties extends Object<LocationProofProperties> {
  private static final long serialVersionUID = 641462434763217121L;
  private int fragmentCount;
  private int fragmentLength;
  private String identifier;
  private transient long seed = 0;

  @Override
  public LocationProofProperties clone() {
    LocationProofProperties properties = new LocationProofProperties();
    properties.setFragmentCount(fragmentCount);
    properties.setFragmentLength(fragmentLength);
    properties.setSeed(seed);
    properties.setIdentifier(identifier);

    return properties;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof LocationProofProperties)) return false;
    LocationProofProperties that = (LocationProofProperties) o;
    return fragmentCount == that.fragmentCount
        && fragmentLength == that.fragmentLength
        && identifier.equals(that.identifier);
  }

  public int getFragmentCount() {
    return fragmentCount;
  }

  public void setFragmentCount(int fragmentCount) {
    this.fragmentCount = fragmentCount;
  }

  public int getFragmentLength() {
    return fragmentLength;
  }

  public void setFragmentLength(int fragmentLength) {
    this.fragmentLength = fragmentLength;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public long getSeed() {
    return seed;
  }

  public void setSeed(long seed) {
    this.seed = seed;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fragmentCount, fragmentLength, identifier);
  }

  @Override
  public String toString() {
    return "LocationProofProperties{"
        + "fragmentCount="
        + fragmentCount
        + ", fragmentLength="
        + fragmentLength
        + ", identifier='"
        + identifier
        + '\''
        + '}';
  }
}
