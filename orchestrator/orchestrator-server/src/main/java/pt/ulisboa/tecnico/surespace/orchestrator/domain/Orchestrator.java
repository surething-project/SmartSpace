/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.domain;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.common.async.AsyncListener;
import pt.ulisboa.tecnico.surespace.common.connection.ServerInitializer;
import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.domain.TimeInterval;
import pt.ulisboa.tecnico.surespace.common.domain.exception.ObjectException;
import pt.ulisboa.tecnico.surespace.common.exception.BroadException;
import pt.ulisboa.tecnico.surespace.common.location.exception.LocationException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;
import pt.ulisboa.tecnico.surespace.common.message.*;
import pt.ulisboa.tecnico.surespace.common.message.SignedProveLocationRequest.ProveLocationRequest;
import pt.ulisboa.tecnico.surespace.common.message.SignedProveLocationResponse.ProveLocationResponse;
import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationRequest.RequestAuthorizationRequest;
import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationResponse.RequestAuthorizationResponse;
import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationResponse.RequestAuthorizationResponse.RequestAuthorizationResponseBuilder;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageValidatorException;
import pt.ulisboa.tecnico.surespace.common.proof.Beacon;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProofProperties;
import pt.ulisboa.tecnico.surespace.common.signal.Signal;
import pt.ulisboa.tecnico.surespace.ds2os.service.OrchestrationService;
import pt.ulisboa.tecnico.surespace.ds2os.service.ServiceInitializer;
import pt.ulisboa.tecnico.surespace.ds2os.service.exception.OrchestrationServiceException;
import pt.ulisboa.tecnico.surespace.ds2os.service.view.AdaptationServiceView;
import pt.ulisboa.tecnico.surespace.orchestrator.OrchestratorServiceImpl;
import pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationRequest;
import pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationResponse;
import pt.ulisboa.tecnico.surespace.orchestrator.domain.OrchestratorDatabase.DatabaseEntry;
import pt.ulisboa.tecnico.surespace.orchestrator.domain.exception.OrchestratorException;
import pt.ulisboa.tecnico.surespace.orchestrator.manager.OrchestratorManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MINUTES;
import static pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationResponse.RequestProofInformationResponse;
import static pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationResponse.newBuilder;

public final class Orchestrator implements AutoCloseable {
  private static final Scanner SCANNER = new Scanner(System.in);
  public final OrchestratorManager manager;
  private final OrchestratorDatabase database;
  private final OrchestrationService orchestrationService;
  private final Server server;

  public Orchestrator(ServerInitializer init)
      throws CertificateException, NoSuchAlgorithmException, KeyStoreException, VslException,
          IOException, LogManagerException, KeyStoreManagerException, PropertyManagerException,
          ObjectException, OrchestratorException, EntityManagerException {
    manager = new OrchestratorManager(init);

    if (init.missingHost()) {
      init.setHost(manager.property().get("orchestrator", "host").asString());
    }

    if (init.missingPort()) {
      init.setPort(manager.property().get("orchestrator", "port").asInt() + init.getId());
    }

    try {
      // Make sure this Orchestrator is registered.
      manager.keyStore().registerOrchestrator();

      // Start the Orchestrator.
      InetSocketAddress address = new InetSocketAddress(init.getHost(), init.getPort());
      OrchestratorServiceImpl service = new OrchestratorServiceImpl(this);
      server = NettyServerBuilder.forAddress(address).addService(service).build();
      start(init);

      // Create the database.
      database = new OrchestratorDatabase();

      // Start the DS2OS Orchestrator Service.
      String agentUrl = manager.property().get("ds2os", "agent", "url").asString();
      String ds2osKeyStorePath = manager.property().get("ds2os", "keystore", "path").asString();

      ServiceInitializer orchestratorInitializer =
          new ServiceInitializer().setAgentUrl(agentUrl).setKeyStore(ds2osKeyStorePath);
      orchestrationService = new OrchestrationService(orchestratorInitializer, manager.property());

    } catch (OrchestratorException e) {
      close();
      throw e;
    }
  }

