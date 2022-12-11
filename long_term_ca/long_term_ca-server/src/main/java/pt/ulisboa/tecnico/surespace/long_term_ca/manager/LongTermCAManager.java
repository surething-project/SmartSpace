/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.manager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import pt.ulisboa.tecnico.surespace.common.manager.EntityManager;
import pt.ulisboa.tecnico.surespace.common.manager.GlobalManagerInterface;
import pt.ulisboa.tecnico.surespace.common.manager.NonceManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;

import java.security.Security;

public class LongTermCAManager implements GlobalManagerInterface {
  private final EntityManager entityManager;
  private final LongTermCAKeyStoreManager keyStoreManager;
  private final LongTermCALogManager logManager;
  private final NonceManager nonceManager;
  private final LongTermCAPropertyManager propertyManager;

  public LongTermCAManager()
      throws PropertyManagerException, LogManagerException, EntityManagerException,
          KeyStoreManagerException {
    Security.addProvider(new BouncyCastleProvider());

    this.logManager = new LongTermCALogManager();
    this.propertyManager = new LongTermCAPropertyManager(logManager);

    this.entityManager = new EntityManager(propertyManager);
    entityManager.current(entityManager.getByPath("surespace://rca/ltca"));

    this.nonceManager = new NonceManager();
    this.keyStoreManager = new LongTermCAKeyStoreManager(this);
  }

  @Override
  public EntityManager entity() {
    return entityManager;
  }

  @Override
  public LongTermCAKeyStoreManager keyStore() {
    return keyStoreManager;
  }

  @Override
  public LongTermCALogManager log() {
    return logManager;
  }

  @Override
  public NonceManager nonce() {
    return nonceManager;
  }

  @Override
  public LongTermCAPropertyManager property() {
    return propertyManager;
  }
}
