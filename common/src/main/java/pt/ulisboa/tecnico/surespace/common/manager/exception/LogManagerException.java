/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager.exception;

public class LogManagerException extends ManagerException {
  private static final long serialVersionUID = 41648582195402615L;

  public LogManagerException(String format, Object... objects) {
    super(format, objects);
  }
}
