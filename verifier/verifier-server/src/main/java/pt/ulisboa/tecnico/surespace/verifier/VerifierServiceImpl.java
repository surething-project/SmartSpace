/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.surespace.verifier.domain.Verifier;
import pt.ulisboa.tecnico.surespace.verifier.grpc.PingRequest;
import pt.ulisboa.tecnico.surespace.verifier.grpc.PingResponse;
import pt.ulisboa.tecnico.surespace.verifier.grpc.VerifierServiceGrpc.VerifierServiceImplBase;
import pt.ulisboa.tecnico.surespace.verifier.grpc.VerifyProofRequest;
import pt.ulisboa.tecnico.surespace.verifier.grpc.VerifyProofResponse;

import static pt.ulisboa.tecnico.surespace.verifier.grpc.VerifierAdapter.adapt;

public final class VerifierServiceImpl extends VerifierServiceImplBase {
  private final Verifier verifier;

  public VerifierServiceImpl(Verifier verifier) {
    this.verifier = verifier;
  }

  private static StatusRuntimeException exceptionFromDomain(Exception e) {
    return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
  }

  @Override
  public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
    verifier.managerLog().info("[+] Handling ping.");
    responseObserver.onNext(PingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void verifyProof(
      VerifyProofRequest request, StreamObserver<VerifyProofResponse> responseObserver) {
    verifier.managerLog().info("[+] Handling verifyProof.");

    try {
      responseObserver.onNext(adapt(verifier.verifyProof(adapt(request))));
      responseObserver.onCompleted();
      verifier.managerLog().info("[+] Handled verifyProof.");

    } catch (Exception e) {
      e.printStackTrace();
      responseObserver.onError(exceptionFromDomain(e));
    }
  }
}
