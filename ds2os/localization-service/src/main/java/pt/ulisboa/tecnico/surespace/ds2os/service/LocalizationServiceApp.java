/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.common.location.LocationOLC;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public final class LocalizationServiceApp {
  public static void main(String[] args)
      throws CertificateException, NoSuchAlgorithmException, KeyStoreException, VslException,
          IOException {
    if (args.length != 3) {
      System.out.println("Usage: java -jar service <agentUrl> <keyStorePath> <locationOlc>");
      System.exit(0);
    }

    new LocalizationService(
            new ServiceInitializer().setAgentUrl(args[0]).setKeyStore(args[1]),
            new LocationOLC(args[2]))
        .awaitForTermination()
        .close();
  }
}
