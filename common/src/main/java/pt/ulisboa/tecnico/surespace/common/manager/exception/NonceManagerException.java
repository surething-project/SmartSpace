/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager.exception;

public class NonceManagerException extends ManagerException {
  private static final long serialVersionUID = -7397480611575561288L;

  public NonceManagerException(String format, Object... objects) {
    super(format, objects);
  }
}
