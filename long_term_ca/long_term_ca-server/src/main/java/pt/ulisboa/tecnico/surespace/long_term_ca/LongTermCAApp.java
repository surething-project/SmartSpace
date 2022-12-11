/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca;

import pt.ulisboa.tecnico.surespace.common.connection.ServerInitializer;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;
import pt.ulisboa.tecnico.surespace.long_term_ca.domain.LongTermCA;
import pt.ulisboa.tecnico.surespace.long_term_ca.domain.exception.LongTermCAException;

import java.util.Scanner;

public final class LongTermCAApp {
  public static void main(String[] args)
      throws PropertyManagerException, LogManagerException, LongTermCAException,
          EntityManagerException, KeyStoreManagerException {
    ServerInitializer initializer = new ServerInitializer();

    if (args.length >= 1) {
      initializer.setHost(args[0]);
    }

    if (args.length == 2) {
      initializer.setPort(Integer.parseInt(args[1]));
    }

    if (args.length > 2) {
      System.err.println("Invalid argument(s).");
      System.err.printf("Usage: java %s <host> <port>%n", LongTermCAApp.class.getName());
      System.exit(0);
    }

    LongTermCA longTermCA = new LongTermCA(initializer);
    System.out.println("> Press any key to shutdown");
    new Scanner(System.in).nextLine();
    longTermCA.close();
  }
}
