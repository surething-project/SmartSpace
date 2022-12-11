/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager.exception;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public class ManagerException extends BroadException {
  private static final long serialVersionUID = 43471385623432383L;

  public ManagerException(String format, Object... objects) {
    super(format, objects);
  }
}
