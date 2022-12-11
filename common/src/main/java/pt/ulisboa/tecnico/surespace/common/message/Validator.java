/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message;

import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.manager.GlobalManagerInterface;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageValidatorException;

public abstract class Validator<Type extends Object<?>> {
  protected final GlobalManagerInterface manager;
  private MessageValidatorException exception;

  public Validator(GlobalManagerInterface manager) {
    if (manager == null) throw new IllegalArgumentException("Provided a null manager");
    this.manager = manager;
  }

  public abstract Validator<Type> init(Type initiator);

  protected abstract boolean isInited();

  protected final boolean noException() {
    return null == exception;
  }

  protected abstract void reset();

  protected final void setException(MessageValidatorException exception) {
    this.exception = exception;
  }

  protected final boolean shouldStop() {
    return !isInited() || !noException();
  }

  public final void validate() throws MessageValidatorException {
    reset();

    if (null != exception) {
      try {
        throw exception;

      } finally {
        exception = null;
      }
    }
  }
}
