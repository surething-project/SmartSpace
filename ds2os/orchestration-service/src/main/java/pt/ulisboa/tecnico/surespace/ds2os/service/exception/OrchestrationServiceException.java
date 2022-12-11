/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.exception;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class OrchestrationServiceException extends BroadException {
  private static final long serialVersionUID = -6807252091730177170L;

  public OrchestrationServiceException(String format, Object... objects) {
    super(format, objects);
  }
}
