/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.domain.exception;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class LongTermCAException extends BroadException {
  private static final long serialVersionUID = -8357734795263608378L;

  public LongTermCAException(String format, Object... objects) {
    super(format, objects);
  }
}
