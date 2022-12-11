/*
 * Copyright (C) 2021 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.verifier.matlab;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import pt.ulisboa.tecnico.surespace.verifier.matlab.exception.SignalProcessingException;

import static org.apache.commons.lang3.StringUtils.capitalize;

public final class SignalProcessing {
  private final MatlabProxy engine;

  public SignalProcessing(MatlabProxy engine) {
    this.engine = engine;
  }

  private double getAudioSimilarity(String proofId, double proofDuration)
      throws SignalProcessingException {
    double[] results = process(2, "sound", proofId, proofDuration);
    return 0.661 * results[0] + 0.339 * results[1];
  }

  private double getLightSimilarity(String proofId, double proofDuration)
      throws SignalProcessingException {
    return process(1, "light", proofId, proofDuration)[0];
  }

  private double getSimilarity(String proofId, double proofDuration)
      throws SignalProcessingException {
    return 0.436 * getLightSimilarity(proofId, proofDuration)
        + 0.564 * getAudioSimilarity(proofId, proofDuration);
  }

  private double[] process(int numArgs, String beacon, String proofId, double proofDuration)
      throws SignalProcessingException {
    String fnName = "process" + capitalize(beacon);
    double[] result = new double[numArgs];

    try {
      // Execute function on MATLAB.
      Object[] results = engine.returningFeval(fnName, numArgs, proofId, proofId, proofDuration);

      // Store the result as a double.
      for (int i = 0; i < numArgs; i++) result[i] = ((double[]) results[i])[0];

    } catch (MatlabInvocationException e) {
      e.printStackTrace();
      throw new SignalProcessingException(e.getMessage());
    }

    return result;
  }

  public boolean proofAccepted(String proofId, double proofDuration)
      throws SignalProcessingException {
    return getSimilarity(proofId, proofDuration) >= 0.84;
  }
}
