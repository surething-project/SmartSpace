/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier;

import pt.ulisboa.tecnico.surespace.common.connection.ServerInitializer;
import pt.ulisboa.tecnico.surespace.common.domain.exception.ObjectException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;
import pt.ulisboa.tecnico.surespace.verifier.domain.Verifier;
import pt.ulisboa.tecnico.surespace.verifier.domain.exception.VerifierException;
import pt.ulisboa.tecnico.surespace.verifier.matlab.exception.WrapperException;

import java.util.Scanner;

public final class VerifierApp {
  public static void main(String[] args)
      throws KeyStoreManagerException, PropertyManagerException, VerifierException,
          LogManagerException, ObjectException, EntityManagerException, WrapperException {
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
      System.err.printf("Usage: java %s <id> <host> <port>%n", VerifierApp.class.getName());
      System.exit(0);
    }

    Verifier verifier = new Verifier(initializer);
    System.out.println("> Press any key to shutdown");
    new Scanner(System.in).nextLine();
    verifier.close();
  }
}
