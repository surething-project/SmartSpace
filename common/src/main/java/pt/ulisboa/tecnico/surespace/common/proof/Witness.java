/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.proof;

public final class Witness extends Device {
  private static final long serialVersionUID = 8499026931351442082L;

  public Witness(String identifier) {
    super(identifier);
  }

  public Witness(Beacon beacon) {
    super(beacon.getIdentifier());
  }

  @Override
  public Witness clone() {
    return new Witness(identifier);
  }

  @Override
  public String getType() {
    return "witness";
  }
}
