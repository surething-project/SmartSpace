/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.surespace.common.connection.Client;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityRequest;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityResponse;
import pt.ulisboa.tecnico.surespace.long_term_ca.grpc.LongTermCAServiceGrpc;
import pt.ulisboa.tecnico.surespace.long_term_ca.grpc.LongTermCAServiceGrpc.LongTermCAServiceFutureStub;
import pt.ulisboa.tecnico.surespace.long_term_ca.grpc.PingRequest;

import java.util.concurrent.ExecutionException;

import static pt.ulisboa.tecnico.surespace.long_term_ca.grpc.LongTermCAAdapter.adapt;

public final class LongTermCAClient extends Client<LongTermCAClientException> {
  private final ManagedChannel channel;
  private final LongTermCAServiceFutureStub stub;

  public LongTermCAClient(String host, int port) {
    super(host, port);

    channel = ManagedChannelBuilder.forAddress(getHost(), getPort()).usePlaintext().build();
    stub = LongTermCAServiceGrpc.newFutureStub(channel);
  }

  @Override
  public void close() {
    channel.shutdownNow();
  }

  @Override
  public void ping() throws LongTermCAClientException {
    try {
      stub.ping(PingRequest.getDefaultInstance()).get();

    } catch (StatusRuntimeException e) {
      throw new LongTermCAClientException(e.getStatus().getDescription());

    } catch (InterruptedException | ExecutionException e) {
      throw new LongTermCAClientException(e.getMessage());
    }
  }

  public SignedRegisterEntityResponse registerEntity(SignedRegisterEntityRequest request)
      throws LongTermCAClientException {
    try {
      return adapt(stub.registerEntity(adapt(request)).get());

    } catch (StatusRuntimeException e) {
      throw new LongTermCAClientException(e.getStatus().getDescription());

    } catch (InterruptedException | ExecutionException e) {
      throw new LongTermCAClientException(e.getMessage());
    }
  }
}
