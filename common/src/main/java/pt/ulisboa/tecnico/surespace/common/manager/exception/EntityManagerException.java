/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager.exception;

public class EntityManagerException extends ManagerException {
  private static final long serialVersionUID = -1174068022394337806L;

  public EntityManagerException(String format, Object... objects) {
    super(format, objects);
  }
}
