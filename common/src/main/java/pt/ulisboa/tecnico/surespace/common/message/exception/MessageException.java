/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message.exception;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class MessageException extends BroadException {
  private static final long serialVersionUID = -3796225067378334660L;

  public MessageException(String format, Object... objects) {
    super(format, objects);
  }
}
