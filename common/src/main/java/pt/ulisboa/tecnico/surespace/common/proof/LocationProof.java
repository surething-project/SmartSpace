/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.proof;

import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationResponse;
import pt.ulisboa.tecnico.surespace.common.signal.Signal;

import java.util.LinkedList;
import java.util.Objects;

public final class LocationProof extends Object<LocationProof> {
  private static final long serialVersionUID = 1858359377808051280L;
  private final SignedRequestAuthorizationResponse authorization;
  private final LinkedList<Signal> signals = new LinkedList<>();

  public LocationProof(SignedRequestAuthorizationResponse authorization) {
    this.authorization = authorization.clone();
  }

  public void addSignal(Signal signal) {
    signals.add(signal.clone());
  }

  @Override
  public LocationProof clone() {
    LocationProof proof = new LocationProof(authorization);
    for (Signal signal : signals) proof.addSignal(signal);
    return proof;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof LocationProof)) return false;
    LocationProof proof = (LocationProof) o;
    return authorization.equals(proof.authorization) && signals.equals(proof.signals);
  }

  public SignedRequestAuthorizationResponse getAuthorization() {
    return authorization.clone();
  }

  public LinkedList<Signal> getSignals() {
    LinkedList<Signal> signals = new LinkedList<>();
    for (Signal signal : this.signals) signals.add(signal.clone());
    return signals;
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorization, signals);
  }

  @Override
  public String toString() {
    return "LocationProof{" + "authorization=" + authorization + ", signals=" + signals + '}';
  }
}
