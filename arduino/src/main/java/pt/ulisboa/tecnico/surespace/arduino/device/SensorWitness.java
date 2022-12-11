/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.device;

import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;
import pt.ulisboa.tecnico.surespace.arduino.pin.AnalogArduinoPin;
import pt.ulisboa.tecnico.surespace.arduino.pin.ArduinoPin;
import pt.ulisboa.tecnico.surespace.arduino.pin.listener.ArduinoListener;

import static java.time.temporal.ChronoUnit.MILLIS;

public abstract class SensorWitness extends AnalogArduinoPin {
  private long listenerId = -1;

  public SensorWitness(int id) throws ArduinoException {
    super(id);

    if (!setPinMode(ArduinoPin.PinMode.INPUT).isOk())
      throw new ArduinoException("Could not set pin mode to input");
  }

  public synchronized void start(long period, ArduinoListener listener) throws ArduinoException {
    listenerId = addListener(period, MILLIS, listener);
    startListening();
  }

  public synchronized void stop() throws ArduinoException {
    if (!removeListener(listenerId))
      throw new ArduinoException("Could not remove listener with ID {}.", listenerId);

    listenerId = -1;
    stopListening();
  }
}
