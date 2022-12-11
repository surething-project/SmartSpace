/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message;

import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.manager.KeyStoreManagerInterface;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Objects;

public abstract class SignedMessage<MessageType extends Message<MessageType>>
    extends Object<SignedMessage<MessageType>> {
  private static final long serialVersionUID = 3914700390570701034L;
  protected MessageType message;
  protected byte[] signature;

  @Override
  public abstract SignedMessage<MessageType> clone();

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof SignedMessage)) return false;
    SignedMessage<?> that = (SignedMessage<?>) o;
    return message.equals(that.message) && Arrays.equals(signature, that.signature);
  }

  public final MessageType getMessage() {
    return message.clone();
  }

  protected final void setMessage(MessageType message) {
    if (message == null) throw new IllegalArgumentException("Provided a null message");
    this.message = message.clone();
  }

  public final byte[] getSignature() {
    return signature.clone();
  }

  protected final void setSignature(byte[] signature) {
    if (signature == null) throw new IllegalArgumentException("Provided a null signature");
    this.signature = signature.clone();
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(message);
    result = 31 * result + Arrays.hashCode(signature);
    return result;
  }

  @Override
  public String toString() {
    return "SignedMessage{"
        + "message="
        + message
        + ", signature="
        + Arrays.toString(signature)
        + '}';
  }

  @SuppressWarnings("unchecked")
  public abstract static class SignedMessageBuilder<
      Builder extends SignedMessageBuilder<Builder, MessageType, SignedMessageType>,
      MessageType extends Message<MessageType>,
      SignedMessageType extends SignedMessage<MessageType>> {
    private final SignedMessageType signedMessage;

    public SignedMessageBuilder(SignedMessageType signedMessage) {
      this.signedMessage = signedMessage;
    }

    public final SignedMessageType build() {
      return signedMessage;
    }

    private byte[] getMessageBytes() {
      return signedMessage.message.getBytes();
    }

    public final Builder setMessage(MessageType message) {
      signedMessage.setMessage(message);
      return (Builder) this;
    }

    public final Builder setSignature(KeyStoreManagerInterface keyStoreManager)
        throws KeyStoreManagerException {
      byte[] signature = keyStoreManager.signData(signedMessage.message.sender, getMessageBytes());
      signedMessage.setSignature(signature);
      return (Builder) this;
    }

    public final Builder setSignature(
        KeyStoreManagerInterface keyStoreManager, PrivateKey privateKey)
        throws KeyStoreManagerException {
      byte[] signature = keyStoreManager.signData(privateKey, getMessageBytes());
      signedMessage.setSignature(signature);
      return (Builder) this;
    }
  }
}
