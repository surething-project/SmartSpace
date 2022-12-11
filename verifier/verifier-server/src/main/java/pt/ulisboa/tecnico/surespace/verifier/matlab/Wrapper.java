/*
 * Copyright (C) 2021 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.matlab;

import matlabcontrol.*;
import org.apache.commons.lang3.tuple.Pair;
import pt.ulisboa.tecnico.surespace.common.proof.Device;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProofProperties;
import pt.ulisboa.tecnico.surespace.common.signal.Fragment;
import pt.ulisboa.tecnico.surespace.common.signal.Signal;
import pt.ulisboa.tecnico.surespace.common.signal.property.Property;
import pt.ulisboa.tecnico.surespace.verifier.matlab.exception.SignalProcessingException;
import pt.ulisboa.tecnico.surespace.verifier.matlab.exception.WrapperException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Wrapper implements AutoCloseable {
  private static final String DATASET_PATH = "matlab/dataset";
  private final MatlabProxy engine;
  private final SignalProcessing signalProcessing;
  private LocationProofView proofView;

  public Wrapper() throws WrapperException {
    try {
      // Start MATLAB engine.
      engine =
          new MatlabProxyFactory(
                  new MatlabProxyFactoryOptions.Builder()
                      .setMatlabStartingDirectory(Paths.get("matlab").toFile())
                      .setUsePreviouslyControlledSession(true)
                      .build())
              .getProxy();

      signalProcessing = new SignalProcessing(engine);

    } catch (MatlabConnectionException e) {
      e.printStackTrace();
      throw new WrapperException("Could not start MATLAB engine.");
    }
  }

  private void assertNonNullLocationProof() throws WrapperException {
    if (proofView == null)
      throw new WrapperException("You must init the wrapper with a non-null location proof first.");
  }

  @Override
  public void close() {
    // Close the proxy.
    engine.disconnect();
  }

  private void createDatasetsDir() throws WrapperException {
    File dir = getFile();
    if (!dir.exists())
      if (!dir.mkdir()) throw new WrapperException("Could not create datasets directory.");
  }

  private void createProofDir() throws WrapperException {
    assertNonNullLocationProof();
    createDatasetsDir();

    File dir = getFile(getLocationProofIdentifier());
    if (!dir.exists())
      if (!dir.mkdir()) throw new WrapperException("Could not create proof directory.");
  }

  private File getFile(String proofId, Pair<Device, Property> key, boolean trusted) {
    return getFile(
        proofId,
        (trusted ? "trusted" : "untrusted")
            + "_"
            + key.getLeft().getTypedIdentifier()
            + "_"
            + key.getRight().getIdentifier()
            + ".csv");
  }

  private File getFile(String... more) {
    return Paths.get(DATASET_PATH, more).toFile();
  }

  private double getLocationProofDuration() {
    LocationProofProperties properties = proofView.getAuthorization().getMessage().getProperties();
    return (properties.getFragmentCount() * properties.getFragmentLength()) / 1000.0;
  }

  private String getLocationProofIdentifier() {
    return proofView.getAuthorization().getMessage().getProperties().getIdentifier();
  }

  public void locationProofInit(final LocationProofView proofView) throws WrapperException {
    if (proofView == null) throw new WrapperException("Provided a null location proof.");
    this.proofView = proofView;
  }

  public boolean locationProofVerify() throws SignalProcessingException {
    return signalProcessing.proofAccepted(getLocationProofIdentifier(), getLocationProofDuration());
  }

  public void locationProofWriteToDirectory() throws WrapperException {
    // Create directory for location proof.
    createProofDir();
    String proofId = getLocationProofIdentifier();

    // Write trusted signals to file.
    writeSignalsToFile(proofId, proofView.getTrustedSignals(), true);
    // Write untrusted signals to file.
    writeSignalsToFile(proofId, proofView.getUntrustedSignals(), false);
  }

  private void writeFragmentToFile(String proofId, Fragment fragment, boolean trusted) {
    HashMap<Pair<Device, Property>, LinkedList<Pair<Long, String>>> readings =
        fragment.getReadings();

    for (Pair<Device, Property> key : readings.keySet()) {
      writeToFile(
          getFile(proofId, key, trusted),
          readings.get(key).stream()
                  .map(pair -> pair.getLeft() + "," + pair.getRight())
                  .reduce((s, s2) -> s + "\n" + s2)
                  .orElse("")
              + "\n");
    }
  }

  private void writeSignalToFile(String proofId, Signal signal, boolean trusted) {
    for (Fragment fragment : signal.getFragments()) {
      writeFragmentToFile(proofId, fragment, trusted);
    }
  }

  private void writeSignalsToFile(String proofId, LinkedHashSet<Signal> signals, boolean trusted) {
    for (Signal signal : signals) {
      writeSignalToFile(proofId, signal, trusted);
    }
  }

  private void writeToFile(File file, String content) {
    try (FileOutputStream fos = new FileOutputStream(file, true)) {
      fos.write(content.getBytes(UTF_8));

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
