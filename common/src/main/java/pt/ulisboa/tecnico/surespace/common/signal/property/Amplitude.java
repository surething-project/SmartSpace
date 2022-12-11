/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.signal.property;

public final class Amplitude extends Property {
  private static final long serialVersionUID = 337078734443544167L;

  public Amplitude() {
    super("amplitude");
  }

  @Override
  public final Amplitude clone() {
    return new Amplitude();
  }
}
