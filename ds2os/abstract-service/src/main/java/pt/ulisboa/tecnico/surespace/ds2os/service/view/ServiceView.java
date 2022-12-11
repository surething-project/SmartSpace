/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.view;

import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;

import java.util.Objects;

public abstract class ServiceView {
  protected final RegularNode service;

  public ServiceView(RegularNode node) {
    service = new RegularNode(node);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ServiceView)) return false;
    ServiceView that = (ServiceView) o;
    return service.equals(that.service);
  }

  protected final String getAddress() {
    return service.getAddress();
  }

  @Override
  public int hashCode() {
    return Objects.hash(service);
  }

  @Override
  public String toString() {
    return "ServiceView{" + "service=" + service + '}';
  }
}
