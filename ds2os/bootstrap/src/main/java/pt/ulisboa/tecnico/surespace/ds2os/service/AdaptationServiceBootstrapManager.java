/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Scanner;

public final class AdaptationServiceBootstrapManager {
  private final LinkedHashSet<AdaptationService> services = new LinkedHashSet<>();

  public void addService(AdaptationService service) {
    services.add(service);
  }

  public void addService(Collection<AdaptationService> services) {
    this.services.addAll(services);
  }

  public int awaitForTermination() {
    System.out.println("[*] Press any key to exit.");
    new Scanner(System.in).nextLine();

    // Close all services.
    for (AdaptationService service : services) {
      try {
        service.close();

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    int size = services.size();
    services.clear();

    return size;
  }
}
