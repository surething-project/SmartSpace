/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.pin;

import org.ardulink.core.Link;
import org.ardulink.core.Pin;
import org.ardulink.core.convenience.Links;
import org.ardulink.core.events.RplyEvent;
import org.ardulink.core.qos.ResponseAwaiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;
import pt.ulisboa.tecnico.surespace.arduino.pin.listener.ArduinoListener;
import pt.ulisboa.tecnico.surespace.arduino.pin.listener.ArduinoPinListener;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class ArduinoPin implements AutoCloseable {
  protected static final Logger LOGGER = LoggerFactory.getLogger(ArduinoPin.class);
  protected static final int MAX_RETRIES = 3;
  protected static final int TIMEOUT = 280;
  private static final int BAUD_RATE = 9600;
  private static final String CUSTOM_PIN_MODE = "pinMode";
  private static final AtomicInteger PIN_COUNTER = new AtomicInteger(0);
  protected static Link link;
  private final ConcurrentHashMap<Long, ArduinoPinListener> eventListeners;
  private final int pinId;

  public ArduinoPin(int pinId) throws ArduinoException {
    eventListeners = new ConcurrentHashMap<>();

    try {
      if (PIN_COUNTER.getAndIncrement() == 0) {
        // Cannot retrieve a default link, because we require a higher baud rate.
        link = Links.getLink("ardulink://serial-jssc?port=COM3&baudrate=" + BAUD_RATE);
        LOGGER.info("[+] Retrieved default instance instance of Arduino link.");
      }

      this.pinId = pinId;
      LOGGER.info(String.format("[+] Created pin with ID %d.", pinId));

    } catch (Exception e) {
      e.printStackTrace();
      throw new ArduinoException("Could not initialize Arduino pin: " + e.getMessage());
    }
  }

  public final long addListener(long amount, ChronoUnit unit, ArduinoListener listener)
      throws ArduinoException {
    ArduinoPinListener pinListener = new ArduinoPinListener(amount, unit, getPin(), listener);

    try {
      link.addListener(pinListener);
      eventListeners.putIfAbsent(pinListener.getId(), pinListener);

    } catch (IOException e) {
      e.printStackTrace();
      throw new ArduinoException(e.getMessage());
    }

    return pinListener.getId();
  }

  @Override
  public void close() throws ArduinoException {
    try {
      // Remove any custom listeners.
      stopListening();
      removeAllListeners();

      if (PIN_COUNTER.decrementAndGet() == 0) {
        link.close();
        LOGGER.info("[+] Link instance was closed.");
      }

    } catch (Exception e) {
      LOGGER.error("[-] Link instance could not be closed.");
      throw new ArduinoException("Could not close link instance: " + e.getMessage());
    }
  }

  protected abstract Pin getPin();

  protected final int getPinId() {
    return pinId;
  }

  private void removeAllListeners() {
    Enumeration<Long> listenerIds = eventListeners.keys();
    while (listenerIds.hasMoreElements()) {
      removeListener(listenerIds.nextElement());
    }
  }

  public final boolean removeListener(long listenerId) {
    ArduinoPinListener listener = eventListeners.get(listenerId);
    if (listener == null) return false;

    try {
      link.removeListener(listener);
      eventListeners.remove(listenerId);
      return true;

    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  protected final RplyEvent sendCustomMessage(Object... objectArgs) throws ArduinoException {
    ArrayList<String> args = new ArrayList<>();
    for (Object argument : objectArgs) args.add(String.valueOf(argument));

    for (int i = 1; i <= MAX_RETRIES; i++) {
      try {
        LOGGER.info("[+] Sending custom message {} to link.", args);
        return ResponseAwaiter.onLink(link)
            .withTimeout(TIMEOUT, MILLISECONDS)
            .waitForResponse(link.sendCustomMessage(args.toArray(new String[0])));

      } catch (Exception e) {
        LOGGER.error("[-] Attempt {}/{} failed to deliver message {}.", i, MAX_RETRIES, args);
      }
    }

    LOGGER.error("[-] Could not complete custom message call {}.", args);
    throw new ArduinoException("Could not send custom message");
  }

  protected final RplyEvent setPinMode(PinMode pinMode) throws ArduinoException {
    LOGGER.info(String.format("[+] Setting pin mode to %s for pin %d.", pinMode, getPinId()));
    return sendCustomMessage(CUSTOM_PIN_MODE, valueOf(pinId), pinMode.toString());
  }

  public final void startListening() throws ArduinoException {
    try {
      link.startListening(getPin());

    } catch (IOException e) {
      throw new ArduinoException(e.getMessage());
    }
  }

  public final void stopListening() throws ArduinoException {
    try {
      link.stopListening(getPin());

    } catch (IOException e) {
      LOGGER.error("[-] Could not stop listening on pin.");
      throw new ArduinoException(e.getMessage());
    }
  }

  protected enum PinMode {
    OUTPUT,
    INPUT
  }
}
