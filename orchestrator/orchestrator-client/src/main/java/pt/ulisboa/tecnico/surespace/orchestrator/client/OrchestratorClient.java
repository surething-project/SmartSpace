/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.surespace.common.connection.Client;
import pt.ulisboa.tecnico.surespace.common.message.SignedProveLocationRequest;
import pt.ulisboa.tecnico.surespace.common.message.SignedProveLocationResponse;
import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationRequest;
import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationResponse;
import pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationRequest;
import pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationResponse;
import pt.ulisboa.tecnico.surespace.orchestrator.grpc.OrchestratorServiceGrpc;
import pt.ulisboa.tecnico.surespace.orchestrator.grpc.OrchestratorServiceGrpc.OrchestratorServiceFutureStub;
import pt.ulisboa.tecnico.surespace.orchestrator.grpc.PingRequest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pt.ulisboa.tecnico.surespace.orchestrator.grpc.OrchestratorAdapter.adapt;

public final class OrchestratorClient extends Client<OrchestratorClientException> {
  private final ManagedChannel channel;
  private final OrchestratorServiceFutureStub stub;

  public OrchestratorClient(String host, int port) {
    super(host, port);

    channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    stub = OrchestratorServiceGrpc.newFutureStub(channel);
  }

  private static OrchestratorClientException exceptionFromStatus(StatusRuntimeException e) {
    return new OrchestratorClientException(e.getStatus().getDescription());
  }

  @Override
  public void close() {
    channel.shutdownNow();
  }

  @Override
  public void ping() throws OrchestratorClientException {
    try {
      stub.ping(PingRequest.getDefaultInstance()).get(TIMEOUT, MILLISECONDS);

    } catch (StatusRuntimeException e) {
      throw exceptionFromStatus(e);

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new OrchestratorClientException(e.getMessage());
    }
  }

  public SignedProveLocationResponse proveLocation(SignedProveLocationRequest request)
      throws OrchestratorClientException {
    try {
      return adapt(stub.proveLocation(adapt(request)).get(TIMEOUT, MILLISECONDS));

    } catch (StatusRuntimeException e) {
      throw exceptionFromStatus(e);

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new OrchestratorClientException(e.getMessage());
    }
  }

  public SignedRequestAuthorizationResponse requestAuthorization(
      SignedRequestAuthorizationRequest request) throws OrchestratorClientException {
    try {
      return adapt(stub.requestAuthorization(adapt(request)).get(TIMEOUT, MILLISECONDS));

    } catch (StatusRuntimeException e) {
      throw exceptionFromStatus(e);

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new OrchestratorClientException(e.getMessage());
    }
  }

  public SignedRequestProofInformationResponse requestProofInformation(
      SignedRequestProofInformationRequest request) throws OrchestratorClientException {
    try {
      return adapt(stub.requestProofInformation(adapt(request)).get(TIMEOUT, MILLISECONDS));

    } catch (StatusRuntimeException e) {
      throw exceptionFromStatus(e);

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new OrchestratorClientException(e.getMessage());
    }
  }
}
