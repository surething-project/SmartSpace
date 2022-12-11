/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message;

import java.util.Arrays;
import java.util.Objects;

public final class SignedProveLocationRequest
    extends SignedMessage<SignedProveLocationRequest.ProveLocationRequest> {
  private static final long serialVersionUID = 7257425538360160953L;

  public static SignedProveLocationRequestBuilder newBuilder() {
    return new SignedProveLocationRequestBuilder();
  }

  @Override
  public SignedMessage<ProveLocationRequest> clone() {
    return this;
  }

  public static final class ProveLocationRequest extends Message<ProveLocationRequest> {
    private static final long serialVersionUID = 4586501719356671379L;
    private SignedRequestAuthorizationResponse signedRequestAuthorizationResponse;

    private ProveLocationRequest() {}

    public static ProveLocationRequestBuilder newBuilder() {
      return new ProveLocationRequestBuilder();
    }

    @Override
    public ProveLocationRequest clone() {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ProveLocationRequest)) return false;
      if (!super.equals(o)) return false;
      ProveLocationRequest that = (ProveLocationRequest) o;
      return signedRequestAuthorizationResponse.equals(that.signedRequestAuthorizationResponse);
    }

    public SignedRequestAuthorizationResponse getSignedRequestAuthorizationResponse() {
      return signedRequestAuthorizationResponse.clone();
    }

    private void setSignedRequestAuthorizationResponse(
        SignedRequestAuthorizationResponse signedRequestAuthorizationResponse) {
      this.signedRequestAuthorizationResponse = signedRequestAuthorizationResponse.clone();
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), signedRequestAuthorizationResponse);
    }

    @Override
    public String toString() {
      return "ProveLocationRequest{"
          + "certificateBytes="
          + Arrays.toString(certificateBytes)
          + ", nonce="
          + nonce
          + ", receiver="
          + receiver
          + ", sender="
          + sender
          + ", signedRequestAuthorizationResponse="
          + signedRequestAuthorizationResponse
          + '}';
    }

    public static final class ProveLocationRequestBuilder
        extends MessageBuilder<ProveLocationRequestBuilder, ProveLocationRequest> {
      public ProveLocationRequestBuilder() {
        super(new ProveLocationRequest());
      }

      public ProveLocationRequestBuilder setSignedRequestAuthorizationResponse(
          SignedRequestAuthorizationResponse signedProofOfAuthorization) {
        message.setSignedRequestAuthorizationResponse(signedProofOfAuthorization);
        return this;
      }
    }
  }

  public static final class SignedProveLocationRequestBuilder
      extends SignedMessageBuilder<
          SignedProveLocationRequestBuilder, ProveLocationRequest, SignedProveLocationRequest> {
    public SignedProveLocationRequestBuilder() {
      super(new SignedProveLocationRequest());
    }
  }
}
