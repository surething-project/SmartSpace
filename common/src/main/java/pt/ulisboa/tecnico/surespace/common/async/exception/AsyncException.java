/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.async.exception;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class AsyncException extends BroadException {
  private static final long serialVersionUID = 6374775843305398024L;

  public AsyncException(String format, Object... objects) {
    super(format, objects);
  }
}
