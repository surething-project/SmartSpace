/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.connection;

import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

public abstract class Client<ClientException extends BroadException> implements AutoCloseable {
  protected static final long TIMEOUT = 3000;
  private final String host;
  private final int port;

  protected Client(final String host, final int port) {
    this.host = host;
    this.port = port;
  }

  @Override
  public abstract void close();

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public abstract void ping() throws ClientException;
}
