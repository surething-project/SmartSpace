/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.message;

import pt.ulisboa.tecnico.surespace.common.message.Message;
import pt.ulisboa.tecnico.surespace.common.message.SignedMessage;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProof;

import static pt.ulisboa.tecnico.surespace.verifier.message.SignedVerifyProofRequest.VerifyProofRequest;

public final class SignedVerifyProofRequest extends SignedMessage<VerifyProofRequest> {
  private static final long serialVersionUID = 4596753711214238857L;

  private SignedVerifyProofRequest() {}

  public static SignedVerifyProofRequestBuilder newBuilder() {
    return new SignedVerifyProofRequestBuilder();
  }

  @Override
  public SignedVerifyProofRequest clone() {
    return this;
  }

  public static final class SignedVerifyProofRequestBuilder
      extends SignedMessageBuilder<
          SignedVerifyProofRequestBuilder, VerifyProofRequest, SignedVerifyProofRequest> {
    public SignedVerifyProofRequestBuilder() {
      super(new SignedVerifyProofRequest());
    }
  }

  public static final class VerifyProofRequest extends Message<VerifyProofRequest> {
    private static final long serialVersionUID = -9151783474420948263L;
    private LocationProof locationProof;

    private VerifyProofRequest() {}

    public static VerifyProofRequestBuilder newBuilder() {
      return new VerifyProofRequestBuilder();
    }

    @Override
    public VerifyProofRequest clone() {
      return this;
    }

    public LocationProof getLocationProof() {
      return locationProof.clone();
    }

    private void setLocationProof(LocationProof locationProof) {
      this.locationProof = locationProof.clone();
    }

    public static final class VerifyProofRequestBuilder
        extends MessageBuilder<VerifyProofRequestBuilder, VerifyProofRequest> {
      public VerifyProofRequestBuilder() {
        super(new VerifyProofRequest());
      }

      public VerifyProofRequestBuilder setLocationProof(LocationProof locationProof) {
        message.setLocationProof(locationProof);
        return this;
      }
    }
  }
}
