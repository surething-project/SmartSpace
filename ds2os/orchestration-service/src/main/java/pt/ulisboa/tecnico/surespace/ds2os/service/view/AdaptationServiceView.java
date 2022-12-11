/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.view;

import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.surespace.common.proof.Beacon;
import pt.ulisboa.tecnico.surespace.common.proof.Device;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProofProperties;
import pt.ulisboa.tecnico.surespace.common.signal.Fragment;
import pt.ulisboa.tecnico.surespace.common.signal.Reading;
import pt.ulisboa.tecnico.surespace.common.signal.Signal;
import pt.ulisboa.tecnico.surespace.common.signal.property.Property;
import pt.ulisboa.tecnico.surespace.ds2os.service.LockHandler;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.AbstractNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.AdaptationService;

public abstract class AdaptationServiceView extends ServiceView {
  protected static final Logger LOGGER = LoggerFactory.getLogger(AdaptationServiceView.class);
  protected final Beacon beacon;
  protected final RegularNode beaconIsOn;
  protected final RegularNode beaconNode;
  protected final RegularNode fragmentCount;
  protected final RegularNode fragmentCurrent;
  protected final RegularNode fragmentLength;
  protected final RegularNode isLocked;
  protected final RegularNode isStarted;
  protected final RegularNode seedNode;
  protected Signal signal;
  private Fragment fragment;

  public AdaptationServiceView(RegularNode node, Beacon beacon) {
    super(node);
    this.beacon = beacon;

    seedNode = service.child(AdaptationService.SEED);

    fragmentCount = service.child(AdaptationService.FRAGMENT_COUNT);
    fragmentCurrent = service.child(AdaptationService.FRAGMENT_CURRENT);
    fragmentLength = service.child(AdaptationService.FRAGMENT_LENGTH);

    beaconNode = service.child(AdaptationService.BEACON);
    beaconIsOn = beaconNode.child(AdaptationService.Beacon.IS_ON);

    isLocked = service.child(AdaptationService.IS_LOCKED);
    isStarted = service.child(AdaptationService.IS_STARTED);
  }

  public abstract String getDescriptor();

  public final Signal getSignal() {
    return signal.clone();
  }

  protected void handleFragmentCurrentValueChange(AbstractNode<?> node) throws VslException {
    LOGGER.info("[+] handleFragmentCurrentValueChange: new fragment detected.");
    serviceCreateFragment(node.getValue().asInteger());
  }

  protected void handleLock(String address) throws VslException {
    isLocked.setValue(true);
    LOGGER.info("[+] serviceLock: {} has been successfully locked.", getAddress());
  }

  protected void handleUnlock() {}

  protected void serviceAddReading(Device device, Property property, long time, String value) {
    fragment.addReading(new Reading(device, property, time, value));
  }

  protected void serviceBeforeStart() throws VslException {
    // Clear previous signal and fragment.
    signal = null;
    fragment = null;

    fragmentCurrent.subscribe(this::handleFragmentCurrentValueChange);
  }

  protected void serviceBeforeStop() throws VslException {
    fragmentCurrent.unsubscribe();

    // Add last fragment.
    if (fragment != null) signal.addFragment(fragment);
  }

  private void serviceCreateFragment(int id) {
    if (id > 1 && fragment != null) {
      signal.addFragment(fragment);
    }

    fragment = new Fragment(id);
  }

  private void serviceCreateSignal() {
    signal = new Signal(beacon);
  }

  public final boolean serviceLock() {
    try {
      service.lock((LockHandler) this::handleLock);
      return true;

    } catch (VslException e) {
      LOGGER.error("[-] serviceLock: {} could not be locked: {}.", getAddress(), e.getMessage());
      e.printStackTrace();

      return false;
    }
  }

  public final boolean serviceStart() {
    try {
      serviceBeforeStart();
      serviceCreateSignal();

      isStarted.setValue(true);
      LOGGER.info("[+] serviceStart: {} has been successfully started.", getAddress());

      return true;

    } catch (VslException e) {
      LOGGER.error("[-] serviceStart: {} could not be started: {}.", getAddress(), e.getMessage());
      e.printStackTrace();

      return false;
    }
  }

  public final boolean serviceStop() {
    try {
      serviceBeforeStop();
      isStarted.setValue(false);
      LOGGER.info("[+] serviceStop: {} has been successfully stopped.", getAddress());

      return true;

    } catch (VslException e) {
      LOGGER.error("[-] serviceStop: {} could not be stopped: {}.", getAddress(), e.getMessage());
      e.printStackTrace();

      return false;
    }
  }

  public final boolean serviceUnlock() {
    try {
      isLocked.setValue(false);
      handleUnlock();
      LOGGER.info("[+] serviceUnlock: {} has been unlocked successfully.", getAddress());

      return true;

    } catch (VslException e) {
      LOGGER.info("[+] serviceUnlock: {} could not be unlocked: {}.", getAddress(), e.getMessage());
      e.printStackTrace();

      return false;
    }
  }

  public final void setProperties(LocationProofProperties properties) throws VslException {
    seedNode.setValue(properties.getSeed());
    fragmentCount.setValue(properties.getFragmentCount());
    fragmentLength.setValue(properties.getFragmentLength());
  }
}
