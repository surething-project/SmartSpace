/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.signal.property;

public final class Intensity extends Property {
  private static final long serialVersionUID = -8481544266437720385L;

  public Intensity() {
    super("intensity");
  }

  @Override
  public final Intensity clone() {
    return new Intensity();
  }
}
