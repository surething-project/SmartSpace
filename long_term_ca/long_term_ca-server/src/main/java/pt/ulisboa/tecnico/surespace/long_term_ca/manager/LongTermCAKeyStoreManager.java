/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.long_term_ca.manager;

import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.manager.EntityManager;
import pt.ulisboa.tecnico.surespace.common.manager.KeyStoreManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;
import static java.security.KeyFactory.getInstance;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.YEAR;
import static org.bouncycastle.asn1.oiw.OIWObjectIdentifiers.idSHA1;
import static org.bouncycastle.asn1.x509.Extension.*;
import static org.bouncycastle.asn1.x509.KeyUsage.*;
import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public final class LongTermCAKeyStoreManager extends KeyStoreManager {
  private final LongTermCAManager manager;

  public LongTermCAKeyStoreManager(LongTermCAManager manager) throws KeyStoreManagerException {
    super(manager.property().get("keystore", "key").asString().toCharArray(), manager.property());

    this.manager = manager;
    afterLoading();

    // Create the root and the two intermediate certification authorities.
    createCertificates();

    // Create two files: one export for the PCA and another export for the remaining users.
    exportKeyStores();
  }

  public void addNode(CertificateProperties properties) throws KeyStoreManagerException {
    KeyPair keyPair = generateKeyPair();
    PKCS10CertificationRequest csr = generateCsr(keyPair, properties.getSubject().getX500Name());

    if (isRootCa(properties.getSubject()))
      properties.getIssuer().setPrivateKey(keyPair.getPrivate());
    Certificate certificate = signCsr(csr, properties);

    Certificate[] rootChain = new Certificate[0];
    if (containsKey(properties.getIssuer()))
      rootChain = getCertificateChain(properties.getIssuer());
    Certificate[] nodeChain = new Certificate[rootChain.length + 1];
    arraycopy(rootChain, 0, nodeChain, 1, rootChain.length);
    nodeChain[0] = certificate;

    // Add entry to the keystore.
    setCertificateEntry(properties.getSubject(), certificate);
    setKeyEntry(properties.getSubject(), keyPair.getPrivate(), nodeChain);
  }

  private void createCaNode(CertificateProperties properties) throws KeyStoreManagerException {
    try {
      // We must also specify the validity here.
      if (isRootCa(properties.getSubject())) {
        properties.setValidity(getPropertyByCA("rca", "validity").asInt());

      } else if (isRootCa(properties.getIssuer())) {
        properties.setValidity(getPropertyByCA("rca", "child", "validity").asInt());

        Certificate rootCa = getCertificate(getCACertificateEntityByName("rca"));
        properties.addExtension(getExtensionAuthorityKeyIdentifier(rootCa));
      }

      properties.addExtension(getExtensionKeyUsage());
      properties.addExtension(getExtensionBasicConstraints());
      addNode(properties);

    } catch (IOException | CertificateEncodingException | OperatorCreationException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  private void createCertificates() throws KeyStoreManagerException {
    // Create the Root Certificate Authority.
    createIntermediateCa(getCACertificateEntityByName("rca"));

    try {
      // Create both the Long-Term and the Pseudonym Certificate Authorities.
      // Bear in mind they are entities of the system!
      createIntermediateCa(managerEntity().getByPath("surespace://rca/ltca"));
      createIntermediateCa(managerEntity().getByPath("surespace://rca/pca"));

    } catch (EntityManagerException e) {
      e.printStackTrace();
      throw new KeyStoreManagerException(e.getMessage());
    }

    // Create both the Orchestrator and the Verifier Certificate Authorities.
    createIntermediateCa(getCACertificateEntityByName("oca"));
    createIntermediateCa(getCACertificateEntityByName("vca"));

    managerLog().info("[+] Generated all CA certificates.");
  }

  private void createIntermediateCa(Entity entity) throws KeyStoreManagerException {
    if (!containsCertificate(entity)) {
      CertificateProperties properties = new CertificateProperties();
      properties.setIssuer(getCACertificateEntityByName("rca"));

      CertificateEntity certificateEntity = new CertificateEntity();
      certificateEntity.setAlias(entity.getDescriptiveUId());

      String type = entity.getType().toLowerCase();
      certificateEntity.setX500Name(getX500NameByCA(type));
      properties.setSubject(certificateEntity);

      createCaNode(properties);
    }
  }

  private void createIntermediateCa(CertificateEntity subjectCa) throws KeyStoreManagerException {
    if (!containsCertificate(subjectCa)) {
      CertificateProperties properties = new CertificateProperties();
      properties.setIssuer(getCACertificateEntityByName("rca"));
      properties.setSubject(subjectCa);

      createCaNode(properties);
    }
  }

  public KeyStore createKeyStoreWithCACertificates() throws KeyStoreManagerException {
    try {
      KeyStore caKeyStore =
          KeyStore.getInstance(managerProperty().get("keystore", "instance").asString());
      load(caKeyStore, null);

      // Load all relevant certificates.
      setCertificateEntry(caKeyStore, "rca");
      setCertificateEntry(caKeyStore, managerEntity().getByPath("surespace://rca/ltca"));
      setCertificateEntry(caKeyStore, managerEntity().getByPath("surespace://rca/pca"));
      setCertificateEntry(caKeyStore, "oca");
      setCertificateEntry(caKeyStore, "vca");

      return caKeyStore;

    } catch (KeyStoreManagerException | KeyStoreException | EntityManagerException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  private synchronized void exportKeyStores() throws KeyStoreManagerException {
    File regular = new File(managerProperty().get("keystore", "export", "path").asString());
    if (!regular.exists()) {
      try (FileOutputStream outputStream = new FileOutputStream(regular)) {
        KeyStore keyStore = createKeyStoreWithCACertificates();
        keyStore.store(outputStream, getKeyStorePassword());
        managerLog().info("[+] Regular keystore was successfully exported.");

      } catch (IOException
          | KeyStoreException
          | NoSuchAlgorithmException
          | CertificateException e) {

        e.printStackTrace();
        throw new KeyStoreManagerException(e.getMessage());
      }
    }

    // Current version does not support a PCA.
    /*
    File pca = new File(managerProperty().get("keystore", "export", "pca", "path").asString());
    if (!pca.exists()) {
      try (FileOutputStream outputStream = new FileOutputStream(pca)) {
        KeyStore keyStore = createKeyStoreWithCACertificates();

        Entity pcaEntity = managerEntity().getByPath("surespace://rca/pca");
        setKeyEntry(keyStore, pcaEntity);
        keyStore.store(outputStream, getKeyStorePassword());

        // We must remove the key of the PCA.
        removeKeyEntry(pcaEntity);
        managerLog().info("[+] PCA keystore was successfully exported.");

      } catch (IOException
          | EntityManagerException
          | KeyStoreException
          | NoSuchAlgorithmException
          | CertificateException e) {

        e.printStackTrace();
        throw new KeyStoreManagerException(e.getMessage());
      }
    }
    */
  }

  private Extension getExtensionAuthorityKeyIdentifier(Certificate parentCertificate)
      throws IOException, CertificateEncodingException, OperatorCreationException {
    AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(idSHA1);
    DigestCalculator digestCalculator = new BcDigestCalculatorProvider().get(algorithmIdentifier);

    X509CertificateHolder certHolder = new X509CertificateHolder(parentCertificate.getEncoded());
    X509ExtensionUtils extensionUtils = new X509ExtensionUtils(digestCalculator);
    AuthorityKeyIdentifier keyIdentifier = extensionUtils.createAuthorityKeyIdentifier(certHolder);

    return new Extension(authorityKeyIdentifier, false, keyIdentifier.getEncoded());
  }

  private Extension getExtensionBasicConstraints() throws IOException {
    return new Extension(basicConstraints, false, new BasicConstraints(true).getEncoded());
  }

  private Extension getExtensionKeyUsage() throws IOException {
    return new Extension(
        keyUsage,
        true,
        new KeyUsage(digitalSignature | nonRepudiation | keyCertSign | cRLSign).getEncoded());
  }

  private boolean isRootCa(CertificateEntity certificateEntity) throws KeyStoreManagerException {
    return certificateEntity == getCACertificateEntityByName("rca");
  }

  private File keyStoreFile() {
    return new File(managerProperty().get("keystore", "path").asString());
  }

  @Override
  protected InputStream keyStoreInputStream() throws KeyStoreManagerException {
    try {
      return new FileInputStream(keyStoreFile());

    } catch (FileNotFoundException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  @Override
  protected OutputStream keyStoreOutputStream() throws KeyStoreManagerException {
    try {
      return new FileOutputStream(keyStoreFile());

    } catch (FileNotFoundException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }
  }

  private EntityManager managerEntity() {
    return manager.entity();
  }

  private LongTermCALogManager managerLog() {
    return manager.log();
  }

  private LongTermCAPropertyManager managerProperty() {
    return manager.property();
  }

  private Certificate[] registerByCsr(
      CertificateEntity ca, Entity subjectEntity, PKCS10CertificationRequest certificationRequest)
      throws KeyStoreManagerException {

    // Check if alias already exists.
    if (containsCertificate(subjectEntity))
      throw new KeyStoreManagerException("Entity '" + subjectEntity + "' already exists");

    // Start building the certificate properties. In the first place, specify the issuer of the
    // certificate, based on the provided CA name.
    CertificateProperties certificateProperties = new CertificateProperties();
    certificateProperties.setIssuer(ca);

    // Compute the equivalent of the provided Entity.
    CertificateEntity subject = new CertificateEntity();
    subject.setAlias(subjectEntity.getPath());
    certificateProperties.setSubject(subject);

    // Get the certificate validity.
    certificateProperties.setValidity(getPropertyByCA(ca.getName(), "child", "validity").asInt());

    // Get the entire certificate chain so the client can install it.
    Certificate cert = signCsr(certificationRequest, certificateProperties);
    Certificate[] oldChain = getCertificateChain(ca);
    Certificate[] newChain = new Certificate[oldChain.length + 1];
    arraycopy(oldChain, 0, newChain, 1, oldChain.length);
    newChain[0] = cert;

    // Add user certificate to our keystore.
    setCertificateEntry(subjectEntity, cert);
    return newChain;
  }

  private Certificate[] registerByCsr(
      Entity issuerEntity, Entity subjectEntity, PKCS10CertificationRequest certificationRequest)
      throws KeyStoreManagerException {
    CertificateEntity issuer = getCACertificateEntityByName(issuerEntity.getType());
    issuer.setAlias(issuerEntity.getDescriptiveUId());

    return registerByCsr(issuer, subjectEntity, certificationRequest);
  }

  private Certificate[] registerByCsr(
      String name, Entity subjectEntity, PKCS10CertificationRequest certificationRequest)
      throws KeyStoreManagerException {
    return registerByCsr(getCACertificateEntityByName(name), subjectEntity, certificationRequest);
  }

  public Certificate[] registerOrchestrator(Entity entity, PKCS10CertificationRequest csr)
      throws KeyStoreManagerException {
    return registerByCsr("oca", entity, csr);
  }

  public Certificate[] registerProver(Entity entity, PKCS10CertificationRequest csr)
      throws KeyStoreManagerException, EntityManagerException {
    return registerByCsr(managerEntity().getByPath("surespace://rca/ltca"), entity, csr);
  }

  public Certificate[] registerVerifier(Entity entity, PKCS10CertificationRequest csr)
      throws KeyStoreManagerException {
    return registerByCsr("vca", entity, csr);
  }

  private void setCertificateEntry(KeyStore keyStore, Entity entity)
      throws KeyStoreManagerException {
    setCertificateEntry(keyStore, entity, getCertificate(entity));
  }

  private void setCertificateEntry(KeyStore keyStore, String name) throws KeyStoreManagerException {
    CertificateEntity ca = getCACertificateEntityByName(name);
    setCertificateEntry(keyStore, ca, getCertificate(ca));
  }

  private void setKeyEntry(KeyStore keyStore, Entity entity) throws KeyStoreManagerException {
    setKeyEntry(keyStore, entity, getPrivateKey(entity), getCertificateChain(entity));
  }

  public Certificate signCsr(PKCS10CertificationRequest csr, CertificateProperties properties)
      throws KeyStoreManagerException {
    try {
      // Get the subject public key.
      byte[] encodedPublicKeyInfo = csr.getSubjectPublicKeyInfo().getEncoded();
      X509EncodedKeySpec subjectKeySpec = new X509EncodedKeySpec(encodedPublicKeyInfo);
      PublicKey publicKey = getInstance(getCertSigAlgId()).generatePublic(subjectKeySpec);

      // Generate a random number to be used as certificate serial number.
      BigInteger serial = BigInteger.valueOf(currentTimeMillis());

      // Get the correct validity.
      Calendar calendar = Calendar.getInstance();
      calendar.add(DAY_OF_YEAR, -1);
      Date notBefore = calendar.getTime();
      calendar.add(YEAR, properties.getValidity());
      Date notAfter = calendar.getTime();

      // According to the given information, get an instance of a certificate builder.
      JcaX509v3CertificateBuilder certBuilder =
          new JcaX509v3CertificateBuilder(
              properties.getIssuer().getX500Name(),
              serial,
              notBefore,
              notAfter,
              csr.getSubject(),
              publicKey);

      // Add necessary extensions.
      for (Extension extension : properties.getExtensions()) certBuilder.addExtension(extension);

      JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder(getCertDigAlgId());
      // Choose where to pick the private key from.
      PrivateKey issuerPrivateKey = properties.getIssuer().getPrivateKey();
      properties.getIssuer().setPrivateKey(null); // Highly recommended.
      if (issuerPrivateKey == null) issuerPrivateKey = getPrivateKey(properties.getIssuer());
      ContentSigner contentSigner = contentSignerBuilder.build(issuerPrivateKey);

      // Build the certificate.
      X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);
      return new JcaX509CertificateConverter()
          .setProvider(PROVIDER_NAME)
          .getCertificate(certificateHolder);

    } catch (IOException
        | CertificateException
        | NoSuchAlgorithmException
        | OperatorCreationException
        | InvalidKeySpecException e) {

      throw new KeyStoreManagerException(e.getMessage());
    }
  }
}
