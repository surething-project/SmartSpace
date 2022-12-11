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

public final class LightAdaptationServiceApp {
  public static void main(String[] args)
      throws NoSuchAlgorithmException, CertificateException, VslException, KeyStoreException,
          IOException, ArduinoException {
    if (args.length != 2) {
      System.out.println("Usage: java -jar service <agentUrl> <keyStorePath>");
      System.exit(0);
    }

    new LightAdaptationService(new ServiceInitializer().setAgentUrl(args[0]).setKeyStore(args[1]))
        .awaitForTermination()
        .close();
  }
}
