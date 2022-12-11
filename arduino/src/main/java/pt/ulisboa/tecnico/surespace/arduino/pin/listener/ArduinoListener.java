/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.pin.listener;

public abstract class ArduinoListener {
  public void accept(Boolean value) {}

  public void accept(Integer value) {}
}
