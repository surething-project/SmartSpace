/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.device;

import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;

public final class SoundWitness extends SensorWitness {
  public SoundWitness(int id) throws ArduinoException {
    super(id);
  }
}
