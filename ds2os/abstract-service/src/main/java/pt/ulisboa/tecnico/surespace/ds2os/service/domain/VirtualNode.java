/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.domain;

import org.ds2os.vsl.connector.ServiceConnector;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidOperationException;
import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.ds2os.service.VirtualNodeHandler;

import java.io.InputStream;

public final class VirtualNode extends AbstractNode<VirtualNode> {
  private static final long serialVersionUID = 709024931835729271L;
  private NodeValue value;

  public VirtualNode(AbstractNode<?> node) {
    super(node);
  }

  public VirtualNode(ServiceConnector connector, String absoluteAddress) {
    super(connector, absoluteAddress);
  }

  @Override
  protected VirtualNode generateNode(ServiceConnector connector, String absoluteAddress) {
    return new VirtualNode(connector, absoluteAddress);
  }

  @Override
  protected VslNode get() {
    return connector.getNodeFactory().createImmutableLeaf(value.asString());
  }

  private InvalidOperationException newInvalidOperationException() {
    return new InvalidOperationException("Only get and set operations are supported");
  }

  public void register(VirtualNodeHandler handler) throws VslException {
    connector.registerVirtualNode(
        address,
        new VslVirtualNodeHandler() {
          @Override
          public VslNode get(String address, VslAddressParameters params, VslIdentity identity)
              throws VslException {
            return handler.get(VirtualNode.this, params, identity).asVslNode();
          }

          @Override
          public InputStream getStream(String address, VslIdentity identity) throws VslException {
            throw newInvalidOperationException();
          }

          @Override
          public void set(String address, VslNode value, VslIdentity identity) throws VslException {
            handler.set(VirtualNode.this, new NodeValue(value), identity);
          }

          @Override
          public void setStream(String address, InputStream stream, VslIdentity identity)
              throws VslException {
            throw newInvalidOperationException();
          }

          @Override
          public void subscribe(String address) throws VslException {
            throw newInvalidOperationException();
          }

          @Override
          public void unsubscribe(String address) throws VslException {
            throw newInvalidOperationException();
          }
        });
  }

  @Override
  public void setValue(String value) {
    this.value = new NodeValue(createLeaf(value));
  }

  public void unregister() throws VslException {
    connector.unregisterVirtualNode(address);
  }
}
