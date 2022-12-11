/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.domain.Nonce;
import pt.ulisboa.tecnico.surespace.common.manager.exception.NonceManagerException;

public interface NonceManagerInterface extends ManagerInterface<NonceManagerException> {
  Nonce next(Entity entity);

  boolean valid(Nonce nonce, Entity entity);
}
