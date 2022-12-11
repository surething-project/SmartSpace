/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.arduino.device.LightBeacon;
import pt.ulisboa.tecnico.surespace.arduino.device.LightWitness;
import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;
import pt.ulisboa.tecnico.surespace.arduino.pin.listener.ArduinoListener;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.LightAdaptationService.WITNESS;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.LightAdaptationService.Witness.INTENSITY;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.LightAdaptationService.Witness.INTENSITY_SAMPLING_RATE;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.Property.VALUE;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SmartDevice.IS_ON;

public final class LightAdaptationService extends AdaptationService {
  private static final double BEACON_MAX_PERIOD = 5500.0;
  private static final double BEACON_MIN_PERIOD = 2000.0;
  private static final int BEACON_PORT = 5;
  private static final int WITNESS_PERIOD = 50;
  private static final int WITNESS_PORT = 3;
  private final LightBeacon beacon;
  private final LinkedList<Integer> randomPeriods = new LinkedList<>();
  private final LightWitness witness;
  private final RegularNode witnessIntensitySamplingRateValue;
  private final RegularNode witnessIntensityValue;
  private final RegularNode witnessIsOn;

  public LightAdaptationService(ServiceInitializer init)
      throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
          VslException, ArduinoException {
    super(init.setServiceModelId("services/lightadaptationservice"));

    final RegularNode witnessNode = (RegularNode) getNode(WITNESS);
    witnessIsOn = witnessNode.child(IS_ON);

    final RegularNode witnessIntensity = witnessNode.child(INTENSITY);
    witnessIntensityValue = witnessIntensity.child(VALUE);

    final RegularNode witnessIntensitySamplingRate = witnessNode.child(INTENSITY_SAMPLING_RATE);
    witnessIntensitySamplingRateValue = witnessIntensitySamplingRate.child(VALUE);

    // Connect to Arduino Uno board.
    beacon = new LightBeacon(BEACON_PORT);
    witness = new LightWitness(WITNESS_PORT);

    serviceBootstrap();
  }

  @Override
  protected void beaconReset() throws VslException {
    beaconIsOn.setValue(false);
  }

  @Override
  protected void beaconStart() throws VslException {
    beaconIsOn.setValue(true);

    // Pick the first period.
    int period = randomPeriods.removeFirst();
    LOGGER.info("[+] beaconStart: picked {} ms.", period);
    beacon.start(period);
  }

  @Override
  protected void beaconStop() throws VslException {
    beaconReset();

    beacon.stop();
  }

  @Override
  protected void fragmentStart() throws VslException {
    super.fragmentStart();
    witnessStart();
  }

  @Override
  protected void fragmentStop() throws VslException {
    witnessStop();
    super.fragmentStop();
  }

  @Override
  protected void serviceGenerateQuirkyProperties(Random random) throws VslException {
    randomPeriods.clear();

    double length = (BEACON_MAX_PERIOD - BEACON_MIN_PERIOD) / fragmentCount;
    for (int i = 0; i < fragmentCount; i++) {
      int lowerLimit = (int) (BEACON_MIN_PERIOD + i * length);
      int upperLimit = (int) (lowerLimit + length);

      // Pick a random value in range.
      randomPeriods.add(random.nextInt((upperLimit - lowerLimit) + 1) + lowerLimit);
    }

    // Shuffle random values deterministically.
    Collections.shuffle(randomPeriods, random);
    LOGGER.info("[+] serviceGenerateQuirkyProperties: picked {}.", randomPeriods);
  }

  @Override
  protected void serviceReset() throws VslException {
    super.serviceReset();
    witnessReset();
  }

  private void witnessReset() throws VslException {
    witnessIsOn.setValue(false);
    witnessIntensityValue.setValue(0);
    witnessIntensitySamplingRateValue.setValue(0);
  }

  private void witnessStart() throws VslException {
    witnessIsOn.setValue(true);
    witnessIntensitySamplingRateValue.setValue(utilPeriodToFrequency(WITNESS_PERIOD));

    try {
      witness.start(
          WITNESS_PERIOD,
          new ArduinoListener() {
            @Override
            public void accept(Integer value) {
              EXECUTORS.submit(
                  () -> {
                    try {
                      witnessIntensityValue.setValue(value);

                    } catch (VslException e) {
                      e.printStackTrace();
                    }
                  });
            }
          });

    } catch (ArduinoException e) {
      e.printStackTrace();
    }
  }

  private void witnessStop() throws VslException {
    witnessReset();

    try {
      witness.stop();

    } catch (ArduinoException e) {
      e.printStackTrace();
    }
  }
}
