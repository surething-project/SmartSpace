/*
 * Copyright (C) 2021 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.matlab.exception;

public final class SignalProcessingException extends WrapperException {
  private static final long serialVersionUID = -4437371175467757903L;

  public SignalProcessingException(String format, Object... objects) {
    super(format, objects);
  }
}
