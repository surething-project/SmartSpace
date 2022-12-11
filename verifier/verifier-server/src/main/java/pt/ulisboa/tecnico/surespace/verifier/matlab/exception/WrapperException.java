/*
 * Copyright (C) 2021 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.matlab.exception;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public class WrapperException extends BroadException {
  private static final long serialVersionUID = 4892209760447375060L;

  public WrapperException(String format, Object... objects) {
    super(format, objects);
  }
}
