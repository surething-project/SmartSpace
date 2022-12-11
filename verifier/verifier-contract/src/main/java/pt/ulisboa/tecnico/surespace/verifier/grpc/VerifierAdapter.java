/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.grpc;

import com.google.protobuf.ByteString;
import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.verifier.message.SignedVerifyProofRequest;
import pt.ulisboa.tecnico.surespace.verifier.message.SignedVerifyProofResponse;

public final class VerifierAdapter {
  // VerifyProof

  public static VerifyProofRequest adapt(SignedVerifyProofRequest request) {
    return VerifyProofRequest.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedVerifyProofRequest adapt(VerifyProofRequest request) {
    return Object.fromBytes(
        request.getSignedContent().toByteArray(), SignedVerifyProofRequest.class);
  }

  public static VerifyProofResponse adapt(SignedVerifyProofResponse request) {
    return VerifyProofResponse.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedVerifyProofResponse adapt(VerifyProofResponse request) {
    return Object.fromBytes(
        request.getSignedContent().toByteArray(), SignedVerifyProofResponse.class);
  }
}
