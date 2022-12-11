/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.domain.exception;

import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class ObjectException extends BroadException {
  private static final long serialVersionUID = 4038520561360682910L;

  public ObjectException(Object<?> object) {
    super("Error with object '%s'", object);
  }

  public ObjectException(Object<?> object, String format, java.lang.Object... objects) {
    super("Error with object '%s': %s", object, composeMessage(format, objects));
  }
}
