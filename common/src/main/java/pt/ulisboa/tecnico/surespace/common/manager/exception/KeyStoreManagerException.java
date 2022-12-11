/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager.exception;

public class KeyStoreManagerException extends ManagerException {
  private static final long serialVersionUID = -773287474127052615L;

  public KeyStoreManagerException(String format, Object... objects) {
    super(format, objects);
  }
}
