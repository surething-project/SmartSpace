/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

@SuppressWarnings("unused")
public interface KeyStoreManagerInterface extends ManagerInterface<KeyStoreManagerException> {
  byte[] bytesFromCertificate(Certificate certificate) throws KeyStoreManagerException;

  byte[][] bytesFromCertificateChain(Certificate[] certificateChain)
      throws KeyStoreManagerException;

  Certificate[] certificateChainFromBytes(byte[][] certificateChainBytes)
      throws KeyStoreManagerException;

  Certificate certificateFromBytes(byte[] certificateBytes) throws KeyStoreManagerException;

  boolean containsCertificate(Entity entity);

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  boolean containsKey(Entity entity);

  InputStream exportCertificateChain(Entity entity) throws KeyStoreManagerException;

  InputStream exportPrivateKey(Entity entity) throws KeyStoreManagerException;

  PKCS10CertificationRequest generateCsr(KeyPair pair, X500Name subject)
      throws KeyStoreManagerException;

  Certificate getCertificate(Entity entity) throws KeyStoreManagerException;

  Certificate[] getCertificateChain(Entity entity) throws KeyStoreManagerException;

  PrivateKey getPrivateKey(Entity entity) throws KeyStoreManagerException;

  boolean isCorrectlySigned(Entity entity, byte[] signedData, byte[] signature);

  boolean isCorrectlySigned(Certificate certificate, byte[] signedData, byte[] signature);

  boolean isCorrectlySignedWithPublicKey(Entity entity, byte[] signedData, byte[] signature);

  boolean isCorrectlySignedWithPublicKey(
      PrivateKey privateKey, byte[] signedData, byte[] signature);

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  boolean isValidCertificate(Certificate certificate);

  void removeCertificateEntry(Entity entity) throws KeyStoreManagerException;

  void removeKeyEntry(Entity entity) throws KeyStoreManagerException;

  void setCertificateEntry(Entity entity, Certificate certificate) throws KeyStoreManagerException;

  void setKeyEntry(Entity entity, PrivateKey privateKey, Certificate[] certificateChain)
      throws KeyStoreManagerException;

  byte[] signData(Entity entity, byte[] data) throws KeyStoreManagerException;

  byte[] signData(PrivateKey privateKey, byte[] data) throws KeyStoreManagerException;

  byte[] signDataWithPublicKey(Entity entity, byte[] data) throws KeyStoreManagerException;

  byte[] signDataWithPublicKey(PublicKey publicKey, byte[] data) throws KeyStoreManagerException;
}