  @Override
  public void close() {
    if (orchestrationService != null) orchestrationService.close();
    if (server != null) server.shutdownNow();

    manager.log().info("[+] Server has been shut down.");
  }

  public void openMenu() {
    boolean stop = false;

    while (!stop) {
      System.out.print(
          ":: Menu ::\n"
              + "(1) List location proofs\n"
              + "(2) Get details about a location proof\n"
              + "> ");
      switch (SCANNER.nextLine()) {
        case "1":
          for (String listEntry : database.listEntries()) System.out.println("- " + listEntry);
          break;

        case "2":
          System.out.print("> Identifier:\n> ");
          System.out.println(database.getEntry(SCANNER.nextLine()));
          break;

        default:
          stop = true;
          break;
      }

      System.out.println();
    }
  }

  public SignedProveLocationResponse proveLocation(SignedProveLocationRequest signedRequest)
      throws OrchestratorException, MessageValidatorException, EntityManagerException,
          KeyStoreManagerException {
    final Entity orchestrator = manager.entity().current();
    SignedMessageValidator verifier = new SignedMessageValidator(manager);
    ProveLocationRequest request = signedRequest.getMessage();

    new MessageValidator(manager)
        .init(request)
        .assertReceiver(orchestrator)
        .assertCertificateValid()
        .assertNonceValid()
        .assertSenderKnown()
        .validate();

    new SignedMessageValidator(manager).init(signedRequest).assertSignature().validate();

    // Start signed message assertions.
    verifier.assertSignature();

    // Get proof identifier.
    String identifier =
        request
            .getSignedRequestAuthorizationResponse()
            .getMessage()
            .getProperties()
            .getIdentifier();

    // Make sure a previous entry exists.
    if (!database.hasEntry(identifier))
      throw new OrchestratorException("An authorization is required to prove your location");

    // Retrieve information from the database.
    DatabaseEntry entry = database.getEntry(identifier);
    SignedRequestAuthorizationResponse signedAuth = entry.getSignedAuth();
    RequestAuthorizationResponse auth = signedAuth.getMessage();

    if (!signedAuth.equals(request.getSignedRequestAuthorizationResponse()))
      throw new OrchestratorException("Unrecognized authorization");

    if (!auth.getValidity().isValid())
      throw new OrchestratorException("The authorization is no longer valid");

    // Warning.
    manager.log().info("[+] Starting proof of location with ID '%s'.", identifier);

    // Time to begin the proof of location.
    try {
      orchestrationService.proveLocation(
          auth.getProperties(), entry.getServices(), new HandleProof(entry));

    } catch (BroadException e) {
      e.printStackTrace();
      throw new OrchestratorException("Could not start proof of location: %s", e.getMessage());
    }

    // Sign the response.
    return SignedProveLocationResponse.newBuilder()
        .setMessage(
            ProveLocationResponse.newBuilder()
                .setSender(orchestrator)
                .setCertificateBytes(manager.keyStore())
                .setReceiver(request.getSender())
                .setNonce(manager.nonce())
                .build())
        .setSignature(manager.keyStore())
        .build();
  }

