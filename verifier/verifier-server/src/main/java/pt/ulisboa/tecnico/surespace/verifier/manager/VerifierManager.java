/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.manager;

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
import pt.ulisboa.tecnico.surespace.orchestrator.client.OrchestratorClient;

import java.security.Security;

public final class VerifierManager implements GlobalManagerInterface {
  private final EntityManager entityManager;
  private final VerifierKeyStoreManager keyStoreManager;
  private final VerifierLogManager logManager;
  private final NonceManager nonceManager;
  private final Entity orchestrator;
  private final OrchestratorClient orchestratorClient;
  private final VerifierPropertyManager propertyManager;

  public VerifierManager(ServerInitializer initializer)
      throws KeyStoreManagerException, PropertyManagerException, LogManagerException,
          ObjectException, EntityManagerException {
    Security.addProvider(new BouncyCastleProvider());

    this.logManager = new VerifierLogManager();
    this.propertyManager = new VerifierPropertyManager(logManager);

    if (initializer.missingID()) {
      initializer.setId(Integer.parseInt(propertyManager.get("verifier", "id").asString()));
    }

    this.entityManager = new EntityManager(propertyManager);
    String name = "Verifier " + initializer.getId();
    String path = "surespace://rca/vca/" + initializer.getId();
    entityManager.current(new Entity(name, path));

    // Get a client for Orchestrator 1.
    String orchestratorHost = propertyManager.get("orchestrator", "host").asString();
    int orchestratorPort = propertyManager.get("orchestrator", "port").asInt();
    orchestratorClient = new OrchestratorClient(orchestratorHost, orchestratorPort);

    String orchestratorPath = propertyManager.get("orchestrator", "path").asString();
    orchestrator = entityManager.getByPath(orchestratorPath);

    this.nonceManager = new NonceManager();
    this.keyStoreManager = new VerifierKeyStoreManager(this);
  }

  @Override
  public EntityManager entity() {
    return entityManager;
  }

  public Entity getOrchestrator() {
    return orchestrator.clone();
  }

  public OrchestratorClient getOrchestratorClient() {
    return orchestratorClient;
  }

  @Override
  public VerifierKeyStoreManager keyStore() {
    return keyStoreManager;
  }

  @Override
  public VerifierLogManager log() {
    return logManager;
  }

  @Override
  public NonceManager nonce() {
    return nonceManager;
  }

  @Override
  public VerifierPropertyManager property() {
    return propertyManager;
  }
}
