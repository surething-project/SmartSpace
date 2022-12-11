/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.pin;

import org.ardulink.core.Pin.AnalogPin;
import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;

import java.io.IOException;

public class AnalogArduinoPin extends ArduinoPin {
  private final AnalogPin analogPin;

  public AnalogArduinoPin(int id) throws ArduinoException {
    super(id);
    analogPin = AnalogPin.analogPin(id);
  }

  @Override
  protected final AnalogPin getPin() {
    return analogPin;
  }

  public final void write(int value) throws ArduinoException {
    try {
      LOGGER.info("[*] Writing '{}'.", value);
      link.switchAnalogPin(analogPin, value);

    } catch (IOException e) {
      throw new ArduinoException(e.getMessage());
    }
  }
}
