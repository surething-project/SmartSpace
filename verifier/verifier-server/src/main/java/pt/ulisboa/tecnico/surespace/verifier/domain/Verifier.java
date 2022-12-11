/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.domain;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import pt.ulisboa.tecnico.surespace.common.connection.ServerInitializer;
import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.domain.exception.ObjectException;
import pt.ulisboa.tecnico.surespace.common.manager.EntityManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;
import pt.ulisboa.tecnico.surespace.common.message.MessageValidator;
import pt.ulisboa.tecnico.surespace.common.message.SignedMessageValidator;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageValidatorException;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProof;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProofProperties;
import pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationRequest;
import pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationRequest.RequestProofInformationRequest;
import pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationResponse;
import pt.ulisboa.tecnico.surespace.orchestrator.client.OrchestratorClientException;
import pt.ulisboa.tecnico.surespace.verifier.VerifierServiceImpl;
import pt.ulisboa.tecnico.surespace.verifier.domain.exception.VerifierException;
import pt.ulisboa.tecnico.surespace.verifier.manager.VerifierKeyStoreManager;
import pt.ulisboa.tecnico.surespace.verifier.manager.VerifierLogManager;
import pt.ulisboa.tecnico.surespace.verifier.manager.VerifierManager;
import pt.ulisboa.tecnico.surespace.verifier.manager.VerifierPropertyManager;
import pt.ulisboa.tecnico.surespace.verifier.matlab.LocationProofView;
import pt.ulisboa.tecnico.surespace.verifier.matlab.Wrapper;
import pt.ulisboa.tecnico.surespace.verifier.matlab.exception.WrapperException;
import pt.ulisboa.tecnico.surespace.verifier.message.SignedVerifyProofRequest;
import pt.ulisboa.tecnico.surespace.verifier.message.SignedVerifyProofResponse;
import pt.ulisboa.tecnico.surespace.verifier.message.SignedVerifyProofResponse.VerifyProofResponse;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class Verifier implements AutoCloseable {
  private final VerifierManager manager;
  private final Wrapper matlab;
  private final Server server;

  public Verifier(ServerInitializer init)
      throws PropertyManagerException, KeyStoreManagerException, LogManagerException,
          VerifierException, ObjectException, EntityManagerException, WrapperException {
    manager = new VerifierManager(init);

    if (init.missingHost()) init.setHost(managerProperty().get("verifier", "host").asString());

    if (init.missingPort())
      init.setPort(managerProperty().get("verifier", "port").asInt() + init.getId());

    // Make sure this Verifier is registered.
    managerKeyStore().registerVerifier();

    // Create the MATLAB wrapper.
    matlab = new Wrapper();
    managerLog().info("[+] Started MATLAB wrapper.");

    // Get port from properties.
    InetSocketAddress address = new InetSocketAddress(init.getHost(), init.getPort());
    VerifierServiceImpl service = new VerifierServiceImpl(this);
    server = NettyServerBuilder.forAddress(address).addService(service).build();
    start(init);
  }

  @Override
  public void close() {
    server.shutdownNow();
    manager.log().info("[+] Server has been shut down.");

    matlab.close();
    manager.log().info("[+] MATLAB engine has been shut down.");
  }

  private EntityManager managerEntity() {
    return manager.entity();
  }

  private VerifierKeyStoreManager managerKeyStore() {
    return manager.keyStore();
  }

  public VerifierLogManager managerLog() {
    return manager.log();
  }

  private VerifierPropertyManager managerProperty() {
    return manager.property();
  }

  public void start(ServerInitializer init) throws VerifierException {
    try {
      server.start();
      managerLog().info("[+] Started server at %s:%d.", init.getHost(), init.getPort());

    } catch (IOException e) {
      e.printStackTrace();
      throw new VerifierException(e.getMessage());
    }
  }

  public SignedVerifyProofResponse verifyProof(SignedVerifyProofRequest signedRequest)
      throws MessageValidatorException, EntityManagerException, KeyStoreManagerException,
          OrchestratorClientException {
    final Entity verifier = managerEntity().current();
    final Entity orchestrator = manager.getOrchestrator();

    // Basic validation.
    new MessageValidator(manager)
        .init(signedRequest.getMessage())
        .assertReceiver(verifier)
        .assertCertificateValid()
        .assertNonceValid()
        .validate();

    new SignedMessageValidator(manager).init(signedRequest).assertSignature().validate();

    // Get proof identifier.
    LocationProofProperties properties =
        signedRequest
            .getMessage()
            .getLocationProof()
            .getAuthorization()
            .getMessage()
            .getProperties();

    // Request proof information to the orchestrator.
    RequestProofInformationRequest orchestratorRequest =
        RequestProofInformationRequest.newBuilder()
            .setIdentifier(properties.getIdentifier())
            .setSender(verifier)
            .setReceiver(orchestrator)
            .setCertificateBytes(managerKeyStore())
            .setNonce(manager.nonce())
            .build();

    SignedRequestProofInformationRequest orchestratorSignedRequest =
        SignedRequestProofInformationRequest.newBuilder()
            .setMessage(orchestratorRequest)
            .setSignature(managerKeyStore())
            .build();

    SignedRequestProofInformationResponse orchestratorSignedResponse =
        manager.getOrchestratorClient().requestProofInformation(orchestratorSignedRequest);

    // Validate orchestrator response.
    new MessageValidator(manager)
        .init(orchestratorSignedResponse.getMessage())
        .assertSender(orchestrator)
        .assertReceiver(verifier)
        .assertCertificateValid()
        .assertNonceValid()
        .validate();

    new SignedMessageValidator(manager)
        .init(orchestratorSignedResponse)
        .assertSignature()
        .validate();

    boolean proofAccepted = false;
    try {
      // Init wrapper with current location proof.
      LocationProof locationProof = signedRequest.getMessage().getLocationProof();
      matlab.locationProofInit(
          new LocationProofView(
              locationProof,
              orchestratorSignedResponse.getMessage().getSignals(),
              locationProof.getSignals()));

      // Write signals to files.
      matlab.locationProofWriteToDirectory();

      // Check if the proof has been accepted.
      proofAccepted = matlab.locationProofVerify();

    } catch (WrapperException e) {
      e.printStackTrace();
    }

    return SignedVerifyProofResponse.newBuilder()
        .setMessage(
            VerifyProofResponse.newBuilder()
                .setSender(verifier)
                .setProofAccepted(proofAccepted)
                .setReceiver(signedRequest.getMessage().getSender())
                .setCertificateBytes(managerKeyStore())
                .setNonce(manager.nonce())
                .build())
        .setSignature(managerKeyStore())
        .build();
  }
}
