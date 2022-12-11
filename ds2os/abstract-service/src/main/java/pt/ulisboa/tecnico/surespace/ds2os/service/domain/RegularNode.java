/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.domain;

import org.ds2os.vsl.connector.ServiceConnector;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.exception.InterruptedOperationException;
import org.ds2os.vsl.exception.VslException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class RegularNode extends AbstractNode<RegularNode>
    implements Comparable<RegularNode> {
  private static final int MAX_RETRIES = 2;
  private static final int TIMEOUT = 1000;
  private static final long serialVersionUID = -7633271515279626593L;

  public RegularNode(AbstractNode<?> node) {
    super(node);
  }

  public RegularNode(ServiceConnector connector, String absoluteAddress) {
    super(connector, absoluteAddress);
  }

  @Override
  public final int compareTo(@NotNull RegularNode o) {
    return this.address.compareTo(o.address);
  }

  @Override
  protected RegularNode generateNode(ServiceConnector connector, String absoluteAddress) {
    return new RegularNode(connector, absoluteAddress);
  }

  @Override
  protected VslNode get() throws VslException {
    return get(MAX_RETRIES);
  }

  private VslNode get(int retries) throws VslException {
    try {
      return CompletableFuture.supplyAsync(
              () -> {
                try {
                  return connector.get(address);

                } catch (VslException e) {
                  throw new CompletionException(e);
                }
              })
          .get(TIMEOUT, MILLISECONDS);

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      if (retries > 0) return get(retries - 1);
      else {
        if (e.getCause() instanceof VslException) throw (VslException) e.getCause();
        else throw new InterruptedOperationException(e.getCause().getMessage());
      }
    }
  }

  @Override
  public void setValue(String value) throws VslException {
    connector.set(address, createLeaf(value));
  }
}
