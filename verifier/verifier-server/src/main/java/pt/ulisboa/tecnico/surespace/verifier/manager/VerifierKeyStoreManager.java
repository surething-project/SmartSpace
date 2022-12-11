/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.manager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.manager.EntityManager;
import pt.ulisboa.tecnico.surespace.common.manager.KeyStoreManager;
import pt.ulisboa.tecnico.surespace.common.manager.PropertyManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.long_term_ca.client.LongTermCAClient;
import pt.ulisboa.tecnico.surespace.long_term_ca.client.LongTermCAClientException;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityRequest;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityRequest.RegisterEntityRequest;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityRequest.RegisterEntityRequest.RegisterEntityRequestBuilder;
import pt.ulisboa.tecnico.surespace.long_term_ca.common.message.SignedRegisterEntityResponse;

import java.io.*;
import java.security.KeyPair;
import java.security.cert.Certificate;

import static org.bouncycastle.asn1.x500.style.BCStyle.CN;

public final class VerifierKeyStoreManager extends KeyStoreManager {
  private final VerifierManager manager;

  protected VerifierKeyStoreManager(VerifierManager manager) throws KeyStoreManagerException {
    super(manager.property().get("keystore", "key").asString().toCharArray(), manager.property());

    this.manager = manager;
    afterLoading();
  }

  private X500Name getVerifierX500Name(Entity verifier) {
    return new X500NameBuilder().addRDN(CN, verifier.getName()).build();
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

  private VerifierLogManager managerLog() {
    return manager.log();
  }

  private PropertyManager managerProperty() {
    return manager.property();
  }

  public void registerVerifier() throws KeyStoreManagerException {
    Entity verifier;
    try {
      verifier = managerEntity().current();

    } catch (EntityManagerException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }

    if (!containsKey(verifier) || !containsCertificate(verifier)) {
      managerLog().warning("[-] Verifier '%s' is not registered.", verifier.getName());

      // Generate key pair and CSR.
      KeyPair keyPair = generateKeyPair();
      PKCS10CertificationRequest csr = generateCsr(keyPair, getVerifierX500Name(verifier));

      // Create a client of the LTCA to register the Verifier.
      String ltcaHost = managerProperty().get("ltca", "host").asString();
      int ltcaPort = managerProperty().get("ltca", "port").asInt();
      LongTermCAClient ltcaClient = new LongTermCAClient(ltcaHost, ltcaPort);

      RegisterEntityRequestBuilder requestBuilder =
          RegisterEntityRequest.newBuilder().setSender(verifier);

      try {
        requestBuilder
            .setReceiver(managerEntity().getByPath("surespace://rca/ltca"))
            .setNonce(manager.nonce());

      } catch (EntityManagerException e) {
        throw new KeyStoreManagerException(e.getMessage());
      }

      try {
        requestBuilder.setCsr(csr.getEncoded());

      } catch (IOException e) {
        e.printStackTrace();
        throw new KeyStoreManagerException(e.getMessage());
      }

      RegisterEntityRequest request = requestBuilder.build();
      SignedRegisterEntityRequest signedRequest =
          SignedRegisterEntityRequest.newBuilder()
              .setMessage(request)
              .setSignature(this, keyPair.getPrivate())
              .build();

      SignedRegisterEntityResponse signedResponse;
      try {
        signedResponse = ltcaClient.registerEntity(signedRequest);

      } catch (LongTermCAClientException e) {
        throw new KeyStoreManagerException(e.getMessage());
      }

      byte[][] chainBytes = signedResponse.getMessage().getCertificateBytesChain();
      Certificate[] certificateChain = certificateChainFromBytes(chainBytes);
      setKeyEntry(verifier, keyPair.getPrivate(), certificateChain);
      setCertificateEntry(verifier, certificateChain[0]);

      managerLog().info("[+] Registered '%s'.", verifier.getName());
    }
  }
}
