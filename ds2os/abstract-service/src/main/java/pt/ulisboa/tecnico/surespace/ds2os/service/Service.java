/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.connector.ServiceConnector;
import org.ds2os.vsl.core.impl.ServiceManifest;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.AbstractNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.view.TypeSearchServiceView;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.Scanner;

import static pt.ulisboa.tecnico.surespace.ds2os.service.domain.AbstractNode.composeAddress;

public abstract class Service implements AutoCloseable {
  protected static final Logger LOGGER = LoggerFactory.getLogger(Service.class);
  private static final String KEYSTORE_KEY = "K3yst0r3";
  protected final RegularNode agentRoot;
  protected final ServiceConnector connector;
  protected final RegularNode knowledgeRoot;
  protected final VslNodeFactory nodeFactory;
  protected final TypeSearchServiceView searchService;

  public Service(ServiceInitializer init)
      throws CertificateException, NoSuchAlgorithmException, KeyStoreException, VslException,
          IOException {
    if (init.getAgentUrl() == null) throw new IllegalArgumentException("Provided a null agent URL");

    if (init.getKeyStore() == null)
      throw new IllegalArgumentException("Provided a null keystore path");

    if (init.getServiceModelId() == null)
      throw new IllegalArgumentException("Provided a null service model ID");

    try {
      // Try to activate the connector.
      this.connector = new ServiceConnector(init.getAgentUrl(), init.getKeyStore(), KEYSTORE_KEY);
      connector.activate();
      nodeFactory = connector.getNodeFactory();

      // Only the service model ID matters here.
      ServiceManifest manifest = new ServiceManifest(init.getServiceModelId(), null, null);
      knowledgeRoot = new RegularNode(connector, connector.registerService(manifest));
      agentRoot = knowledgeRoot.parent();

      RegularNode searchNode = agentRoot.child("/typeSearch");
      searchService = new TypeSearchServiceView(searchNode);
      LOGGER.info("[+] Registered service @'{}'.", knowledgeRoot);

    } catch (IOException
        | CertificateException
        | NoSuchAlgorithmException
        | VslException
        | KeyStoreException e) {

      e.printStackTrace();
      close();
      throw e;
    }
  }

  public Service awaitForTermination() {
    System.out.println("[*] Press any key to exit.");
    new Scanner(System.in).nextLine();

    return this;
  }

  @Override
  public void close() {
    LOGGER.info("[*] Shutting down...");

    try {
      connector.unregisterService();

    } catch (VslException e) {
      e.printStackTrace();
    }

    connector.shutdown();
    LOGGER.info("[+] Shut down.");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Service)) return false;
    Service service = (Service) o;
    return knowledgeRoot.equals(service.knowledgeRoot);
  }

  protected final String getAddress() {
    return knowledgeRoot.getAddress();
  }

  public final RegularNode getKnowledgeRoot() {
    return knowledgeRoot;
  }

  protected final AbstractNode<?> getNode(String... relativeAddress) {
    return knowledgeRoot.child(composeAddress(relativeAddress));
  }

  @Override
  public int hashCode() {
    return Objects.hash(knowledgeRoot);
  }
}
