/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.manager;

import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.surespace.common.async.AsyncListener;
import pt.ulisboa.tecnico.surespace.common.exception.BroadException;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProofProperties;
import pt.ulisboa.tecnico.surespace.ds2os.service.view.AdaptationServiceView;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class AdaptationServiceViewManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdaptationServiceViewManager.class);
  private static final int TIMEOUT = 3000;
  private final LocationProofProperties proofProperties;
  private final LinkedHashSet<AdaptationServiceView> services = new LinkedHashSet<>();

  public AdaptationServiceViewManager(LocationProofProperties proofProperties) {
    if (proofProperties == null)
      throw new IllegalArgumentException("Provided null proof properties");
    this.proofProperties = proofProperties.clone();
  }

  private CompletableFuture<Boolean> getCompletableFuture(
      AdaptationServiceView view, SERVICE_OPERATION operation) {
    switch (operation) {
      case LOCK:
        return CompletableFuture.supplyAsync(view::serviceLock);
      case UNLOCK:
        return CompletableFuture.supplyAsync(view::serviceUnlock);
      case STOP:
        return CompletableFuture.supplyAsync(view::serviceStop);
      case START:
        return CompletableFuture.supplyAsync(
            () -> {
              try {
                // Provide the service with useful information.
                view.setProperties(proofProperties);

              } catch (VslException e) {
                e.printStackTrace();
                return false;
              }

              // Try to start the service.
              return view.serviceStart();
            });
      default:
        throw new RuntimeException("Provided an invalid operation");
    }
  }

  public void serviceAdd(AdaptationServiceView service) {
    services.add(service);
  }

  public void serviceAdd(Collection<AdaptationServiceView> services) {
    this.services.addAll(services);
  }

  public boolean servicesLock() {
    if (!successfullyLockedServices()) {
      LOGGER.error("[+] servicesLock: at least one service could not be locked.");
      servicesUnlock();

      return false;
    }

    LOGGER.info("[+] servicesLock: all services have been successfully locked.");
    return true;
  }

  public boolean servicesStart(
      AsyncListener<LinkedHashSet<AdaptationServiceView>, BroadException> listener) {
    if (!successfullyStartedServices()) {
      LOGGER.error("[-] servicesStart: at least one service could not be started.");
      servicesStop();
      servicesUnlock();

      return false;
    }

    // Everything went as expected, create timer.
    LOGGER.info("[+] servicesStart: all services have been successfully started.");

    int duration = proofProperties.getFragmentCount() * proofProperties.getFragmentLength();
    new Timer()
        .schedule(
            new TimerTask() {
              @Override
              public void run() {
                LOGGER.info("[+] servicesStart: trying to stop services.");
                servicesStop();

                LOGGER.info("[+] servicesStart: trying to unlock services.");
                servicesUnlock();

                // The proof is completed and ready to be handled.
                listener.onComplete(services);
              }
            },
            duration);

    return true;
  }

  public void servicesStop() {
    if (successfullyStoppedServices())
      LOGGER.info("[+] servicesStop: all services have been successfully stopped.");
    else LOGGER.error("[+] servicesStop: at least one service could not be stopped.");
  }

  public void servicesUnlock() {
    if (!successfullyUnlockedServices())
      LOGGER.error("[+] servicesUnlock: at least one service could not be unlocked.");
    else LOGGER.info("[+] servicesUnlock: all services have been successfully unlocked.");
  }

  private boolean successfullyChangedState(SERVICE_OPERATION operation) {
    final ArrayList<CompletableFuture<Boolean>> completableFutures = new ArrayList<>();

    for (AdaptationServiceView service : services)
      completableFutures.add(
          getCompletableFuture(service, operation)
              .orTimeout(TIMEOUT, MILLISECONDS)
              .exceptionally(throwable -> false));

    return completableFutures.stream()
        .map(CompletableFuture::join)
        .reduce((aBoolean, aBoolean2) -> aBoolean && aBoolean2)
        .orElse(false);
  }

  private boolean successfullyLockedServices() {
    return successfullyChangedState(SERVICE_OPERATION.LOCK);
  }

  private boolean successfullyStartedServices() {
    return successfullyChangedState(SERVICE_OPERATION.START);
  }

  private boolean successfullyStoppedServices() {
    return successfullyChangedState(SERVICE_OPERATION.STOP);
  }

  private boolean successfullyUnlockedServices() {
    return successfullyChangedState(SERVICE_OPERATION.UNLOCK);
  }

  private enum SERVICE_OPERATION {
    LOCK,
    START,
    UNLOCK,
    STOP
  }
}
