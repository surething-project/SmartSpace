/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.manager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.manager.EntityManager;
import pt.ulisboa.tecnico.surespace.common.manager.KeyStoreManager;
import pt.ulisboa.tecnico.surespace.common.manager.NonceManager;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.KeyStoreManagerException;
import pt.ulisboa.tecnico.surespace.common.message.MessageValidator;
import pt.ulisboa.tecnico.surespace.common.message.SignedMessageValidator;
import pt.ulisboa.tecnico.surespace.common.message.exception.MessageValidatorException;
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

public final class OrchestratorKeyStoreManager extends KeyStoreManager {
  private final OrchestratorManager manager;

  protected OrchestratorKeyStoreManager(OrchestratorManager manager)
      throws KeyStoreManagerException {
    super(manager.property().get("keystore", "key").asString().toCharArray(), manager.property());

    this.manager = manager;
    beforeLoading();
    afterLoading();
  }

  private EntityManager entityManager() {
    return manager.entity();
  }

  private X500Name getOrchestratorX500Name(Entity orchestrator) {
    return new X500NameBuilder().addRDN(CN, orchestrator.getName()).build();
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

  private OrchestratorLogManager managerLog() {
    return manager.log();
  }

  private OrchestratorPropertyManager managerProperty() {
    return manager.property();
  }

  private NonceManager nonceManager() {
    return manager.nonce();
  }

  public void registerOrchestrator() throws KeyStoreManagerException {
    final Entity orchestrator;
    final Entity ltca;
    try {
      orchestrator = entityManager().current();
      ltca = entityManager().getByPath("surespace://rca/ltca");

    } catch (EntityManagerException e) {
      throw new KeyStoreManagerException(e.getMessage());
    }

    if (!containsKey(orchestrator) || !containsCertificate(orchestrator)) {
      managerLog().warning("[-] Orchestrator '%s' is not registered.", orchestrator.getName());

      // Generate key pair and CSR.
      KeyPair keyPair = generateKeyPair();
      PKCS10CertificationRequest csr = generateCsr(keyPair, getOrchestratorX500Name(orchestrator));

      // Create a client of the LTCA to register the Orchestrator.
      String ltcaHost = managerProperty().get("ltca", "host").asString();
      int ltcaPort = managerProperty().get("ltca", "port").asInt();
      LongTermCAClient ltcaClient = new LongTermCAClient(ltcaHost, ltcaPort);

      RegisterEntityRequestBuilder requestBuilder =
          RegisterEntityRequest.newBuilder()
              .setSender(orchestrator)
              .setReceiver(ltca)
              .setNonce(nonceManager());

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

      // Validate the response.
      try {
        new MessageValidator(manager)
            .init(signedResponse.getMessage())
            .assertReceiver(orchestrator)
            .assertSenderKnown()
            .assertSender(ltca)
            .assertCertificateValid()
            .assertNonceValid()
            .validate();

        new SignedMessageValidator(manager).assertSignature().validate();

      } catch (MessageValidatorException e) {
        throw new KeyStoreManagerException(e.getMessage());
      }

      byte[][] chainBytes = signedResponse.getMessage().getCertificateBytesChain();
      Certificate[] certificateChain = certificateChainFromBytes(chainBytes);
      Certificate certificate = certificateChain[0];

      try {
        // Validate the certificate.
        if (!isValidCertificate(certificate))
          throw new KeyStoreManagerException("Invalid certificate received");

        setKeyEntry(orchestrator, keyPair.getPrivate(), certificateChain);
        setCertificateEntry(orchestrator, certificate);

      } catch (Exception e) {
        removeKeyEntry(orchestrator);
        removeCertificateEntry(orchestrator);
        throw new KeyStoreManagerException("Could not store certificate and/or private key");
      }

      managerLog().info("[+] Registered '%s'.", orchestrator.getName());
    }
  }
}
