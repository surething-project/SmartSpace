/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.grpc;

import com.google.protobuf.ByteString;
import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.message.SignedProveLocationRequest;
import pt.ulisboa.tecnico.surespace.common.message.SignedProveLocationResponse;
import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationRequest;
import pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationResponse;
import pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationRequest;
import pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationResponse;

public final class OrchestratorAdapter {
  // RequestAuthorization

  public static RequestAuthorizationRequest adapt(SignedRequestAuthorizationRequest request) {
    return RequestAuthorizationRequest.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedRequestAuthorizationRequest adapt(RequestAuthorizationRequest request) {
    return Object.fromBytes(
        request.getSignedContent().toByteArray(), SignedRequestAuthorizationRequest.class);
  }

  public static RequestAuthorizationResponse adapt(SignedRequestAuthorizationResponse request) {
    return RequestAuthorizationResponse.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedRequestAuthorizationResponse adapt(RequestAuthorizationResponse request) {
    return Object.fromBytes(
        request.getSignedContent().toByteArray(), SignedRequestAuthorizationResponse.class);
  }

  // ProveLocation

  public static ProveLocationRequest adapt(SignedProveLocationRequest request) {
    return ProveLocationRequest.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedProveLocationRequest adapt(ProveLocationRequest request) {
    return Object.fromBytes(
        request.getSignedContent().toByteArray(), SignedProveLocationRequest.class);
  }

  public static ProveLocationResponse adapt(SignedProveLocationResponse request) {
    return ProveLocationResponse.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedProveLocationResponse adapt(ProveLocationResponse request) {
    return Object.fromBytes(
        request.getSignedContent().toByteArray(), SignedProveLocationResponse.class);
  }

  // RequestProofInformation

  public static RequestProofInformationRequest adapt(SignedRequestProofInformationRequest request) {
    return RequestProofInformationRequest.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedRequestProofInformationRequest adapt(RequestProofInformationRequest request) {
    return Object.fromBytes(
        request.getSignedContent().toByteArray(), SignedRequestProofInformationRequest.class);
  }

  public static RequestProofInformationResponse adapt(
      SignedRequestProofInformationResponse request) {
    return RequestProofInformationResponse.newBuilder()
        .setSignedContent(ByteString.copyFrom(request.getBytes()))
        .build();
  }

  public static SignedRequestProofInformationResponse adapt(
      RequestProofInformationResponse request) {
    return Object.fromBytes(
        request.getSignedContent().toByteArray(), SignedRequestProofInformationResponse.class);
  }
}
