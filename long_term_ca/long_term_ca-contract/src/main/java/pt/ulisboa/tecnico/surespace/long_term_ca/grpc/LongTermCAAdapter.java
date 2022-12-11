/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.grpc;

import com.google.protobuf.ByteString;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityRequest;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityResponse;

import static pt.ulisboa.tecnico.surespace.common.domain.Object.fromBytes;

public final class LongTermCAAdapter {
  // RegisterEntity

  public static RegisterEntityRequest adapt(SignedRegisterEntityRequest request) {
    return RegisterEntityRequest.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedRegisterEntityRequest adapt(RegisterEntityRequest request) {
    return fromBytes(request.getSignedContent().toByteArray(), SignedRegisterEntityRequest.class);
  }

  public static RegisterEntityResponse adapt(SignedRegisterEntityResponse request) {
    return RegisterEntityResponse.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedRegisterEntityResponse adapt(RegisterEntityResponse request) {
    return fromBytes(request.getSignedContent().toByteArray(), SignedRegisterEntityResponse.class);
  }
}
