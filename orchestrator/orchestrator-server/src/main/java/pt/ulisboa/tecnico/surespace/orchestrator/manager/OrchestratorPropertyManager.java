/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.manager;

import pt.ulisboa.tecnico.surespace.common.manager.LogManagerInterface;
import pt.ulisboa.tecnico.surespace.common.manager.PropertyManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;

public final class OrchestratorPropertyManager extends PropertyManager {
  public OrchestratorPropertyManager(LogManagerInterface logManager)
      throws PropertyManagerException {
    super(logManager);
    beforeLoading();
  }

  @Override
  public void beforeLoading() throws PropertyManagerException {
    super.beforeLoading();
    extend(getClass().getClassLoader().getResourceAsStream("orchestrator.properties"));
  }
}
