/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.core.VslLockHandler;
import org.ds2os.vsl.exception.VslException;

public interface LockHandler extends VslLockHandler {
  @Override
  default void lockExpired(String address) throws VslException {}

  @Override
  default void lockWillExpire(String address) throws VslException {}
}
