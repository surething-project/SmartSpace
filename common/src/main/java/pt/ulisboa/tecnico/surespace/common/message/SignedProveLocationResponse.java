/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message;

import java.util.Arrays;

public final class SignedProveLocationResponse
    extends SignedMessage<SignedProveLocationResponse.ProveLocationResponse> {
  private static final long serialVersionUID = 4362285323048958001L;

  public static SignedProveLocationResponseBuilder newBuilder() {
    return new SignedProveLocationResponseBuilder();
  }

  @Override
  public SignedMessage<ProveLocationResponse> clone() {
    return this;
  }

  public static final class ProveLocationResponse extends Message<ProveLocationResponse> {
    private static final long serialVersionUID = 1994764403913153454L;

    public static ProveLocationResponseBuilder newBuilder() {
      return new ProveLocationResponseBuilder();
    }

    @Override
    public ProveLocationResponse clone() {
      return this;
    }

    @Override
    public String toString() {
      return "ProveLocationResponse{"
          + "certificateBytes="
          + Arrays.toString(certificateBytes)
          + ", nonce="
          + nonce
          + ", receiver="
          + receiver
          + ", sender="
          + sender
          + '}';
    }

    public static final class ProveLocationResponseBuilder
        extends MessageBuilder<ProveLocationResponseBuilder, ProveLocationResponse> {
      public ProveLocationResponseBuilder() {
        super(new ProveLocationResponse());
      }
    }
  }

  public static final class SignedProveLocationResponseBuilder
      extends SignedMessageBuilder<
          SignedProveLocationResponseBuilder, ProveLocationResponse, SignedProveLocationResponse> {
    public SignedProveLocationResponseBuilder() {
      super(new SignedProveLocationResponse());
    }
  }
}
