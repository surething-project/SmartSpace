/*
 * Copyright (C) 2021 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.matlab;

import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationResponse;
import pt.ulisboa.tecnico.surespace.common.proof.Beacon;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProof;
import pt.ulisboa.tecnico.surespace.common.signal.Signal;

import java.util.LinkedHashSet;
import java.util.LinkedList;

public class LocationProofView {
  private final SignedRequestAuthorizationResponse authorization;
  private final LinkedHashSet<Beacon> beacons = new LinkedHashSet<>();
  private final LinkedHashSet<Signal> trustedSignals;
  private final LinkedHashSet<Signal> untrustedSignals;

  public LocationProofView(
      LocationProof locationProof,
      LinkedList<Signal> trustedSignals,
      LinkedList<Signal> untrustedSignals) {
    this.authorization = locationProof.getAuthorization();
    this.trustedSignals = new LinkedHashSet<>(trustedSignals);
    this.untrustedSignals = new LinkedHashSet<>(untrustedSignals);

    // Add beacons.
    for (Signal signal : locationProof.getSignals()) {
      beacons.add(signal.getBeacon());
    }
  }

  public SignedRequestAuthorizationResponse getAuthorization() {
    return authorization;
  }

  public LinkedHashSet<Beacon> getBeacons() {
    return beacons;
  }

  public LinkedHashSet<Signal> getTrustedSignals() {
    return trustedSignals;
  }

  public LinkedHashSet<Signal> getUntrustedSignals() {
    return untrustedSignals;
  }
}
