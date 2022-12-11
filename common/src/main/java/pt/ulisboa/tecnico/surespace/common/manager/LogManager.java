/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;

public abstract class LogManager implements LogManagerInterface {
  public LogManager() throws LogManagerException {
    beforeLoading();
    afterLoading();
  }
}
