/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import java.util.Objects;

public final class ServiceInitializer {
  private String agentUrl;
  private String keyStore;
  private String serviceModelId;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ServiceInitializer)) return false;
    ServiceInitializer that = (ServiceInitializer) o;
    return agentUrl.equals(that.agentUrl)
        && keyStore.equals(that.keyStore)
        && serviceModelId.equals(that.serviceModelId);
  }

  public String getAgentUrl() {
    return agentUrl;
  }

  public ServiceInitializer setAgentUrl(String agentUrl) {
    this.agentUrl = agentUrl;
    return this;
  }

  public String getKeyStore() {
    return keyStore;
  }

  public ServiceInitializer setKeyStore(String keyStore) {
    this.keyStore = keyStore;
    return this;
  }

  public String getServiceModelId() {
    return serviceModelId;
  }

  public ServiceInitializer setServiceModelId(String serviceModelId) {
    this.serviceModelId = serviceModelId;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentUrl, keyStore, serviceModelId);
  }

  @Override
  public String toString() {
    return "ServiceInitializer{"
        + "agentUrl='"
        + agentUrl
        + '\''
        + ", keyStore='"
        + keyStore
        + '\''
        + ", serviceModelId='"
        + serviceModelId
        + '\''
        + '}';
  }
}
