/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.client;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class VerifierClientException extends BroadException {
  private static final long serialVersionUID = 4738708946072859127L;

  public VerifierClientException(String format, Object... objects) {
    super(format, objects);
  }
}
