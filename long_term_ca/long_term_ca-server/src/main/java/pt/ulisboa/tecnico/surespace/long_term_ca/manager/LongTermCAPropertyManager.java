/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.manager;

import pt.ulisboa.tecnico.surespace.common.manager.LogManagerInterface;
import pt.ulisboa.tecnico.surespace.common.manager.PropertyManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;

public final class LongTermCAPropertyManager extends PropertyManager {
  private static final String FILE_PATH = "long_term_ca.properties";

  public LongTermCAPropertyManager(LogManagerInterface logManager) throws PropertyManagerException {
    super(logManager);
    beforeLoading();
  }

  @Override
  public void beforeLoading() throws PropertyManagerException {
    super.beforeLoading();
    extend(LongTermCAPropertyManager.class.getClassLoader().getResourceAsStream(FILE_PATH));
  }
}
