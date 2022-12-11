/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message;

import pt.ulisboa.tecnico.surespace.common.location.Location;
import pt.ulisboa.tecnico.surespace.common.proof.Beacon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class SignedRequestAuthorizationRequest
    extends SignedMessage<SignedRequestAuthorizationRequest.RequestAuthorizationRequest> {
  private static final long serialVersionUID = -696855965885881562L;

  public static SignedRequestAuthorizationRequestBuilder newBuilder() {
    return new SignedRequestAuthorizationRequestBuilder();
  }

  @Override
  public SignedRequestAuthorizationRequest clone() {
    return this;
  }

  public static final class RequestAuthorizationRequest
      extends Message<RequestAuthorizationRequest> {
    private static final long serialVersionUID = 4207071435771498096L;
    private Location<?> location;
    private ArrayList<Beacon> supportedBeacons;

    private RequestAuthorizationRequest() {}

    public static RequestAuthorizationRequestBuilder newBuilder() {
      return new RequestAuthorizationRequestBuilder();
    }

    @Override
    public RequestAuthorizationRequest clone() {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof RequestAuthorizationRequest)) return false;
      if (!super.equals(o)) return false;
      RequestAuthorizationRequest that = (RequestAuthorizationRequest) o;
      return location.equals(that.location) && supportedBeacons.equals(that.supportedBeacons);
    }

    public final Location<?> getLocation() {
      return location.clone();
    }

    private void setLocation(Location<?> location) {
      if (location == null) throw new IllegalArgumentException("Provided a null location");
      this.location = location.clone();
    }

    public ArrayList<Beacon> getSupportedBeacons() {
      return new ArrayList<>(supportedBeacons);
    }

    public void setSupportedBeacons(ArrayList<Beacon> supportedBeacons) {
      this.supportedBeacons = new ArrayList<>(supportedBeacons);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), location, supportedBeacons);
    }

    @Override
    public String toString() {
      return "RequestAuthorizationRequest{"
          + "certificateBytes="
          + Arrays.toString(certificateBytes)
          + ", location="
          + location
          + ", nonce="
          + nonce
          + ", receiver="
          + receiver
          + ", sender="
          + sender
          + ", supportedBeacons="
          + supportedBeacons
          + '}';
    }

    public static final class RequestAuthorizationRequestBuilder
        extends MessageBuilder<RequestAuthorizationRequestBuilder, RequestAuthorizationRequest> {
      public RequestAuthorizationRequestBuilder() {
        super(new RequestAuthorizationRequest());
      }

      public RequestAuthorizationRequestBuilder setLocation(Location<?> location) {
        message.setLocation(location);
        return this;
      }

      public RequestAuthorizationRequestBuilder setSupportedBeacons(
          ArrayList<Beacon> supportedBeacons) {
        message.setSupportedBeacons(supportedBeacons);
        return this;
      }
    }
  }

  public static final class SignedRequestAuthorizationRequestBuilder
      extends SignedMessageBuilder<
          SignedRequestAuthorizationRequestBuilder,
          RequestAuthorizationRequest,
          SignedRequestAuthorizationRequest> {
    public SignedRequestAuthorizationRequestBuilder() {
      super(new SignedRequestAuthorizationRequest());
    }
  }
}
