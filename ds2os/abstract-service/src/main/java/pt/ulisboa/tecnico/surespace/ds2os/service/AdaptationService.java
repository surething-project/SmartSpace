/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.AbstractNode.NodeValue;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.VirtualNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AdaptationService extends Service {
  protected static final ExecutorService EXECUTORS = Executors.newCachedThreadPool();
  protected final RegularNode beaconIsOn;
  private final RegularNode fragmentCountNode;
  private final RegularNode fragmentCurrentNode;
  private final RegularNode fragmentLengthNode;
  private final VirtualNode isLocked;
  private final VirtualNode isStarted;
  private final RegularNode seedNode;
  protected int fragmentCount;
  protected int fragmentCurrent;
  protected int fragmentLength;
  private long seed;

  public AdaptationService(ServiceInitializer init)
      throws VslException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
          IOException {
    super(init);

    seedNode = (RegularNode) getNode(ConstantPool.AdaptationService.SEED);

    fragmentCountNode = (RegularNode) getNode(ConstantPool.AdaptationService.FRAGMENT_COUNT);
    fragmentCurrentNode = (RegularNode) getNode(ConstantPool.AdaptationService.FRAGMENT_CURRENT);
    fragmentLengthNode = (RegularNode) getNode(ConstantPool.AdaptationService.FRAGMENT_LENGTH);

    final RegularNode beaconNode = (RegularNode) getNode(ConstantPool.AdaptationService.BEACON);
    beaconIsOn = beaconNode.child(ConstantPool.AdaptationService.Beacon.IS_ON);

    isLocked = new VirtualNode(getNode(ConstantPool.AdaptationService.IS_LOCKED));
    isStarted = new VirtualNode(getNode(ConstantPool.AdaptationService.IS_STARTED));
  }

  protected abstract void beaconReset() throws VslException;

  protected abstract void beaconStart() throws VslException;

  protected abstract void beaconStop() throws VslException;

  @Override
  public final void close() {
    new Thread(this::serviceStop).start();
    subscriptionsUnregister();

    super.close();
  }

  @OverridingMethodsMustInvokeSuper
  protected void fragmentStart() throws VslException {
    beaconStart();
  }

  @OverridingMethodsMustInvokeSuper
  protected void fragmentStop() throws VslException {
    beaconStop();
  }

  private void handleLock() throws VslException {
    if (isLocked.getValue().asBoolean()) {
      LOGGER.error("[-] handleLock: {} is already locked.", getAddress());
      return;
    }

    isLocked.setValue(true);
    LOGGER.info("[+] handleLock: {} has been successfully locked.", getAddress());
    serviceLock();
  }

  protected final void handleStart() throws VslException {
    if (!isLocked.getValue().asBoolean()) {
      LOGGER.error("[-] handleStart: {} is not locked.", getAddress());
      return;
    }

    if (isStarted.getValue().asBoolean()) {
      LOGGER.error("[-] handleStart: {} has started already.", getAddress());
      return;
    }

    isStarted.setValue(true);
    new Thread(this::serviceStart).start();
    LOGGER.info("[+] handleStart: {} is starting.", getAddress());
  }

  protected final void handleStop() throws VslException {
    if (!isLocked.getValue().asBoolean()) {
      LOGGER.error("[-] handleStop: {} is not locked.", getAddress());
      return;
    }

    if (!isStarted.getValue().asBoolean()) {
      LOGGER.error("[-] handleStop: {} is not started.", getAddress());
      return;
    }

    isStarted.setValue(false);
    new Thread(this::serviceStop).start();
    LOGGER.info("[+] handleStop: {} is stopping.", getAddress());
  }

  protected final void handleUnlock() throws VslException {
    if (!isLocked.getValue().asBoolean()) {
      LOGGER.error("[-] handleUnlock: {} is not locked.", getAddress());
      return;
    }

    if (isStarted.getValue().asBoolean()) {
      LOGGER.error("[-] handleUnlock: {} is not stopped.", getAddress());
      return;
    }

    isLocked.setValue(false);
    LOGGER.info("[+] handleUnlock: {} has been successfully unlocked.", getAddress());
    serviceUnlock();
  }

  protected final void serviceBootstrap() throws VslException {
    getKnowledgeRoot()
        .lock(
            (LockHandler)
                address -> {
                  isLocked.setValue(false);
                  isStarted.setValue(false);
                });

    serviceReset();
    LOGGER.info("[+] Service was reset.");

    subscriptionsRegister();
    LOGGER.info("[+] Subscriptions registered.");
  }

  private void serviceBroadcast() throws VslException {
    LOGGER.info("[+] serviceBroadcast: {} fragment(s).", fragmentCount);
    for (int i = 1; i <= fragmentCount; i++) {
      try {
        fragmentCurrent = i;
        serviceStartFragment();

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  protected abstract void serviceGenerateQuirkyProperties(Random random) throws VslException;

  @OverridingMethodsMustInvokeSuper
  protected void serviceLock() throws VslException {}

  @OverridingMethodsMustInvokeSuper
  protected void servicePrepare() throws VslException {
    seed = seedNode.getValue().asLong();
    fragmentCount = fragmentCountNode.getValue().asInteger();
    fragmentLength = fragmentLengthNode.getValue().asInteger();
  }

  @OverridingMethodsMustInvokeSuper
  protected void serviceReset() throws VslException {
    seedNode.setValue(seed = -1);

    fragmentCountNode.setValue(fragmentCount = -1);
    fragmentCurrentNode.setValue(fragmentCurrent = -1);
    fragmentLengthNode.setValue(fragmentLength = -1);

    beaconReset();
  }

  private void serviceStart() {
    try {
      servicePrepare();
      serviceGenerateQuirkyProperties(new Random(seed));
      serviceBroadcast();

    } catch (VslException e) {
      e.printStackTrace();
    }
  }

  private void serviceStartFragment() throws VslException, InterruptedException {
    LOGGER.info("[+] serviceStartFragment: {}, fragment {}.", getAddress(), fragmentCurrent);

    // Update current fragment and start it.
    fragmentCurrentNode.setValue(fragmentCurrent);
    try {
      fragmentStart();

    } catch (Exception e) {
      e.printStackTrace();
    }

    // Wait for the right time.
    TimeUnit.MILLISECONDS.sleep(fragmentLength);
    try {
      fragmentStop();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @OverridingMethodsMustInvokeSuper
  protected void serviceStop() {}

  @OverridingMethodsMustInvokeSuper
  protected void serviceUnlock() throws VslException {
    serviceReset();
  }

  @OverridingMethodsMustInvokeSuper
  protected void subscriptionsRegister() throws VslException {
    isLocked.register(
        new VirtualNodeHandler() {
          @Override
          public void set(VirtualNode virtualNode, NodeValue value, VslIdentity identity)
              throws VslException {
            if (value.asBoolean()) handleLock();
            else handleUnlock();
          }
        });

    isStarted.register(
        new VirtualNodeHandler() {
          @Override
          public void set(VirtualNode virtualNode, NodeValue value, VslIdentity identity)
              throws VslException {
            if (value.asBoolean()) handleStart();
            else handleStop();
          }
        });
  }

  @OverridingMethodsMustInvokeSuper
  protected void subscriptionsUnregister() {
    try {
      isLocked.unregister();

    } catch (VslException e) {
      e.printStackTrace();
    }

    try {
      isStarted.unregister();

    } catch (VslException e) {
      e.printStackTrace();
    }
  }

  protected final double utilFrequencyToPeriod(long frequency) {
    return (1.0 / frequency) * 1000.0;
  }

  protected final double utilPeriodToFrequency(long period) {
    return 1.0 / (period / 1000.0);
  }
}
