/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.surespace.common.location.exception.LocationException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageValidatorException;
import pt.ulisboa.tecnico.surespace.ds2os.service.exception.OrchestrationServiceException;
import pt.ulisboa.tecnico.surespace.orchestrator.domain.Orchestrator;
import pt.ulisboa.tecnico.surespace.orchestrator.domain.exception.OrchestratorException;
import pt.ulisboa.tecnico.surespace.orchestrator.grpc.*;

import static pt.ulisboa.tecnico.surespace.orchestrator.grpc.OrchestratorAdapter.adapt;

public final class OrchestratorServiceImpl
    extends OrchestratorServiceGrpc.OrchestratorServiceImplBase {
  private final Orchestrator orchestrator;

  public OrchestratorServiceImpl(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  private static StatusRuntimeException exceptionFromDomain(Exception e) {
    return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
  }

  @Override
  public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
    orchestrator.manager.log().info("[+] Handling ping.");
    responseObserver.onNext(PingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void proveLocation(
      ProveLocationRequest request, StreamObserver<ProveLocationResponse> responseObserver) {
    orchestrator.manager.log().info("[+] Handling proveLocation.");

    try {
      responseObserver.onNext(adapt(orchestrator.proveLocation(adapt(request))));
      responseObserver.onCompleted();

    } catch (MessageValidatorException
        | KeyStoreManagerException
        | EntityManagerException
        | OrchestratorException e) {

      e.printStackTrace();
      responseObserver.onError(exceptionFromDomain(e));
    }
  }

  @Override
  public void requestAuthorization(
      RequestAuthorizationRequest request,
      StreamObserver<RequestAuthorizationResponse> responseObserver) {

    try {
      orchestrator.manager.log().info("[+] Handling requestAuthorization.");
      responseObserver.onNext(adapt(orchestrator.requestAuthorization(adapt(request))));
      orchestrator.manager.log().info("[+] Handled requestAuthorization.");

      responseObserver.onCompleted();

    } catch (EntityManagerException
        | KeyStoreManagerException
        | MessageValidatorException
        | LocationException
        | OrchestrationServiceException e) {

      e.printStackTrace();
      responseObserver.onError(exceptionFromDomain(e));
    }
  }

  @Override
  public void requestProofInformation(
      RequestProofInformationRequest request,
      StreamObserver<RequestProofInformationResponse> responseObserver) {
    orchestrator.manager.log().info("[+] Handling requestProofInformation.");

    try {
      responseObserver.onNext(adapt(orchestrator.requestProofInformation(adapt(request))));
      responseObserver.onCompleted();

    } catch (EntityManagerException
        | KeyStoreManagerException
        | MessageValidatorException
        | OrchestratorException e) {

      e.printStackTrace();
      responseObserver.onError(exceptionFromDomain(e));
    }
  }
}
