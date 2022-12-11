/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import no.difi.certvalidator.ValidatorBuilder;
import no.difi.certvalidator.api.CertificateValidationException;
import no.difi.certvalidator.rule.ChainRule;
import no.difi.certvalidator.rule.CriticalExtensionRecognizedRule;
import no.difi.certvalidator.rule.ExpirationRule;
import no.difi.certvalidator.rule.SigningRule;
import no.difi.certvalidator.util.KeyStoreCertificateBucket;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.manager.PropertyManagerInterface.Property;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static org.bouncycastle.asn1.x500.style.BCStyle.*;

public abstract class KeyStoreManager implements KeyStoreManagerInterface {
  private static final String KEY_ENTRY_PREFIX = "key_";
  protected final KeyStore keyStore;
  private final String certDigAlgId;
  private final String certSigAlgId;
  private final int certSigAlgKeySize;
  private final CertificateFactory certificateFactory;
  private final ConcurrentHashMap<String, CertificateEntity> certs = new ConcurrentHashMap<>();
  private final PropertyManagerInterface propertyManager;
  private char[] keyStorePassword;

  protected KeyStoreManager(char[] keyStorePassword, PropertyManagerInterface propertyManager)
      throws KeyStoreManagerException {
    this.propertyManager = propertyManager;
    this.keyStorePassword = keyStorePassword;

    try {
      certificateFactory = CertificateFactory.getInstance("X.509");

      String instanceType = propertyManager.get("keystore", "instance").asString();
      keyStore = KeyStore.getInstance(instanceType);

      certSigAlgId = propertyManager.get("cert", "sig_alg", "id").asString();
      certDigAlgId = propertyManager.get("cert", "dig_alg", "id").asString();
      certSigAlgKeySize = propertyManager.get("cert", "sig_alg", "key", "size").asInt();

      // Load all data from the common properties file.
      populateCertificateEntities();

    } catch (KeyStoreException | CertificateException e) {
      e.printStackTrace();
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  @Override
  public void afterLoading() throws KeyStoreManagerException {
    try {
      load();

    } catch (KeyStoreManagerException e) {
      load(keyStore, null);
    }
  }

  @Override
  public byte[] bytesFromCertificate(Certificate certificate) throws KeyStoreManagerException {
    try {
      return certificate.getEncoded();

    } catch (CertificateEncodingException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  @Override
  public byte[][] bytesFromCertificateChain(Certificate[] certificateChain)
      throws KeyStoreManagerException {
    byte[][] certificateChainBytes = new byte[certificateChain.length][];

    int counter = 0;
    for (Certificate certificate : certificateChain) {
      byte[] certificateBytes = bytesFromCertificate(certificate);
      certificateChainBytes[counter++] = certificateBytes;
    }

    return certificateChainBytes;
  }

  @Override
  public Certificate[] certificateChainFromBytes(byte[][] certificateChainBytes)
      throws KeyStoreManagerException {
    Certificate[] certificateChain = new Certificate[certificateChainBytes.length];

    int counter = 0;
    for (byte[] certificateBytes : certificateChainBytes) {
      certificateChain[counter++] = certificateFromBytes(certificateBytes);
    }

    return certificateChain;
  }

  @Override
  public Certificate certificateFromBytes(byte[] certificateBytes) throws KeyStoreManagerException {
    try {
      return certificateFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));

    } catch (CertificateException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  @Deprecated
  protected final synchronized boolean contains(String alias) {
    try {
      return keyStore.containsAlias(alias);

    } catch (KeyStoreException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Deprecated
  protected final boolean contains(CertificateEntity entity) {
    return contains(entity.getAlias());
  }

  @Override
  public final boolean containsCertificate(Entity entity) {
    return containsCertificate(entity.getDescriptiveUId());
  }

  protected final boolean containsCertificate(CertificateEntity entity) {
    return containsCertificate(entity.getAlias());
  }

  protected final synchronized boolean containsCertificate(String alias) {
    try {
      return keyStore.containsAlias(alias);

    } catch (KeyStoreException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public final boolean containsKey(Entity entity) {
    return containsKey(entity.getDescriptiveUId());
  }

  protected final synchronized boolean containsKey(String alias) {
    try {
      return keyStore.containsAlias(KEY_ENTRY_PREFIX + alias);

    } catch (KeyStoreException e) {
      e.printStackTrace();
      return false;
    }
  }

  protected final boolean containsKey(CertificateEntity entity) {
    return containsKey(entity.getAlias());
  }

  @Override
  public final InputStream exportCertificateChain(Entity entity) throws KeyStoreManagerException {
    try {
      byte[] certificateChainBytes = new byte[] {};
      Certificate[] chain = getCertificateChain(entity);

      for (Certificate certificate : chain)
        certificateChainBytes = ArrayUtils.addAll(certificateChainBytes, certificate.getEncoded());

      return new ByteArrayInputStream(certificateChainBytes);

    } catch (CertificateEncodingException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  @Override
  public final InputStream exportPrivateKey(Entity entity) throws KeyStoreManagerException {
    return new ByteArrayInputStream(getPrivateKey(entity.getDescriptiveUId()).getEncoded());
  }

  @Override
  public final PKCS10CertificationRequest generateCsr(KeyPair pair, X500Name subject)
      throws KeyStoreManagerException {
    try {
      PKCS10CertificationRequestBuilder p10Builder =
          new JcaPKCS10CertificationRequestBuilder(subject, pair.getPublic());
      JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(certDigAlgId);
      ContentSigner signer = csBuilder.build(pair.getPrivate());

      return p10Builder.build(signer);

    } catch (OperatorCreationException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  protected final KeyPair generateKeyPair() throws KeyStoreManagerException {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance(certSigAlgId);
      generator.initialize(certSigAlgKeySize);

      return generator.generateKeyPair();

    } catch (NoSuchAlgorithmException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  private CertificateEntity getCACertificateEntity(String name) {
    CertificateEntity certificateEntity = new CertificateEntity();
    certificateEntity.setName(name);
    certificateEntity.setAlias(getStringPropertyByCA(name, "alias"));
    certificateEntity.setX500Name(getX500NameByCA(name));

    return certificateEntity;
  }

  protected final CertificateEntity getCACertificateEntityByName(String name)
      throws KeyStoreManagerException {
    if (!certs.containsKey(name)) throw new KeyStoreManagerException("CA '%s' not found", name);
    return certs.get(name);
  }

  protected final String getCertDigAlgId() {
    return certDigAlgId;
  }

  protected final String getCertSigAlgId() {
    return certSigAlgId;
  }

  protected final synchronized Certificate getCertificate(String alias)
      throws KeyStoreManagerException {
    try {
      Certificate certificate = keyStore.getCertificate(alias);
      if (certificate == null)
        throw new KeyStoreManagerException("Certificate not found for alias '%s'", alias);

      return certificate;

    } catch (KeyStoreException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  protected final Certificate getCertificate(CertificateEntity entity)
      throws KeyStoreManagerException {
    return getCertificate(entity.getAlias());
  }

  @Override
  public final Certificate getCertificate(Entity entity) throws KeyStoreManagerException {
    return getCertificate(entity.getDescriptiveUId());
  }

  @Override
  public final Certificate[] getCertificateChain(Entity entity) throws KeyStoreManagerException {
    return getCertificateChain(entity.getDescriptiveUId());
  }

  protected final Certificate[] getCertificateChain(CertificateEntity entity)
      throws KeyStoreManagerException {
    return getCertificateChain(entity.getAlias());
  }

  protected final synchronized Certificate[] getCertificateChain(String alias)
      throws KeyStoreManagerException {
    try {
      Certificate[] chain = keyStore.getCertificateChain(KEY_ENTRY_PREFIX + alias);
      if (chain == null)
        throw new KeyStoreManagerException("Certificate chain not found for alias '%s'", alias);

      return chain;

    } catch (KeyStoreException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  protected final char[] getKeyStorePassword() {
    return keyStorePassword;
  }

  protected final synchronized PrivateKey getPrivateKey(String alias)
      throws KeyStoreManagerException {
    try {
      Key key = keyStore.getKey(KEY_ENTRY_PREFIX + alias, keyStorePassword);
      if (key == null)
        throw new KeyStoreManagerException("Private key not found for alias '%s'", alias);

      return (PrivateKey) key;

    } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  @Override
  public final PrivateKey getPrivateKey(Entity entity) throws KeyStoreManagerException {
    return getPrivateKey(entity.getDescriptiveUId());
  }

  protected final PrivateKey getPrivateKey(CertificateEntity entity)
      throws KeyStoreManagerException {
    return getPrivateKey(entity.getAlias());
  }

  protected final Property getPropertyByCA(String name, String... path) {
    String[] propertyArray = new String[2 + path.length];
    propertyArray[0] = "ca";
    propertyArray[1] = name.toLowerCase();
    System.arraycopy(path, 0, propertyArray, 2, path.length);

    return propertyManager.get(propertyArray);
  }

  protected final String getStringPropertyByCA(String name, String... properties) {
    return getPropertyByCA(name, properties).asString();
  }

  protected final X500Name getX500NameByCA(String name) {
    return new X500NameBuilder()
        .addRDN(CN, getStringPropertyByCA(name, "cn"))
        .addRDN(O, getStringPropertyByCA(name, "o"))
        .addRDN(L, getStringPropertyByCA(name, "l"))
        .addRDN(ST, getStringPropertyByCA(name, "st"))
        .addRDN(C, getStringPropertyByCA(name, "c"))
        .build();
  }

  @Override
  public final boolean isCorrectlySigned(Entity entity, byte[] signedData, byte[] signature) {
    try {
      return isCorrectlySigned(getCertificate(entity), signedData, signature);

    } catch (KeyStoreManagerException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean isCorrectlySigned(Certificate certificate, byte[] signedData, byte[] signature) {
    return isCorrectlySigned(certificate.getPublicKey(), signedData, signature);
  }

  private synchronized boolean isCorrectlySigned(
      PublicKey publicKey, byte[] signedData, byte[] signature) {
    try {
      Signature sig = Signature.getInstance(certDigAlgId);
      sig.initVerify(publicKey);
      sig.update(signedData);

      return sig.verify(signature);

    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      e.printStackTrace();
      return false;
    }
  }

  public final synchronized boolean isCorrectlySignedWithPublicKey(
      PrivateKey privateKey, byte[] signedData, byte[] signature) {
    try {
      Cipher cipher = Cipher.getInstance("RSA");
      cipher.init(DECRYPT_MODE, privateKey);

      return Arrays.equals(cipher.doFinal(signedData), signature);

    } catch (NoSuchAlgorithmException
        | InvalidKeyException
        | NoSuchPaddingException
        | BadPaddingException
        | IllegalBlockSizeException e) {

      e.printStackTrace();
      return false;
    }
  }

  @Override
  public final boolean isCorrectlySignedWithPublicKey(
      Entity entity, byte[] signedData, byte[] signature) {
    try {
      return isCorrectlySignedWithPublicKey(getPrivateKey(entity), signedData, signature);

    } catch (KeyStoreManagerException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public final boolean isValidCertificate(Certificate certificate) {
    try {
      ValidatorBuilder.newInstance()
          .addRule(
              new ChainRule(
                  new KeyStoreCertificateBucket(keyStore), new KeyStoreCertificateBucket(keyStore)))
          .addRule(new CriticalExtensionRecognizedRule("2.5.29.15"))
          .addRule(new ExpirationRule())
          .addRule(new SigningRule())
          .build()
          .validate(bytesFromCertificate(certificate));
      return true;

    } catch (CertificateValidationException | KeyStoreManagerException e) {
      e.printStackTrace();
      return false;
    }
  }

  protected abstract InputStream keyStoreInputStream() throws KeyStoreManagerException;

  protected abstract OutputStream keyStoreOutputStream() throws KeyStoreManagerException;

  protected final void load() throws KeyStoreManagerException {
    load(keyStore, keyStoreInputStream());
  }

  protected final synchronized void load(KeyStore keyStore, InputStream inputStream)
      throws KeyStoreManagerException {
    try {
      keyStore.load(inputStream, keyStorePassword);

    } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  private void populateCertificateEntities() {
    putCertificateEntity("rca");
    putCertificateEntity("ltca");
    putCertificateEntity("oca");
    putCertificateEntity("vca");
    putCertificateEntity("pca");
  }

  private void putCertificateEntity(String name) {
    certs.putIfAbsent(name, getCACertificateEntity(name));
  }

  @Override
  public final void removeCertificateEntry(Entity entity) throws KeyStoreManagerException {
    removeCertificateEntry(entity.getDescriptiveUId());
  }

  private void removeCertificateEntry(String alias) throws KeyStoreManagerException {
    try {
      if (keyStore.isCertificateEntry(alias)) removeEntry(alias);

    } catch (KeyStoreException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  private synchronized void removeEntry(String alias) throws KeyStoreManagerException {
    try {
      keyStore.deleteEntry(alias);
      store();
      load();

    } catch (KeyStoreException e) {
      e.printStackTrace();
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  @Override
  public final void removeKeyEntry(Entity entity) throws KeyStoreManagerException {
    removeKeyEntry(entity.getDescriptiveUId());
  }

  private void removeKeyEntry(String alias) throws KeyStoreManagerException {
    try {
      alias = KEY_ENTRY_PREFIX + alias;
      if (keyStore.isKeyEntry(alias)) removeEntry(alias);

    } catch (KeyStoreException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  protected final void setCertificateEntry(CertificateEntity entity, Certificate certificate)
      throws KeyStoreManagerException {
    setCertificateEntry(entity.getAlias(), certificate);
  }

  protected final void setCertificateEntry(String alias, Certificate certificate)
      throws KeyStoreManagerException {
    setCertificateEntry(keyStore, alias, certificate);
  }

  protected final synchronized void setCertificateEntry(
      KeyStore keyStore, String alias, Certificate certificate) throws KeyStoreManagerException {
    try {
      keyStore.setCertificateEntry(alias, certificate);
      store();
      load();

    } catch (KeyStoreException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  protected final void setCertificateEntry(
      KeyStore keyStore, CertificateEntity entity, Certificate certificate)
      throws KeyStoreManagerException {
    setCertificateEntry(keyStore, entity.getAlias(), certificate);
  }

  @Override
  public void setCertificateEntry(Entity entity, Certificate certificate)
      throws KeyStoreManagerException {
    setCertificateEntry(keyStore, entity.getDescriptiveUId(), certificate);
  }

  protected final void setCertificateEntry(
      KeyStore keyStore, Entity entity, Certificate certificate) throws KeyStoreManagerException {
    setCertificateEntry(keyStore, entity.getDescriptiveUId(), certificate);
  }

  protected final void setKeyEntry(
      KeyStore keyStore, Entity entity, PrivateKey privateKey, Certificate[] certificateChain)
      throws KeyStoreManagerException {
    setKeyEntry(keyStore, entity.getDescriptiveUId(), privateKey, certificateChain);
  }

  protected final synchronized void setKeyEntry(
      KeyStore keyStore, String alias, PrivateKey privateKey, Certificate[] certificateChain)
      throws KeyStoreManagerException {
    try {
      keyStore.setKeyEntry(
          KEY_ENTRY_PREFIX + alias, privateKey, keyStorePassword, certificateChain);
      store();
      load();

    } catch (KeyStoreException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  protected final void setKeyEntry(
      String alias, PrivateKey privateKey, Certificate[] certificateChain)
      throws KeyStoreManagerException {
    setKeyEntry(keyStore, alias, privateKey, certificateChain);
  }

  protected final void setKeyEntry(
      CertificateEntity entity, PrivateKey privateKey, Certificate[] certificateChain)
      throws KeyStoreManagerException {
    setKeyEntry(entity.getAlias(), privateKey, certificateChain);
  }

  @Override
  public final void setKeyEntry(
      Entity entity, PrivateKey privateKey, Certificate[] certificateChain)
      throws KeyStoreManagerException {
    setKeyEntry(entity.getDescriptiveUId(), privateKey, certificateChain);
  }

  @Override
  public final byte[] signData(Entity entity, byte[] data) throws KeyStoreManagerException {
    return signData(getPrivateKey(entity), data);
  }

  @Override
  public final synchronized byte[] signData(PrivateKey privateKey, byte[] data)
      throws KeyStoreManagerException {
    try {
      Signature signature = Signature.getInstance(certDigAlgId);
      signature.initSign(privateKey);
      signature.update(data);

      return signature.sign();

    } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {
      e.printStackTrace();
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  @Override
  public final byte[] signDataWithPublicKey(Entity entity, byte[] data)
      throws KeyStoreManagerException {
    return signDataWithPublicKey(getCertificate(entity).getPublicKey(), data);
  }

  @Override
  public final byte[] signDataWithPublicKey(PublicKey publicKey, byte[] data)
      throws KeyStoreManagerException {
    try {
      Cipher cipher = Cipher.getInstance("RSA");
      cipher.init(ENCRYPT_MODE, publicKey);
      return cipher.doFinal(data);

    } catch (NoSuchAlgorithmException
        | InvalidKeyException
        | NoSuchPaddingException
        | BadPaddingException
        | IllegalBlockSizeException e) {

      e.printStackTrace();
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  protected final synchronized void store(char[] password) throws KeyStoreManagerException {
    try (OutputStream outputStream = keyStoreOutputStream()) {
      keyStore.store(outputStream, password);

    } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  protected final void store() throws KeyStoreManagerException {
    store(keyStorePassword);
  }

  protected final void updateKeyStorePassword(char[] newPassword) {
    keyStorePassword = newPassword;
  }

  protected static final class CertificateEntity {
    private String alias;
    private String name;
    private PrivateKey privateKey;
    private X500Name x500Name;

    public CertificateEntity() {
      super();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CertificateEntity)) return false;
      CertificateEntity that = (CertificateEntity) o;
      return name.equals(that.name)
          && alias.equals(that.alias)
          && privateKey.equals(that.privateKey)
          && x500Name.equals(that.x500Name);
    }

    public String getAlias() {
      return alias;
    }

    public void setAlias(String alias) {
      this.alias = alias;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public PrivateKey getPrivateKey() {
      return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
      this.privateKey = privateKey;
    }

    public X500Name getX500Name() {
      return x500Name;
    }

    public void setX500Name(X500Name x500Name) {
      this.x500Name = x500Name;
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, alias, privateKey, x500Name);
    }

    @Override
    public String toString() {
      return "CertificateEntity{"
          + "alias='"
          + alias
          + '\''
          + ", name='"
          + name
          + '\''
          + ", privateKey="
          + privateKey
          + ", x500Name="
          + x500Name
          + '}';
    }
  }

  public static final class CertificateProperties {
    private final HashSet<org.bouncycastle.asn1.x509.Extension> extensions = new HashSet<>();
    private CertificateEntity issuer;
    private CertificateEntity subject;
    private int validity;

    public void addExtension(org.bouncycastle.asn1.x509.Extension extension) {
      extensions.add(extension);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CertificateProperties)) return false;
      CertificateProperties that = (CertificateProperties) o;
      return validity == that.validity
          && issuer.equals(that.issuer)
          && subject.equals(that.subject);
    }

    public HashSet<org.bouncycastle.asn1.x509.Extension> getExtensions() {
      return extensions;
    }

    public CertificateEntity getIssuer() {
      return issuer;
    }

    public void setIssuer(CertificateEntity issuer) {
      this.issuer = issuer;
    }

    public CertificateEntity getSubject() {
      return subject;
    }

    public void setSubject(CertificateEntity subject) {
      this.subject = subject;
    }

    public int getValidity() {
      return validity;
    }

    public void setValidity(int validity) {
      this.validity = validity;
    }

    @Override
    public int hashCode() {
      return Objects.hash(issuer, subject, validity);
    }

    @Override
    public String toString() {
      return "CertificateProperties{"
          + "extensions="
          + extensions
          + ", issuer="
          + issuer
          + ", subject="
          + subject
          + ", validity="
          + validity
          + '}';
    }
  }
}
