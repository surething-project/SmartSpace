/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.arduino.exception.ArduinoException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public final class AdaptationServiceBootstrapApp {
  private static final String AGENT_URL = "https://0.0.0.0:8081";
  private static final String LIGHT_ADAPTATION_SERVICE_JKS = "lightadaptationservice.jks";
  private static final String SOUND_ADAPTATION_SERVICE_JKS = "soundadaptationservice.jks";

  public static void main(String[] args)
      throws NoSuchAlgorithmException, CertificateException, VslException, KeyStoreException,
          IOException, ArduinoException {
    // We require the manager because all services have to share the same Arduino link.
    AdaptationServiceBootstrapManager manager = new AdaptationServiceBootstrapManager();

    manager.addService(
        new SoundAdaptationService(
            new ServiceInitializer()
                .setAgentUrl(AGENT_URL)
                .setKeyStore(SOUND_ADAPTATION_SERVICE_JKS)));

    manager.addService(
        new LightAdaptationService(
            new ServiceInitializer()
                .setAgentUrl(AGENT_URL)
                .setKeyStore(LIGHT_ADAPTATION_SERVICE_JKS)));

    System.out.printf("[+] %d service(s) were closed.%n", manager.awaitForTermination());
  }
}
