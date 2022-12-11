/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageValidatorException;
import pt.ulisboa.tecnico.surespace.long_term_ca.domain.LongTermCA;
import pt.ulisboa.tecnico.surespace.long_term_ca.grpc.LongTermCAServiceGrpc.LongTermCAServiceImplBase;
import pt.ulisboa.tecnico.surespace.long_term_ca.grpc.PingRequest;
import pt.ulisboa.tecnico.surespace.long_term_ca.grpc.PingResponse;
import pt.ulisboa.tecnico.surespace.long_term_ca.grpc.RegisterEntityRequest;
import pt.ulisboa.tecnico.surespace.long_term_ca.grpc.RegisterEntityResponse;

import java.io.IOException;

import static io.grpc.Status.INTERNAL;
import static pt.ulisboa.tecnico.surespace.long_term_ca.grpc.LongTermCAAdapter.adapt;

public class LongTermCAServiceImpl extends LongTermCAServiceImplBase {
  private final LongTermCA longTermCA;

  public LongTermCAServiceImpl(LongTermCA longTermCA) {
    this.longTermCA = longTermCA;
  }

  @Override
  public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
    longTermCA.managerLog().info("[+] Handling ping.");
    responseObserver.onNext(PingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void registerEntity(
      RegisterEntityRequest request, StreamObserver<RegisterEntityResponse> responseObserver) {
    try {
      longTermCA.managerLog().info("[+] Handling registerEntity.");
      responseObserver.onNext(adapt(longTermCA.registerEntity(adapt(request))));
      responseObserver.onCompleted();

    } catch (IOException
        | KeyStoreManagerException
        | EntityManagerException
        | MessageValidatorException e) {

      e.printStackTrace();
      responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
