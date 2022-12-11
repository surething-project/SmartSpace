/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator;

import pt.ulisboa.tecnico.surespace.common.message.Message;
import pt.ulisboa.tecnico.surespace.common.message.SignedMessage;

import static pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationRequest.RequestProofInformationRequest;

public final class SignedRequestProofInformationRequest
    extends SignedMessage<RequestProofInformationRequest> {
  private static final long serialVersionUID = -8201002983284239502L;

  private SignedRequestProofInformationRequest() {}

  public static SignedRequestProofInformationRequestBuilder newBuilder() {
    return new SignedRequestProofInformationRequestBuilder();
  }

  @Override
  public SignedRequestProofInformationRequest clone() {
    return this;
  }

  public static final class RequestProofInformationRequest
      extends Message<RequestProofInformationRequest> {
    private static final long serialVersionUID = -8641174348801399353L;

    private String identifier;

    private RequestProofInformationRequest() {}

    public static RequestProofInformationRequestBuilder newBuilder() {
      return new RequestProofInformationRequestBuilder();
    }

    @Override
    public RequestProofInformationRequest clone() {
      return this;
    }

    public String getIdentifier() {
      return identifier;
    }

    private void setIdentifier(String identifier) {
      this.identifier = identifier;
    }

    public static final class RequestProofInformationRequestBuilder
        extends MessageBuilder<
            RequestProofInformationRequestBuilder, RequestProofInformationRequest> {
      public RequestProofInformationRequestBuilder() {
        super(new RequestProofInformationRequest());
      }

      public RequestProofInformationRequestBuilder setIdentifier(String identifier) {
        message.setIdentifier(identifier);
        return this;
      }
    }
  }

  public static final class SignedRequestProofInformationRequestBuilder
      extends SignedMessageBuilder<
          SignedRequestProofInformationRequestBuilder,
          RequestProofInformationRequest,
          SignedRequestProofInformationRequest> {

    public SignedRequestProofInformationRequestBuilder() {
      super(new SignedRequestProofInformationRequest());
    }
  }
}
