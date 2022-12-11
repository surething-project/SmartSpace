/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.domain.exception;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public class VerifierException extends BroadException {
  private static final long serialVersionUID = 8989822174712947013L;

  public VerifierException(String format, Object... objects) {
    super(format, objects);
  }
}
