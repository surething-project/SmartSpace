/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.message;

import pt.ulisboa.tecnico.surespace.common.manager.GlobalManagerInterface;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageValidatorException;

import java.security.cert.Certificate;

public final class SignedMessageValidator extends Validator<SignedMessage<?>> {
  private SignedMessage<?> message;

  public SignedMessageValidator(GlobalManagerInterface manager) {
    super(manager);
  }

  public SignedMessageValidator assertSignature() {
    if (shouldStop()) return this;

    try {
      checkSignature(manager.keyStore().certificateFromBytes(message.message.certificateBytes));

    } catch (KeyStoreManagerException e) {
      e.printStackTrace();
      setException(new MessageValidatorException(e.getMessage()));
    }

    return this;
  }

  public SignedMessageValidator assertSignature(Certificate certificate) {
    if (shouldStop()) return this;

    checkSignature(certificate);
    return this;
  }

  private void checkSignature(Certificate certificate) {
    if (!manager
        .keyStore()
        .isCorrectlySigned(certificate, message.message.getBytes(), message.signature))
      setException(new MessageValidatorException("Wrong message signature"));
  }

  @Override
  public SignedMessageValidator init(SignedMessage<?> message) {
    this.message = message;
    return this;
  }

  @Override
  protected boolean isInited() {
    return null != message && null != message.message;
  }

  @Override
  protected void reset() {
    message = null;
  }
}
