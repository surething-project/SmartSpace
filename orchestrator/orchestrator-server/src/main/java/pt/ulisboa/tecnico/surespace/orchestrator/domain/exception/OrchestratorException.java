/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.domain.exception;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class OrchestratorException extends BroadException {
  private static final long serialVersionUID = 4925030147812332569L;

  public OrchestratorException(String format, Object... objects) {
    super(format, objects);
  }
}
