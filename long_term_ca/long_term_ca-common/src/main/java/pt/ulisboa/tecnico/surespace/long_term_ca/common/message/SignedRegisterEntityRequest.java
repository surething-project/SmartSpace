/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.common.message;

import pt.ulisboa.tecnico.surespace.common.message.Message;
import pt.ulisboa.tecnico.surespace.common.message.SignedMessage;

import java.util.Arrays;

public final class SignedRegisterEntityRequest
    extends SignedMessage<SignedRegisterEntityRequest.RegisterEntityRequest> {
  private static final long serialVersionUID = -1398150571639956435L;

  private SignedRegisterEntityRequest() {}

  public static SignedRegisterEntityRequestBuilder newBuilder() {
    return new SignedRegisterEntityRequestBuilder();
  }

  @Override
  public SignedMessage<RegisterEntityRequest> clone() {
    return this;
  }

  public static final class RegisterEntityRequest extends Message<RegisterEntityRequest> {
    private static final long serialVersionUID = 1069880986684310518L;
    private byte[] csr;

    private RegisterEntityRequest() {}

    public static RegisterEntityRequestBuilder newBuilder() {
      return new RegisterEntityRequestBuilder();
    }

    @Override
    public RegisterEntityRequest clone() {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof RegisterEntityRequest)) return false;
      if (!super.equals(o)) return false;
      RegisterEntityRequest that = (RegisterEntityRequest) o;
      return Arrays.equals(csr, that.csr);
    }

    public byte[] getCsr() {
      return csr.clone();
    }

    private void setCsr(byte[] csr) {
      if (csr == null) throw new IllegalArgumentException("Provided a null CSR");
      this.csr = csr.clone();
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + Arrays.hashCode(csr);
      return result;
    }

    @Override
    public String toString() {
      return "RegisterEntityRequest{"
          + "certificateBytes="
          + Arrays.toString(certificateBytes)
          + ", csr="
          + Arrays.toString(csr)
          + ", nonce="
          + nonce
          + ", receiver="
          + receiver
          + ", sender="
          + sender
          + '}';
    }

    public static final class RegisterEntityRequestBuilder
        extends MessageBuilder<RegisterEntityRequestBuilder, RegisterEntityRequest> {
      public RegisterEntityRequestBuilder() {
        super(new RegisterEntityRequest());
      }

      public RegisterEntityRequestBuilder setCsr(byte[] csr) {
        message.setCsr(csr);
        return this;
      }
    }
  }

  public static final class SignedRegisterEntityRequestBuilder
      extends SignedMessageBuilder<
          SignedRegisterEntityRequestBuilder, RegisterEntityRequest, SignedRegisterEntityRequest> {
    public SignedRegisterEntityRequestBuilder() {
      super(new SignedRegisterEntityRequest());
    }
  }
}
