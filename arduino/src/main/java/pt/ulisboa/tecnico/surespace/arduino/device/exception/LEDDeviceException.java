/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.device.exception;

import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;

/** The type Led device exception. */
public class LEDDeviceException extends ArduinoException {
  private static final long serialVersionUID = 5596846581675307513L;

  /**
   * Instantiates a new {@link LEDDeviceException}.
   *
   * @param message the message
   */
  public LEDDeviceException(String message) {
    super(message);
  }
}
