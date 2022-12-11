/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.client;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public final class OrchestratorClientException extends BroadException {
  private static final long serialVersionUID = -2917664726784999270L;

  public OrchestratorClientException(String format, Object... objects) {
    super(format, objects);
  }
}
