/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.common.message;

import pt.ulisboa.tecnico.surespace.common.message.Message;
import pt.ulisboa.tecnico.surespace.common.message.SignedMessage;

import java.util.Arrays;

public final class SignedRegisterEntityResponse
    extends SignedMessage<SignedRegisterEntityResponse.RegisterEntityResponse> {
  private static final long serialVersionUID = 8473942023285787801L;

  private SignedRegisterEntityResponse() {}

  public static SignedRegisterEntityResponseBuilder newBuilder() {
    return new SignedRegisterEntityResponseBuilder();
  }

  @Override
  public SignedMessage<RegisterEntityResponse> clone() {
    return this;
  }

  public static final class RegisterEntityResponse extends Message<RegisterEntityResponse> {
    private static final long serialVersionUID = 1868091911099370270L;

    private byte[][] certificateBytesChain;

    private RegisterEntityResponse() {}

    public static RegisterEntityResponseBuilder newBuilder() {
      return new RegisterEntityResponseBuilder();
    }

    @Override
    public RegisterEntityResponse clone() {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof RegisterEntityResponse)) return false;
      if (!super.equals(o)) return false;
      RegisterEntityResponse that = (RegisterEntityResponse) o;
      return Arrays.equals(certificateBytesChain, that.certificateBytesChain);
    }

    public byte[][] getCertificateBytesChain() {
      return certificateBytesChain.clone();
    }

    public void setCertificateBytesChain(byte[][] certificateBytesChain) {
      if (certificateBytesChain == null)
        throw new IllegalArgumentException("Provided a null certificate chain");

      this.certificateBytesChain = certificateBytesChain.clone();
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + Arrays.hashCode(certificateBytesChain);
      return result;
    }

    @Override
    public String toString() {
      return "RegisterEntityResponse{"
          + "certificateBytes="
          + Arrays.toString(certificateBytes)
          + ", certificateBytesChain="
          + Arrays.toString(certificateBytesChain)
          + ", nonce="
          + nonce
          + ", receiver="
          + receiver
          + ", sender="
          + sender
          + '}';
    }

    public static final class RegisterEntityResponseBuilder
        extends MessageBuilder<RegisterEntityResponseBuilder, RegisterEntityResponse> {
      public RegisterEntityResponseBuilder() {
        super(new RegisterEntityResponse());
      }

      public RegisterEntityResponseBuilder setCertificateBytesChain(
          byte[][] certificateBytesChain) {
        message.setCertificateBytesChain(certificateBytesChain);
        return this;
      }
    }
  }

  public static final class SignedRegisterEntityResponseBuilder
      extends SignedMessageBuilder<
          SignedRegisterEntityResponseBuilder,
          RegisterEntityResponse,
          SignedRegisterEntityResponse> {
    public SignedRegisterEntityResponseBuilder() {
      super(new SignedRegisterEntityResponse());
    }
  }
}
