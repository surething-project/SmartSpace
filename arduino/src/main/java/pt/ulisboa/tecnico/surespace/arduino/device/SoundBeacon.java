/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.device;

import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;
import pt.ulisboa.tecnico.surespace.arduino.pin.DigitalArduinoPin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SoundBeacon extends DigitalArduinoPin {
  public static final int VOLUME_HIG = 23;
  public static final int VOLUME_LOW = 7;
  public static final int VOLUME_MAX = 30;
  public static final int VOLUME_MED = 15;
  public static final int VOLUME_MIN = 0;
  private static final String COMMAND = "mp3";
  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
  private static final int VOLUME_TIMEOUT = 25;
  private int currentVolume = -1;

  public SoundBeacon(int id) throws ArduinoException {
    super(id);

    if (!setPinMode(PinMode.OUTPUT).isOk())
      throw new ArduinoException("Could not set pin mode to output");

    stop();
    setEffectiveVolume(VOLUME_MIN);
  }

  @Override
  public void close() throws ArduinoException {
    setEffectiveVolume(VOLUME_MIN);
    stop();

    super.close();
  }

  public void nextSong() throws ArduinoException {
    if (!sendCustomMessage(COMMAND, getPinId(), "next").isOk())
      throw new ArduinoException("Could not play next song");
  }

  public void pauseSong() throws ArduinoException {
    if (!sendCustomMessage(COMMAND, getPinId(), "pause").isOk())
      throw new ArduinoException("Could not pause player");
  }

  public void playSong(int songId) {
    try {
      if (!sendCustomMessage(COMMAND, getPinId(), "play", songId).isOk())
        LOGGER.error("[-] Could not play song with ID {}.", songId);

    } catch (ArduinoException e) {
      e.printStackTrace();
    }
  }

  public void previousSong() throws ArduinoException {
    if (!sendCustomMessage(COMMAND, getPinId(), "previous").isOk())
      throw new ArduinoException("Could not play previous song");
  }

  public void resumeSong() throws ArduinoException {
    if (!sendCustomMessage(COMMAND, getPinId(), "resume").isOk())
      throw new ArduinoException("Could not resume player");
  }

  private void setEffectiveVolume(int volume) {
    try {
      if (!sendCustomMessage(COMMAND, getPinId(), "volume", volume).isOk()) {
        LOGGER.error("[-] Could not set volume to {}.", volume);

      } else {
        currentVolume = volume;
        LOGGER.info("[*] Volume set to {}.", volume);
      }

    } catch (ArduinoException e) {
      e.printStackTrace();
    }
  }

  public void setVolume(int desiredVolume) {
    if (desiredVolume < 0 || desiredVolume > 30) {
      LOGGER.error("[-] Volume must be within range [{}, {}].", VOLUME_LOW, VOLUME_MAX);

    } else {
      //      EXECUTOR.submit(
      //          () -> {
      //            if (desiredVolume > currentVolume)
      //              while (currentVolume < desiredVolume) setVolumeAndWait(++currentVolume);
      //            else if (desiredVolume < currentVolume)
      //              while (currentVolume > desiredVolume) setVolumeAndWait(--currentVolume);
      //            else LOGGER.error("[-] Volume already set to {}.", desiredVolume);
      //          });
      setVolumeAndWait(desiredVolume);
    }
  }

  private void setVolumeAndWait(int volume) {
    setEffectiveVolume(volume);

    //    try {
    //      TimeUnit.MILLISECONDS.sleep(VOLUME_TIMEOUT);
    //
    //    } catch (InterruptedException e) {
    //      e.printStackTrace();
    //    }
  }

  public void stop() {
    try {
      if (!sendCustomMessage(COMMAND, getPinId(), "stop").isOk())
        LOGGER.error("[-] Could not stop player.");

    } catch (ArduinoException e) {
      e.printStackTrace();
    }
  }
}
