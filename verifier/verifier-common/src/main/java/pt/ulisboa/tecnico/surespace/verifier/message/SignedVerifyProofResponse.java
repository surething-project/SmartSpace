/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.message;

import pt.ulisboa.tecnico.surespace.common.message.Message;
import pt.ulisboa.tecnico.surespace.common.message.SignedMessage;

public final class SignedVerifyProofResponse
    extends SignedMessage<SignedVerifyProofResponse.VerifyProofResponse> {
  private static final long serialVersionUID = 5286984762547406217L;

  private SignedVerifyProofResponse() {}

  public static SignedVerifyProofResponseBuilder newBuilder() {
    return new SignedVerifyProofResponseBuilder();
  }

  @Override
  public SignedVerifyProofResponse clone() {
    return this;
  }

  public static final class SignedVerifyProofResponseBuilder
      extends SignedMessageBuilder<
          SignedVerifyProofResponseBuilder, VerifyProofResponse, SignedVerifyProofResponse> {
    public SignedVerifyProofResponseBuilder() {
      super(new SignedVerifyProofResponse());
    }
  }

  public static final class VerifyProofResponse extends Message<VerifyProofResponse> {
    private static final long serialVersionUID = 4358696789521354268L;
    private boolean proofAccepted;

    private VerifyProofResponse() {}

    public static VerifyProofResponseBuilder newBuilder() {
      return new VerifyProofResponseBuilder();
    }

    @Override
    public VerifyProofResponse clone() {
      return this;
    }

    public boolean isProofAccepted() {
      return proofAccepted;
    }

    private void setProofAccepted(boolean proofAccepted) {
      this.proofAccepted = proofAccepted;
    }

    public static final class VerifyProofResponseBuilder
        extends MessageBuilder<VerifyProofResponseBuilder, VerifyProofResponse> {
      public VerifyProofResponseBuilder() {
        super(new VerifyProofResponse());
      }

      public VerifyProofResponseBuilder setProofAccepted(boolean proofAccepted) {
        message.setProofAccepted(proofAccepted);
        return this;
      }
    }
  }
}
