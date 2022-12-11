/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message;

import pt.ulisboa.tecnico.surespace.common.domain.TimeInterval;
import pt.ulisboa.tecnico.surespace.common.proof.Beacon;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProofProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static pt.ulisboa.tecnico.surespace.common.message.SignedRequestAuthorizationResponse.RequestAuthorizationResponse;

public final class SignedRequestAuthorizationResponse
    extends SignedMessage<RequestAuthorizationResponse> {
  private static final long serialVersionUID = 8568896586492212364L;

  private SignedRequestAuthorizationResponse() {}

  public static SignedRequestAuthorizationResponseBuilder newBuilder() {
    return new SignedRequestAuthorizationResponseBuilder();
  }

  @Override
  public SignedRequestAuthorizationResponse clone() {
    return this;
  }

  public static final class RequestAuthorizationResponse
      extends Message<RequestAuthorizationResponse> {
    private static final long serialVersionUID = 4207071435771498096L;
    private LocationProofProperties properties;
    private ArrayList<Beacon> selectedBeacons;
    private SignedRequestAuthorizationRequest signedRequestAuthorizationRequest;
    private TimeInterval validity;

    private RequestAuthorizationResponse() {}

    public static RequestAuthorizationResponseBuilder newBuilder() {
      return new RequestAuthorizationResponseBuilder();
    }

    @Override
    public RequestAuthorizationResponse clone() {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof RequestAuthorizationResponse)) return false;
      if (!super.equals(o)) return false;
      RequestAuthorizationResponse that = (RequestAuthorizationResponse) o;
      return properties.equals(that.properties)
          && selectedBeacons.equals(that.selectedBeacons)
          && signedRequestAuthorizationRequest.equals(that.signedRequestAuthorizationRequest)
          && validity.equals(that.validity);
    }

    public LocationProofProperties getProperties() {
      return properties.clone();
    }

    private void setProperties(LocationProofProperties properties) {
      this.properties = properties.clone();
    }

    public ArrayList<Beacon> getSelectedBeacons() {
      return new ArrayList<>(selectedBeacons);
    }

    public void setSelectedBeacons(ArrayList<Beacon> selectedBeacons) {
      this.selectedBeacons = new ArrayList<>(selectedBeacons);
    }

    public SignedRequestAuthorizationRequest getSignedRequestAuthorizationRequest() {
      return signedRequestAuthorizationRequest.clone();
    }

    private void setSignedRequestAuthorizationRequest(
        SignedRequestAuthorizationRequest signedRequestAuthorizationRequest) {
      this.signedRequestAuthorizationRequest = signedRequestAuthorizationRequest.clone();
    }

    public TimeInterval getValidity() {
      return validity.clone();
    }

    private void setValidity(TimeInterval validity) {
      this.validity = validity.clone();
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          super.hashCode(),
          properties,
          selectedBeacons,
          signedRequestAuthorizationRequest,
          validity);
    }

    @Override
    public String toString() {
      return "RequestAuthorizationResponse{"
          + "certificateBytes="
          + Arrays.toString(certificateBytes)
          + ", nonce="
          + nonce
          + ", properties="
          + properties
          + ", receiver="
          + receiver
          + ", selectedBeacons="
          + selectedBeacons
          + ", sender="
          + sender
          + ", signedRequestAuthorizationRequest="
          + signedRequestAuthorizationRequest
          + ", validity="
          + validity
          + '}';
    }

    public static final class RequestAuthorizationResponseBuilder
        extends MessageBuilder<RequestAuthorizationResponseBuilder, RequestAuthorizationResponse> {
      public RequestAuthorizationResponseBuilder() {
        super(new RequestAuthorizationResponse());
      }

      public RequestAuthorizationResponseBuilder setProperties(LocationProofProperties properties) {
        message.setProperties(properties);
        return this;
      }

      public RequestAuthorizationResponseBuilder setRequestAuthorizationRequest(
          SignedRequestAuthorizationRequest requestAuthorizationRequest) {
        message.setSignedRequestAuthorizationRequest(requestAuthorizationRequest);
        return this;
      }

      public RequestAuthorizationResponseBuilder setSelectedBeacons(
          ArrayList<Beacon> selectedBeacons) {
        message.setSelectedBeacons(selectedBeacons);
        return this;
      }

      public RequestAuthorizationResponseBuilder setValidity(TimeInterval validity) {
        message.setValidity(validity);
        return this;
      }
    }
  }

  public static final class SignedRequestAuthorizationResponseBuilder
      extends SignedMessageBuilder<
          SignedRequestAuthorizationResponseBuilder,
          RequestAuthorizationResponse,
          SignedRequestAuthorizationResponse> {
    public SignedRequestAuthorizationResponseBuilder() {
      super(new SignedRequestAuthorizationResponse());
    }
  }
}
