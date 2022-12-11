/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator;

import pt.ulisboa.tecnico.surespace.common.message.Message;
import pt.ulisboa.tecnico.surespace.common.message.SignedMessage;
import pt.ulisboa.tecnico.surespace.common.signal.Signal;

import java.util.LinkedList;

import static pt.ulisboa.tecnico.surespace.orchestrator.SignedRequestProofInformationResponse.RequestProofInformationResponse;

public final class SignedRequestProofInformationResponse
    extends SignedMessage<RequestProofInformationResponse> {
  private static final long serialVersionUID = -6257824109232647029L;

  private SignedRequestProofInformationResponse() {}

  public static SignedRequestProofInformationResponseBuilder newBuilder() {
    return new SignedRequestProofInformationResponseBuilder();
  }

  @Override
  public SignedRequestProofInformationResponse clone() {
    return this;
  }

  public static final class RequestProofInformationResponse
      extends Message<RequestProofInformationResponse> {
    private static final long serialVersionUID = -8395140001782528983L;

    private LinkedList<Signal> signals;

    private RequestProofInformationResponse() {}

    public static RequestProofInformationResponseBuilder newBuilder() {
      return new RequestProofInformationResponseBuilder();
    }

    @Override
    public RequestProofInformationResponse clone() {
      return this;
    }

    public LinkedList<Signal> getSignals() {
      LinkedList<Signal> signals = new LinkedList<>();
      for (Signal signal : this.signals) signals.add(signal.clone());
      return signals;
    }

    private void setSignals(LinkedList<Signal> signals) {
      this.signals = new LinkedList<>();
      for (Signal signal : signals) this.signals.add(signal.clone());
    }

    public static final class RequestProofInformationResponseBuilder
        extends MessageBuilder<
            RequestProofInformationResponseBuilder, RequestProofInformationResponse> {
      public RequestProofInformationResponseBuilder() {
        super(new RequestProofInformationResponse());
      }

      public RequestProofInformationResponseBuilder setSignals(LinkedList<Signal> signals) {
        message.setSignals(signals);
        return this;
      }
    }
  }

  public static final class SignedRequestProofInformationResponseBuilder
      extends SignedMessageBuilder<
          SignedRequestProofInformationResponseBuilder,
          RequestProofInformationResponse,
          SignedRequestProofInformationResponse> {
    public SignedRequestProofInformationResponseBuilder() {
      super(new SignedRequestProofInformationResponse());
    }
  }
}