  public SignedRequestAuthorizationResponse requestAuthorization(
      SignedRequestAuthorizationRequest signedRequest)
      throws EntityManagerException, KeyStoreManagerException, MessageValidatorException,
          LocationException, OrchestrationServiceException {
    final Entity orchestrator = manager.entity().current();
    RequestAuthorizationRequest request = signedRequest.getMessage();

    // Validate the message.
    new MessageValidator(manager)
        .init(request)
        .assertReceiver(orchestrator)
        .assertCertificateValid()
        .assertNonceValid()
        .validate();

    // Register the prover, if it is not registered yet.
    final Entity prover = request.getSender();
    if (!manager.entity().knows(prover)) manager.entity().register(prover);

    // Validate the signed message.
    new SignedMessageValidator(manager).init(signedRequest).assertSignature().validate();

    // Generate proof properties.
    LocationProofProperties properties = new LocationProofProperties();
    properties.setIdentifier(RandomStringUtils.randomAlphanumeric(10)); // Random proof identifier.
    properties.setFragmentCount(2); // TODO Random?
    properties.setFragmentLength(15000); // TODO Random?
    properties.setSeed(new SecureRandom().nextLong()); // Random seed to generate quirky properties.

    // Start composing the authorization.
    RequestAuthorizationResponseBuilder authBuilder =
        RequestAuthorizationResponse.newBuilder()
            .setSender(orchestrator)
            .setReceiver(prover)
            .setCertificateBytes(manager.keyStore())
            .setNonce(manager.nonce())
            .setRequestAuthorizationRequest(signedRequest)
            .setProperties(properties)
            .setValidity(new TimeInterval(5, MINUTES)); // TODO How to determine this period?

    // Retrieve selected beacons, based upon eligible beacons.
    LinkedHashSet<AdaptationServiceView> selectedServices =
        orchestrationService.getSelectedServices(
            request.getLocation(), request.getSupportedBeacons());

    ArrayList<Beacon> selectedBeacons =
        selectedServices.parallelStream()
            .map(view -> new Beacon(view.getDescriptor()))
            .collect(Collectors.toCollection(ArrayList::new));

    // Build the authorization.
    RequestAuthorizationResponse auth = authBuilder.setSelectedBeacons(selectedBeacons).build();

    // Sign the authorization.
    SignedRequestAuthorizationResponse signedAuth =
        SignedRequestAuthorizationResponse.newBuilder()
            .setMessage(auth)
            .setSignature(manager.keyStore())
            .build();

    // Store it in the database.
    database.removeEntry(properties.getIdentifier()); // Start from scratch.
    DatabaseEntry entry = new DatabaseEntry();
    entry.setSignedAuth(signedAuth);
    entry.setServices(selectedServices);
    database.addEntry(properties.getIdentifier(), entry);

    return signedAuth;
  }

  public SignedRequestProofInformationResponse requestProofInformation(
      SignedRequestProofInformationRequest signedRequest)
      throws EntityManagerException, MessageValidatorException, OrchestratorException,
          KeyStoreManagerException {
    final Entity orchestrator = manager.entity().current();
    final Entity verifier = manager.getVerifier();

    // Validate the request.
    new MessageValidator(manager)
        .init(signedRequest.getMessage())
        .assertSender(verifier)
        .assertReceiver(orchestrator)
        .assertCertificateValid()
        .assertNonceValid()
        .validate();

    new SignedMessageValidator(manager).init(signedRequest).assertSignature().validate();

    String identifierToLookup = signedRequest.getMessage().getIdentifier();
    if (!database.hasEntry(identifierToLookup))
      throw new OrchestratorException("Unrecognized proof identifier '%s'.", identifierToLookup);

    // Get signals for the requested location proof.
    LinkedHashSet<Signal> signals = database.getEntry(identifierToLookup).getSignals();

    return newBuilder()
        .setMessage(
            RequestProofInformationResponse.newBuilder()
                .setSender(orchestrator)
                .setReceiver(verifier)
                .setCertificateBytes(manager.keyStore())
                .setNonce(manager.nonce())
                .setSignals(new LinkedList<>(signals))
                .build())
        .setSignature(manager.keyStore())
        .build();
  }

  public void start(ServerInitializer init) throws OrchestratorException {
    try {
      server.start();
      manager.log().info("[+] Started server at %s:%d.", init.getHost(), init.getPort());

    } catch (IOException e) {
      e.printStackTrace();
      throw new OrchestratorException(e.getMessage());
    }
  }

  private static final class HandleProof
      implements AsyncListener<LinkedHashSet<AdaptationServiceView>, BroadException> {
    private final DatabaseEntry entry;

    public HandleProof(DatabaseEntry entry) {
      this.entry = entry.clone();
    }

    @Override
    public void onComplete(LinkedHashSet<AdaptationServiceView> services) {
      for (AdaptationServiceView service : services) entry.addSignals(service.getSignal());
    }
  }
}
