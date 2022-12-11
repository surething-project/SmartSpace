/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public interface ManagerInterface<Exception extends BroadException> {
  default void afterLoading() throws Exception {}

  default void beforeLoading() throws Exception {}
}
