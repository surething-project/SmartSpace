/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.exception;

import org.jetbrains.annotations.NotNull;

public class BroadException extends Exception {
  private static final long serialVersionUID = 3637454643243055832L;

  public BroadException(String format, Object... objects) {
    super(composeMessage(format, objects));
  }

  protected static String composeMessage(String format, Object... objects) {
    if (format == null) format = "No further details";
    return String.format(format, objects);
  }

  @Override
  @Deprecated
  public final String getLocalizedMessage() {
    return getMessage();
  }

  @Override
  public @NotNull String getMessage() {
    return super.getMessage();
  }
}
