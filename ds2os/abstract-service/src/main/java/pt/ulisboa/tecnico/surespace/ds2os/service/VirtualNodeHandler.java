/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.AbstractNode.NodeValue;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.VirtualNode;

public interface VirtualNodeHandler {
  default NodeValue get(VirtualNode virtualNode, VslAddressParameters params, VslIdentity identity)
      throws VslException {
    return virtualNode.getValue();
  }

  default void set(VirtualNode virtualNode, NodeValue value, VslIdentity identity)
      throws VslException {
    virtualNode.setValue(value);
  }
}
