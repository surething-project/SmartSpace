/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.view;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class TypeSearchServiceView extends ServiceView {
  private static final String SPLITTER = "//";

  public TypeSearchServiceView(RegularNode node) {
    super(node);
  }

  private RegularNode createRegularNode(String address) {
    return new RegularNode(service.getConnector(), address);
  }

  private boolean filterNodeAddress(String s) {
    return !s.isEmpty();
  }

  public Set<RegularNode> searchByType(String type) throws VslException {
    String[] types = service.child(type).getValue().withDefaultValue("").asString().split(SPLITTER);
    if (types.length == 0) return new HashSet<>();

    return Arrays.stream(types)
        .filter(this::filterNodeAddress)
        .map(this::createRegularNode)
        .collect(Collectors.toUnmodifiableSet());
  }
}
