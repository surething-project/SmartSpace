/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.domain;

import org.ds2os.vsl.connector.ServiceConnector;
import org.ds2os.vsl.core.VslLockHandler;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.ds2os.service.SubscriberHandler;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static java.util.Arrays.copyOfRange;

@SuppressWarnings("unused")
public abstract class AbstractNode<Node extends AbstractNode<Node>> implements Serializable {
  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractNode.class);
  private static final long serialVersionUID = 3442946529991752204L;
  protected final String address;
  protected final transient ServiceConnector connector;
  private boolean subscribed = false;

  public AbstractNode(AbstractNode<?> node) {
    this(node.connector, node.address);
    subscribed = node.subscribed;
  }

  public AbstractNode(ServiceConnector connector, String absoluteAddress) {
    if (connector == null) throw new IllegalArgumentException("The connector must be non-null");

    if (absoluteAddress == null || absoluteAddress.isBlank())
      throw new IllegalArgumentException("The address must be non-null and non-empty");

    this.connector = connector;
    this.address = absoluteAddress;
  }

  public static String composeAddress(String... addressComponents) {
    return Arrays.stream(addressComponents)
        .map(AbstractNode::fixAddress)
        .collect(Collectors.joining());
  }

  public static String fixAddress(String address) {
    return address.startsWith("/") ? address : "/" + address;
  }

  public static String[] splitAddress(String address) {
    return address.split("/");
  }

  public final Node child(String relativeAddress) {
    return generateNode(connector, composeAddress(address, relativeAddress));
  }

  private void commit() throws VslException {
    connector.commitSubtree(address);
  }

  protected final VslNode createLeaf(String value) {
    return connector.getNodeFactory().createImmutableLeaf(value);
  }

  @Override
  public final boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractNode)) return false;
    AbstractNode<?> that = (AbstractNode<?>) o;
    return address.equals(that.address);
  }

  protected abstract Node generateNode(ServiceConnector connector, String absoluteAddress);

  protected abstract VslNode get() throws VslException;

  public final String getAddress() {
    return address;
  }

  public final ServiceConnector getConnector() {
    return connector;
  }

  public final InputStream getStream() throws VslException {
    return connector.getStream(address);
  }

  public final void setStream(InputStream stream) throws VslException {
    connector.setStream(address, stream);
  }

  public final NodeValue getValue() throws VslException {
    return new NodeValue(get());
  }

  public abstract void setValue(String value) throws VslException;

  public final void setValue(Object<?> value) throws VslException {
    setValue(value.asString());
  }

  public final void setValue(Boolean value) throws VslException {
    setValue(value ? "1" : "0");
  }

  public final void setValue(Integer value) throws VslException {
    setValue(String.valueOf(value));
  }

  public final void setValue(Long value) throws VslException {
    setValue(String.valueOf(value));
  }

  public final void setValue(NodeValue value) throws VslException {
    setValue(value.asString());
  }

  public final void setValue(Double value) throws VslException {
    setValue(String.valueOf(value));
  }

  @Override
  public final int hashCode() {
    return Objects.hash(address);
  }

  public final boolean isSubscribed() {
    return subscribed;
  }

  public final void lock(VslLockHandler handler) throws VslException {
    CompletableFuture<Void> future = new CompletableFuture<>();

    connector.lockSubtree(
        address,
        new VslLockHandler() {
          @Override
          public void lockAcquired(String address) {
            CompletableFuture.runAsync(
                () -> {
                  try {
                    LOGGER.debug("[+] Acquired lock.");
                    if (handler != null) handler.lockAcquired(address);

                    LOGGER.debug("[*] Trying to commit changes.");
                    commit();
                    LOGGER.debug("[+] Changes committed.");
                    future.complete(null);

                  } catch (VslException e) {
                    LOGGER.error("[-] Could not commit.");

                    try {
                      LOGGER.debug("[*] Trying to rollback.");
                      rollback();
                      LOGGER.debug("[+] Roll backed.");

                      future.completeExceptionally(e);

                    } catch (VslException e1) {
                      LOGGER.error("[-] Could not rollback.");
                      future.completeExceptionally(e1);
                    }
                  }
                });
          }

          @Override
          public void lockExpired(String address) throws VslException {
            LOGGER.error("[-] Lock expired.");
            if (handler != null) handler.lockExpired(address);
          }

          @Override
          public void lockWillExpire(String address) throws VslException {
            LOGGER.debug("[*] Lock will expire.");
            if (handler != null) handler.lockWillExpire(address);
          }
        });

    try {
      future.join();

    } catch (CompletionException e) {
      throw ((VslException) e.getCause());
    }
  }

  public final Node parent() {
    String[] splitAddress = splitAddress(address);
    if (splitAddress.length == 2) return generateNode(connector, "/");

    String[] newAddress = copyOfRange(splitAddress, 1, splitAddress.length - 1);
    return generateNode(connector, composeAddress(newAddress));
  }

  private void rollback() throws VslException {
    connector.rollbackSubtree(address);
  }

  public final void subscribe(SubscriberHandler handler) throws VslException {
    if (!subscribed) {
      LOGGER.info("[*] Subscribed to {}.", getAddress());

      connector.subscribe(address, address -> handler.nodeChanged(AbstractNode.this));
      subscribed = true;
    }
  }

  @Override
  public final String toString() {
    return address;
  }

  public final void unsubscribe() throws VslException {
    if (subscribed) {
      LOGGER.info("[*] Unsubscribed from {}.", getAddress());

      connector.unsubscribe(address);
      subscribed = false;
    }
  }

  public static final class NodeValue {
    private final VslNode value;
    private String defaultValue = null;

    public NodeValue(VslNode value) {
      this.value = value;
    }

    public Boolean asBoolean() {
      return asString().equals("1");
    }

    public Double asDouble() {
      try {
        return Double.parseDouble(asString());

      } catch (NumberFormatException e) {
        return getDefaultValue(e).asDouble();
      }
    }

    public Float asFloat() {
      try {
        return Float.parseFloat(asString());

      } catch (NumberFormatException e) {
        return getDefaultValue(e).asFloat();
      }
    }

    public Integer asInteger() {
      try {
        return Integer.parseInt(asString());

      } catch (NumberFormatException e) {
        return getDefaultValue(e).asInteger();
      }
    }

    public Long asLong() {
      try {
        return Long.parseLong(asString());

      } catch (NumberFormatException e) {
        return getDefaultValue(e).asLong();
      }
    }

    public String asString() {
      return value.getValue();
    }

    public VslNode asVslNode() {
      return value;
    }

    private NodeValue getDefaultValue(RuntimeException e) {
      if (defaultValue == null) throw e;
      return new NodeValue(value);
    }

    public NodeValue withDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }
  }
}
