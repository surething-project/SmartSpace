/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.arduino.pin.listener;

import org.ardulink.core.Pin;
import org.ardulink.core.events.AnalogPinValueChangedEvent;
import org.ardulink.core.events.DigitalPinValueChangedEvent;
import org.ardulink.core.events.EventListener;
import org.ardulink.core.events.PinValueChangedEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class ArduinoPinListener implements EventListener {
  private static final AtomicLong COUNTER = new AtomicLong(0);
  private final long amount;
  private final long id;
  private final ArduinoListener listener;
  private final Pin pin;
  private final ChronoUnit unit;
  private Instant lastCall = null;

  public ArduinoPinListener(long amount, ChronoUnit unit, Pin pin, ArduinoListener listener) {
    if (amount < 0) throw new IllegalArgumentException("Provided a negative time amount");
    if (unit == null) throw new IllegalArgumentException("Provided a null time unit");
    if (pin == null) throw new IllegalArgumentException("Provided a null pin");
    if (listener == null) throw new IllegalArgumentException("Provided a null listener");

    this.amount = amount;
    this.unit = unit;
    this.pin = pin;
    this.listener = listener;
    this.id = COUNTER.incrementAndGet();
  }

  private boolean enoughTimeElapsed() {
    final Instant now = Instant.now();

    if (lastCall == null || !now.isBefore(lastCall.plus(amount, unit))) {
      lastCall = now;
      return true;
    }

    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArduinoPinListener)) return false;
    ArduinoPinListener that = (ArduinoPinListener) o;
    return id == that.id;
  }

  public long getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  private boolean mustAccept(PinValueChangedEvent event) {
    return samePin(event) && enoughTimeElapsed();
  }

  private boolean samePin(PinValueChangedEvent event) {
    return event.getPin().equals(pin);
  }

  @Override
  public void stateChanged(AnalogPinValueChangedEvent event) {
    if (mustAccept(event)) listener.accept(event.getValue());
  }

  @Override
  public void stateChanged(DigitalPinValueChangedEvent event) {
    if (mustAccept(event)) listener.accept(event.getValue());
  }

  @Override
  public String toString() {
    return "PinListener{"
        + "amount="
        + amount
        + ", id="
        + id
        + ", lastCall="
        + lastCall
        + ", pin="
        + pin
        + ", unit="
        + unit
        + '}';
  }
}
