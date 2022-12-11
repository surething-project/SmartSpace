/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.manager;

import pt.ulisboa.tecnico.surespace.common.manager.LogManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;

import java.util.logging.Logger;

public final class VerifierLogManager extends LogManager {
  private final Logger logger = Logger.getLogger(VerifierLogManager.class.getName());

  public VerifierLogManager() throws LogManagerException {
    super();
  }

  @Override
  public void debug(Object message) {
    logger.info(String.valueOf(message));
  }

  @Override
  public void error(Object message) {
    logger.warning(String.valueOf(message));
  }

  @Override
  public void info(Object message) {
    logger.info(String.valueOf(message));
  }

  @Override
  public void warning(Object message) {
    logger.warning(String.valueOf(message));
  }
}
