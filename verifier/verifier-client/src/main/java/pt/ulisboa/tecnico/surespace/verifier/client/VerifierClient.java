/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pt.ulisboa.tecnico.surespace.common.async.AsyncListener;
import pt.ulisboa.tecnico.surespace.common.connection.Client;
import pt.ulisboa.tecnico.surespace.verifier.grpc.PingRequest;
import pt.ulisboa.tecnico.surespace.verifier.grpc.VerifierServiceGrpc;
import pt.ulisboa.tecnico.surespace.verifier.grpc.VerifierServiceGrpc.VerifierServiceFutureStub;
import pt.ulisboa.tecnico.surespace.verifier.grpc.VerifyProofResponse;
import pt.ulisboa.tecnico.surespace.verifier.message.SignedVerifyProofRequest;
import pt.ulisboa.tecnico.surespace.verifier.message.SignedVerifyProofResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pt.ulisboa.tecnico.surespace.verifier.grpc.VerifierAdapter.adapt;

public final class VerifierClient extends Client<VerifierClientException> {
  private final ManagedChannel channel;
  private final VerifierServiceFutureStub stub;

  public VerifierClient(String host, int port) {
    super(host, port);

    channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    stub = VerifierServiceGrpc.newFutureStub(channel);
  }

  private static VerifierClientException exceptionFromStatus(StatusRuntimeException e) {
    return new VerifierClientException(e.getStatus().getDescription());
  }

  @Override
  public void close() {
    channel.shutdownNow();
  }

  @Override
  public void ping() throws VerifierClientException {
    try {
      stub.ping(PingRequest.getDefaultInstance()).get(TIMEOUT, MILLISECONDS);

    } catch (StatusRuntimeException e) {
      throw new VerifierClientException(e.getStatus().getDescription());

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new VerifierClientException(e.getMessage());
    }
  }

  public SignedVerifyProofResponse verifyProof(SignedVerifyProofRequest request)
      throws VerifierClientException {
    try {
      return adapt(stub.verifyProof(adapt(request)).get(TIMEOUT, MILLISECONDS));

    } catch (StatusRuntimeException e) {
      throw exceptionFromStatus(e);

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new VerifierClientException(e.getMessage());
    }
  }

  public void verifyProof(
      SignedVerifyProofRequest request,
      AsyncListener<SignedVerifyProofResponse, VerifierClientException> listener) {
    Futures.addCallback(
        stub.verifyProof(adapt(request)),
        new FutureCallback<>() {
          @Override
          public void onFailure(@NotNull Throwable throwable) {
            if (throwable instanceof StatusRuntimeException) {
              listener.onError(exceptionFromStatus((StatusRuntimeException) throwable));

            } else {
              listener.onError(new VerifierClientException(throwable.getMessage()));
            }
          }

          @Override
          public void onSuccess(@Nullable VerifyProofResponse verifyProofResponse) {
            if (verifyProofResponse != null) listener.onComplete(adapt(verifyProofResponse));
          }
        },
        newCachedThreadPool());
  }
}
