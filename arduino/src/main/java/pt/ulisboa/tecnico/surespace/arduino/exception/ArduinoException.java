/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.exception;

public class ArduinoException extends Exception {
  private static final long serialVersionUID = -5678206585900622040L;

  public ArduinoException(String format, Object... objects) {
    super(composeMessage(format, objects));
  }

  protected static String composeMessage(String format, Object... objects) {
    if (format == null) format = "No further details";
    return String.format(format, objects);
  }
}
