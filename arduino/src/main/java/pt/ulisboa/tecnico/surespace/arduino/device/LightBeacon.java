/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.device;

import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;
import pt.ulisboa.tecnico.surespace.arduino.pin.DigitalArduinoPin;

import java.util.Timer;
import java.util.TimerTask;

public final class LightBeacon extends DigitalArduinoPin {
  private static final String COMMAND = "chainableLED";
  private boolean isOn = false;
  private Timer timer = null;

  public LightBeacon(int id) throws ArduinoException {
    super(id);

    if (!setPinMode(PinMode.OUTPUT).isOk())
      throw new ArduinoException("Could not set pin mode to output");

    turnOff();
  }

  private void changeIntensity(double intensity) throws ArduinoException {
    sendCustomMessage(COMMAND, getPinId(), intensity);
  }

  public boolean isOn() {
    return isOn;
  }

  public synchronized void start(long period) {
    if (timer == null) {
      LOGGER.info("[+] Starting timer task.");

      timer = new Timer();
      timer.schedule(
          new TimerTask() {
            @Override
            public void run() {
              try {
                if (isOn) turnOff();
                else turnOn();

              } catch (ArduinoException e) {
                stop();
              }
            }
          },
          0,
          period);
    }
  }

  public synchronized void stop() {
    if (timer != null) {
      LOGGER.info("[+] Stopping timer task.");

      timer.cancel();
      timer = null;

      try {
        turnOff();

      } catch (ArduinoException e) {
        e.printStackTrace();
      }
    }
  }

  private void turnOff() throws ArduinoException {
    changeIntensity(0);
    isOn = false;
  }

  private void turnOn() throws ArduinoException {
    changeIntensity(1.0);
    isOn = true;
  }
}
