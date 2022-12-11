/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message;

import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.domain.Nonce;
import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.manager.KeyStoreManagerInterface;
import pt.ulisboa.tecnico.surespace.common.manager.NonceManagerInterface;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;

import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Objects;

public abstract class Message<MessageType extends Message<MessageType>>
    extends Object<Message<MessageType>> {
  private static final long serialVersionUID = -8776632918861539930L;
  protected byte[] certificateBytes;
  protected Nonce nonce;
  protected Entity receiver;
  protected Entity sender;

  protected Message() {}

  @Override
  public abstract MessageType clone();

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof Message)) return false;
    Message<?> message = (Message<?>) o;
    return Arrays.equals(certificateBytes, message.certificateBytes)
        && nonce.equals(message.nonce)
        && receiver.equals(message.receiver)
        && sender.equals(message.sender);
  }

  public final byte[] getCertificateBytes() {
    return certificateBytes.clone();
  }

  protected final void setCertificateBytes(byte[] certificateBytes) {
    this.certificateBytes = certificateBytes.clone();
  }

  public final Nonce getNonce() {
    return nonce.clone();
  }

  protected final void setNonce(Nonce nonce) {
    if (nonce == null) throw new IllegalArgumentException("Provided a null nonce");
    this.nonce = nonce;
  }

  public final Entity getReceiver() {
    return receiver.clone();
  }

  protected final void setReceiver(Entity receiver) {
    if (receiver == null) throw new IllegalArgumentException("Provided a null receiver");
    this.receiver = receiver;
  }

  public final Entity getSender() {
    return sender.clone();
  }

  protected final void setSender(Entity sender) {
    if (sender == null) throw new IllegalArgumentException("Provided a null sender");
    this.sender = sender;
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(nonce, receiver, sender);
    result = 31 * result + Arrays.hashCode(certificateBytes);
    return result;
  }

  @Override
  public String toString() {
    return "Message{"
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

  @SuppressWarnings("unchecked")
  public abstract static class MessageBuilder<
      Builder extends MessageBuilder<Builder, Message>,
      Message extends pt.ulisboa.tecnico.surespace.common.message.Message<Message>> {
    protected final Message message;

    public MessageBuilder(Message message) {
      this.message = message;
    }

    public final Message build() {
      return message;
    }

    public Builder setCertificateBytes(KeyStoreManagerInterface keyStoreManager)
        throws KeyStoreManagerException {
      Certificate senderCertificate = keyStoreManager.getCertificate(message.sender);
      message.setCertificateBytes(keyStoreManager.bytesFromCertificate(senderCertificate));
      return (Builder) this;
    }

    @Deprecated
    public Builder setNonce(Nonce nonce) {
      message.setNonce(nonce);
      return (Builder) this;
    }

    public Builder setNonce(NonceManagerInterface nonceManager) {
      message.setNonce(nonceManager.next(message.getReceiver()));
      return (Builder) this;
    }

    public Builder setReceiver(Entity receiver) {
      message.setReceiver(receiver);
      return (Builder) this;
    }

    public Builder setSender(Entity sender) {
      message.setSender(sender);
      return (Builder) this;
    }
  }
}
