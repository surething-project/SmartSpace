/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.proof;

public final class Beacon extends Device {
  private static final long serialVersionUID = -6124699224267133003L;

  public Beacon(String identifier) {
    super(identifier);
  }

  @Override
  public Beacon clone() {
    return new Beacon(identifier);
  }

  @Override
  public String getType() {
    return "beacon";
  }
}
