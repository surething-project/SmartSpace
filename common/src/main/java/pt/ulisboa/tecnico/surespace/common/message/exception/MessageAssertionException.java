/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message.exception;

public final class MessageAssertionException extends MessageValidatorException {
  private static final long serialVersionUID = 4446752253448985304L;

  public MessageAssertionException(Object expected, Object received) {
    this("Failed assertion: expected '%s', but received '%s'", expected, received);
  }

  private MessageAssertionException(String format, Object... objects) {
    super(format, objects);
  }
}
