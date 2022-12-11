/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.async;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public interface AsyncListener<Result, Exception extends BroadException> {
  default void onComplete(Result result) {}

  default void onError(Exception e) {}
}
