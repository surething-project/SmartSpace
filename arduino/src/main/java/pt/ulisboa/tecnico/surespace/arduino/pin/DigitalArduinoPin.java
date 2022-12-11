/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.pin;

import org.ardulink.core.Pin.DigitalPin;
import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;

import java.io.IOException;

public class DigitalArduinoPin extends ArduinoPin {
  private final DigitalPin digitalPin;

  public DigitalArduinoPin(int id) throws ArduinoException {
    super(id);
    digitalPin = DigitalPin.digitalPin(id);
  }

  @Override
  protected final DigitalPin getPin() {
    return digitalPin;
  }

  public final void write(boolean value) throws ArduinoException {
    try {
      link.switchDigitalPin(digitalPin, value);

    } catch (IOException e) {
      throw new ArduinoException(e.getMessage());
    }
  }
}
