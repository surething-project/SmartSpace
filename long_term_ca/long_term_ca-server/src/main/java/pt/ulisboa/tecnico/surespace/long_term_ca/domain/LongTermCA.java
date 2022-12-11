/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.domain;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import pt.ulisboa.tecnico.surespace.common.connection.ServerInitializer;
import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.manager.EntityManager;
import pt.ulisboa.tecnico.surespace.common.manager.NonceManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;
import pt.ulisboa.tecnico.surespace.common.message.MessageValidator;
import pt.ulisboa.tecnico.surespace.common.message.SignedMessageValidator;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageValidatorException;
import pt.ulisboa.tecnico.surespace.long_term_ca.LongTermCAServiceImpl;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityRequest;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityRequest.RegisterEntityRequest;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityResponse;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityResponse.RegisterEntityResponse;
import pt.ulisboa.tecnico.surespace.long_term_ca.domain.exception.LongTermCAException;
import pt.ulisboa.tecnico.surespace.long_term_ca.manager.LongTermCAKeyStoreManager;
import pt.ulisboa.tecnico.surespace.long_term_ca.manager.LongTermCALogManager;
import pt.ulisboa.tecnico.surespace.long_term_ca.manager.LongTermCAManager;
import pt.ulisboa.tecnico.surespace.long_term_ca.manager.LongTermCAPropertyManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;

public final class LongTermCA implements AutoCloseable {
  private final LongTermCAManager manager;
  private final Server server;

  public LongTermCA(ServerInitializer init)
      throws PropertyManagerException, KeyStoreManagerException, EntityManagerException,
          LogManagerException, LongTermCAException {
    manager = new LongTermCAManager();

    if (init.missingHost()) init.setHost(managerProperty().get("ltca", "host").asString());

    if (init.missingPort()) init.setPort(managerProperty().get("ltca", "port").asInt());

    InetSocketAddress address = new InetSocketAddress(init.getHost(), init.getPort());
    LongTermCAServiceImpl service = new LongTermCAServiceImpl(this);
    server = NettyServerBuilder.forAddress(address).addService(service).build();
    start(init);
  }

  @Override
  public void close() {
    server.shutdownNow();
    manager.log().info("[+] Server has been shut down.");
  }

  public EntityManager managerEntity() {
    return manager.entity();
  }

  public LongTermCAKeyStoreManager managerKeyStore() {
    return manager.keyStore();
  }

  public LongTermCALogManager managerLog() {
    return manager.log();
  }

  public NonceManager managerNonce() {
    return manager.nonce();
  }

  public LongTermCAPropertyManager managerProperty() {
    return manager.property();
  }

  public SignedRegisterEntityResponse registerEntity(SignedRegisterEntityRequest signedRequest)
      throws IOException, KeyStoreManagerException, EntityManagerException,
          MessageValidatorException {
    final Entity ltca = managerEntity().current();
    final RegisterEntityRequest request = signedRequest.getMessage();

    // Validate the message.
    new MessageValidator(manager)
        .init(request)
        .assertReceiver(ltca)
        .assertSenderUnknown()
        .assertNonceValid()
        .validate();

    final Entity entity = request.getSender();
    PKCS10CertificationRequest csr = new PKCS10CertificationRequest(request.getCsr());

    if (managerKeyStore().containsCertificate(entity))
      throw new KeyStoreManagerException("Entity '" + entity.getPath() + "' already exists");

    Certificate[] chain;
    switch (entity.getType()) {
      case "oca":
        chain = managerKeyStore().registerOrchestrator(entity, csr);
        break;

      case "vca":
        chain = managerKeyStore().registerVerifier(entity, csr);
        break;

      case "ltca":
        chain = managerKeyStore().registerProver(entity, csr);
        break;

      default:
        throw new KeyStoreManagerException("Cannot handle entity type '%s'", entity.getType());
    }

    try {
      // By now, the entity is already registered, so we must verify the signature.
      new SignedMessageValidator(manager).init(signedRequest).assertSignature(chain[0]).validate();

    } catch (Exception e) {
      // Rollback.
      managerKeyStore().removeCertificateEntry(entity);
    }

    RegisterEntityResponse response =
        RegisterEntityResponse.newBuilder()
            .setSender(ltca)
            .setReceiver(entity)
            .setNonce(managerNonce())
            .setCertificateBytes(managerKeyStore())
            .setCertificateBytesChain(managerKeyStore().bytesFromCertificateChain(chain))
            .build();

    return SignedRegisterEntityResponse.newBuilder()
        .setMessage(response)
        .setSignature(managerKeyStore())
        .build();
  }

  private void start(ServerInitializer init) throws LongTermCAException {
    try {
      server.start();
      managerLog().info("[+] Started server at %s:%d.", init.getHost(), init.getPort());

    } catch (IOException e) {
      e.printStackTrace();
      throw new LongTermCAException(e.getMessage());
    }
  }
}
