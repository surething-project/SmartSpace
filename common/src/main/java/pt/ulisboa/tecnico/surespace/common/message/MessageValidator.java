/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message;

import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.manager.GlobalManagerInterface;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageAssertionException;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageValidatorException;

import java.security.cert.Certificate;

public final class MessageValidator extends Validator<Message<?>> {
  private Message<?> message;

  public MessageValidator(GlobalManagerInterface manager) {
    super(manager);
  }

  public MessageValidator assertCertificateValid() {
    if (shouldStop()) return this;

    try {
      if (null == message.certificateBytes) {
        setException(new MessageValidatorException("No certificate bytes were found"));
        return this;
      }

      // Make sure the certificate is in the right format.
      Certificate certificate = manager.keyStore().certificateFromBytes(message.certificateBytes);

      // And make sure it is valid.
      if (!manager.keyStore().isValidCertificate(certificate)) {
        setException(new MessageValidatorException("Invalid certificate"));
        return this;
      }

      // Store the certificate for further use.
      final Entity sender = message.sender;
      if (!manager.keyStore().containsCertificate(sender)) {
        try {
          manager.keyStore().setCertificateEntry(sender, certificate);

        } catch (KeyStoreManagerException e) {
          setException(new MessageValidatorException(e.getMessage()));
        }
      }

    } catch (KeyStoreManagerException e) {
      setException(new MessageValidatorException(e.getMessage()));
    }

    return this;
  }

  public MessageValidator assertNonceValid() {
    if (shouldStop()) return this;

    if (!manager.nonce().valid(message.nonce, message.sender))
      setException(new MessageValidatorException("Invalid nonce"));

    return this;
  }

  public MessageValidator assertReceiver(Entity receiver) {
    if (shouldStop()) return this;

    if (!message.receiver.equals(receiver))
      setException(new MessageAssertionException(receiver, message.receiver));

    return this;
  }

  public MessageValidator assertReceiverKnown() {
    if (shouldStop()) return this;

    if (!manager.entity().knows(message.receiver))
      setException(new MessageValidatorException("Unknown receiver"));

    return this;
  }

  public MessageValidator assertReceiverUnknown() {
    if (shouldStop()) return this;

    if (manager.entity().knows(message.receiver))
      setException(new MessageValidatorException("Known receiver"));

    return this;
  }

  public MessageValidator assertSender(Entity sender) {
    if (shouldStop()) return this;

    if (!message.sender.equals(sender))
      setException(new MessageAssertionException(sender, message.sender));

    return this;
  }

  public MessageValidator assertSenderKnown() {
    if (shouldStop()) return this;

    if (!manager.entity().knows(message.sender))
      setException(new MessageValidatorException("Unknown sender"));

    return this;
  }

  public MessageValidator assertSenderUnknown() {
    if (shouldStop()) return this;

    if (manager.entity().knows(message.sender))
      setException(new MessageValidatorException("Known sender"));

    return this;
  }

  @Override
  public MessageValidator init(Message<?> message) {
    this.message = message;
    return this;
  }

  @Override
  protected boolean isInited() {
    return message != null;
  }

  @Override
  protected void reset() {
    message = null;
  }
}
