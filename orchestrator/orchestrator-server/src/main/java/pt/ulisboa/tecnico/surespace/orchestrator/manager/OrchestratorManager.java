/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.manager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import pt.ulisboa.tecnico.surespace.common.connection.ServerInitializer;
import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.domain.exception.ObjectException;
import pt.ulisboa.tecnico.surespace.common.manager.EntityManager;
import pt.ulisboa.tecnico.surespace.common.manager.GlobalManagerInterface;
import pt.ulisboa.tecnico.surespace.common.manager.NonceManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;
import pt.ulisboa.tecnico.surespace.verifier.client.VerifierClient;

import java.security.Security;

public final class OrchestratorManager implements GlobalManagerInterface {
  private final EntityManager entityManager;
  private final OrchestratorKeyStoreManager keyStoreManager;
  private final OrchestratorLogManager logManager;
  private final NonceManager nonceManager;
  private final OrchestratorPropertyManager propertyManager;
  private final Entity verifier;
  private final VerifierClient verifierClient;

  public OrchestratorManager(ServerInitializer initializer)
      throws KeyStoreManagerException, LogManagerException, PropertyManagerException,
          ObjectException, EntityManagerException {
    Security.addProvider(new BouncyCastleProvider());

    this.logManager = new OrchestratorLogManager();
    this.propertyManager = new OrchestratorPropertyManager(logManager);

    if (initializer.missingID()) {
      initializer.setId(Integer.parseInt(propertyManager.get("orchestrator", "id").asString()));
    }

    this.entityManager = new EntityManager(propertyManager);
    String name = "Orchestrator " + initializer.getId();
    String path = "surespace://rca/oca/" + initializer.getId();
    entityManager.current(new Entity(name, path));

    // Get a client for Verifier 1.
    String verifierHost = propertyManager.get("verifier", "host").asString();
    int verifierPort = propertyManager.get("verifier", "port").asInt();
    verifierClient = new VerifierClient(verifierHost, verifierPort);

    String verifierPath = propertyManager.get("verifier", "path").asString();
    verifier = entityManager.getByPath(verifierPath);

    this.nonceManager = new NonceManager();
    this.keyStoreManager = new OrchestratorKeyStoreManager(this);
  }

  @Override
  public EntityManager entity() {
    return entityManager;
  }

  public Entity getVerifier() {
    return verifier.clone();
  }

  public VerifierClient getVerifierClient() {
    return verifierClient;
  }

  @Override
  public OrchestratorKeyStoreManager keyStore() {
    return keyStoreManager;
  }

  @Override
  public OrchestratorLogManager log() {
    return logManager;
  }

  @Override
  public NonceManager nonce() {
    return nonceManager;
  }

  @Override
  public OrchestratorPropertyManager property() {
    return propertyManager;
  }
}
