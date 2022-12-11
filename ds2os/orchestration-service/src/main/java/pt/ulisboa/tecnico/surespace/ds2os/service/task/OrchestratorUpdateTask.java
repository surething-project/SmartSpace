/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.task;

import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.surespace.common.exception.BroadException;
import pt.ulisboa.tecnico.surespace.ds2os.service.OrchestrationService;

import java.util.TimerTask;

public final class OrchestratorUpdateTask extends TimerTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorUpdateTask.class);
  private final OrchestrationService service;

  public OrchestratorUpdateTask(OrchestrationService service) {
    this.service = service;
  }

  @Override
  public void run() {
    try {
      service.updateAgentInformation();

    } catch (VslException | BroadException e) {
      LOGGER.error("[-] Could not update agent information.");
    }
  }
}
