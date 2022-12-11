/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.client;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class LongTermCAClientException extends BroadException {
  private static final long serialVersionUID = 8299573595013459290L;

  public LongTermCAClientException(String format, Object... objects) {
    super(format, objects);
  }
}
