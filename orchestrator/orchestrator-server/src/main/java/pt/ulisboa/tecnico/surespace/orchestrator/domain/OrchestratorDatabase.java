/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.domain;

import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationResponse;
import pt.ulisboa.tecnico.surespace.common.signal.Signal;
import pt.ulisboa.tecnico.surespace.ds2os.service.view.AdaptationServiceView;

import java.util.*;

public final class OrchestratorDatabase {
  private final HashMap<String, DatabaseEntry> entries = new HashMap<>();

  public synchronized void addEntry(String identifier, DatabaseEntry entry) {
    entries.putIfAbsent(identifier, entry.clone());
  }

  public synchronized DatabaseEntry getEntry(String identifier) {
    return entries.get(identifier);
  }

  public synchronized boolean hasEntry(String identifier) {
    return entries.containsKey(identifier);
  }

  public synchronized HashSet<String> listEntries() {
    return new HashSet<>(entries.keySet());
  }

  public synchronized void removeEntry(String identifier) {
    entries.remove(identifier);
  }

  public static final class DatabaseEntry extends Object<DatabaseEntry> {
    private static final long serialVersionUID = -4881617325130994849L;
    private final LinkedHashSet<Signal> signals = new LinkedHashSet<>();
    private LinkedHashSet<AdaptationServiceView> services;
    private SignedRequestAuthorizationResponse signedAuth;

    public void addSignals(Signal... signals) {
      this.signals.addAll(Arrays.asList(signals));
    }

    @Override
    public DatabaseEntry clone() {
      return this;
    }

    @Override
    public boolean equals(java.lang.Object o) {
      if (this == o) return true;
      if (!(o instanceof DatabaseEntry)) return false;
      DatabaseEntry that = (DatabaseEntry) o;
      return services.equals(that.services)
          && signals.equals(that.signals)
          && signedAuth.equals(that.signedAuth);
    }

    public LinkedHashSet<AdaptationServiceView> getServices() {
      return new LinkedHashSet<>(services);
    }

    public void setServices(LinkedHashSet<AdaptationServiceView> services) {
      this.services = services;
    }

    public LinkedHashSet<Signal> getSignals() {
      return new LinkedHashSet<>(signals);
    }

    public SignedRequestAuthorizationResponse getSignedAuth() {
      return signedAuth.clone();
    }

    public void setSignedAuth(SignedRequestAuthorizationResponse signedAuth) {
      this.signedAuth = signedAuth.clone();
    }

    @Override
    public int hashCode() {
      return Objects.hash(services, signals, signedAuth);
    }

    @Override
    public String toString() {
      return "DatabaseEntry{"
          + "services="
          + services
          + ", signals="
          + signals
          + ", signedAuth="
          + signedAuth
          + '}';
    }
  }
}
