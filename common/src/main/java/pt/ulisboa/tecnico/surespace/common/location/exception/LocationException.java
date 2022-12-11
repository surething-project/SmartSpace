/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.location.exception;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class LocationException extends BroadException {
  private static final long serialVersionUID = -6427072597177514595L;

  public LocationException(String format, Object... objects) {
    super(format, objects);
  }
}
