/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.arduino.device.SoundBeacon;
import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;
import pt.ulisboa.tecnico.surespace.common.async.AsyncListener;
import pt.ulisboa.tecnico.surespace.common.exception.BroadException;
import pt.ulisboa.tecnico.surespace.ds2os.service.audio.AudioProcessing;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Random;

import static pt.ulisboa.tecnico.surespace.arduino.device.SoundBeacon.VOLUME_MED;
import static pt.ulisboa.tecnico.surespace.arduino.device.SoundBeacon.VOLUME_MIN;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.Property.VALUE;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SmartDevice.IS_ON;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SoundAdaptationService.WITNESS;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SoundAdaptationService.Witness.AMPLITUDE;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SoundAdaptationService.Witness.AMPLITUDE_SAMPLING_RATE;

public final class SoundAdaptationService extends AdaptationService {
  private static final int BEACON_MAX_SONG_ID = 20;
  private static final int BEACON_MIN_SONG_ID = 1;
  private static final int BEACON_PORT = 8;
  private static final int WITNESS_PERIOD = 30;
  private final SoundBeacon beacon;
  private final AudioProcessing witness;
  private final RegularNode witnessAmplitudeSamplingRateValue;
  private final RegularNode witnessAmplitudeValue;
  private final RegularNode witnessIsOn;
  private int songId;

  public SoundAdaptationService(ServiceInitializer init)
      throws VslException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
          IOException, ArduinoException {
    super(init.setServiceModelId("services/soundadaptationservice"));

    final RegularNode witnessNode = (RegularNode) getNode(WITNESS);
    witnessIsOn = witnessNode.child(IS_ON);

    final RegularNode witnessAmplitude = witnessNode.child(AMPLITUDE);
    witnessAmplitudeValue = witnessAmplitude.child(VALUE);

    final RegularNode witnessAmplitudeSamplingRate = witnessNode.child(AMPLITUDE_SAMPLING_RATE);
    witnessAmplitudeSamplingRateValue = witnessAmplitudeSamplingRate.child(VALUE);

    // Connect to Arduino Uno board.
    beacon = new SoundBeacon(BEACON_PORT);
    witness = new AudioProcessing(); // We are using a "virtual" witness.

    serviceBootstrap();
  }

  @Override
  protected void beaconReset() throws VslException {
    beaconIsOn.setValue(false);
  }

  @Override
  protected void beaconStart() throws VslException {
    if (fragmentCurrent == 1) {
      beaconIsOn.setValue(true);

      beacon.setVolume(VOLUME_MED);
      beacon.playSong(songId);
    }
  }

  @Override
  protected void beaconStop() throws VslException {
    if (fragmentCurrent == fragmentCount) {
      beaconReset();

      beacon.setVolume(VOLUME_MIN);
      beacon.stop();
    }
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
    songId = random.nextInt(BEACON_MAX_SONG_ID - BEACON_MIN_SONG_ID + 1) + BEACON_MIN_SONG_ID;

    LOGGER.info("[+] serviceGenerateQuirkyProperties: picked {}.", songId);
  }

  @Override
  protected void serviceReset() throws VslException {
    super.serviceReset();
    witnessReset();
  }

  private void witnessReset() throws VslException {
    witnessIsOn.setValue(false);
    witnessAmplitudeValue.setValue(0);
    witnessAmplitudeSamplingRateValue.setValue(0);
  }

  private void witnessStart() throws VslException {
    if (fragmentCurrent == 1) {
      witnessIsOn.setValue(true);
      witnessAmplitudeSamplingRateValue.setValue(utilPeriodToFrequency(WITNESS_PERIOD));

      int proofLength = fragmentCount * fragmentLength;
      try {
        witness.init(songId, proofLength, WITNESS_PERIOD);
        witness.start(new Listener());

      } catch (IOException | URISyntaxException | BroadException e) {
        e.printStackTrace();
        witness.stop();
      }
    }
  }

  private void witnessStop() throws VslException {
    if (fragmentCurrent == fragmentCount) {
      witnessReset();
      witness.stop();
    }
  }

  private class Listener implements AsyncListener<Double, BroadException> {
    @Override
    public void onComplete(Double aDouble) {
      EXECUTORS.submit(
          () -> {
            try {
              witnessAmplitudeValue.setValue(aDouble);

            } catch (VslException e) {
              e.printStackTrace();
            }
          });
    }

    @Override
    public void onError(BroadException e) {
      e.printStackTrace();
      witness.stop();
    }
  }
}
