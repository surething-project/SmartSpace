/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.connection;

import com.google.common.net.InetAddresses;

import java.util.Objects;

public final class ServerInitializer {
  private static final String DEFAULT_HOST = "";
  private static final int DEFAULT_ID = -1;
  private static final int DEFAULT_PORT = -1;
  private String host;
  private int id;
  private int port;

  public ServerInitializer() {
    this(DEFAULT_HOST, DEFAULT_PORT);
  }

  public ServerInitializer(String host, int port) {
    this.host = host;
    this.port = port;
    this.id = DEFAULT_ID;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ServerInitializer)) return false;
    ServerInitializer that = (ServerInitializer) o;
    return id == that.id && port == that.port && host.equals(that.host);
  }

  public String getHost() {
    return host;
  }

  public ServerInitializer setHost(String host) {
    try {
      //noinspection UnstableApiUsage
      InetAddresses.forString(host);

    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid host '" + host + "'");
    }

    this.host = host;
    return this;
  }

  public int getId() {
    return id;
  }

  public ServerInitializer setId(int id) {
    if (id <= 0) throw new IllegalArgumentException("Invalid ID '" + id + "': must be positive");

    this.id = id;
    return this;
  }

  public int getPort() {
    return port;
  }

  public ServerInitializer setPort(int port) {
    if (port < 0 || port > 0xFFFF)
      throw new IllegalArgumentException("Invalid port '" + port + "': out of range [0, 65535]");

    this.port = port;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, host, port);
  }

  public boolean missingHost() {
    return host.equals(DEFAULT_HOST);
  }

  public boolean missingID() {
    return id == DEFAULT_ID;
  }

  public boolean missingPort() {
    return port == DEFAULT_PORT;
  }

  @Override
  public String toString() {
    return "ServerInitializer{" + "host='" + host + '\'' + ", id=" + id + ", port=" + port + '}';
  }
}
