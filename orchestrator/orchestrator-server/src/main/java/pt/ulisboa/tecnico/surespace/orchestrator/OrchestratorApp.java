/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.common.connection.ServerInitializer;
import pt.ulisboa.tecnico.surespace.common.exception.BroadException;
import pt.ulisboa.tecnico.surespace.orchestrator.domain.Orchestrator;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class OrchestratorApp {
  public static void main(String[] args)
      throws BroadException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
          VslException, IOException {
    ServerInitializer initializer = new ServerInitializer();

    if (args.length >= 1) {
      // ID.
      initializer.setId(Math.max(1, Integer.parseInt(args[0])));
    }

    if (args.length >= 2) {
      // ID, and host.
      initializer.setHost(args[1]);
    }

    if (args.length >= 3) {
      // ID, host, and port.
      initializer.setPort(Integer.parseInt(args[2]));
    }

    if (args.length > 3) {
      System.err.println("Invalid argument(s).");
      System.err.printf("Usage: java %s <id> <host> <port>%n", OrchestratorApp.class.getName());
      System.exit(0);
    }

    Orchestrator orchestrator = new Orchestrator(initializer);
    orchestrator.openMenu();
    orchestrator.close();
  }
}
