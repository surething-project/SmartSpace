/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.view;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.common.proof.Beacon;
import pt.ulisboa.tecnico.surespace.common.proof.Witness;
import pt.ulisboa.tecnico.surespace.common.signal.property.Amplitude;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.AbstractNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;

import static java.time.Instant.now;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.Property.VALUE;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SmartDevice.IS_ON;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SoundAdaptationService.WITNESS;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SoundAdaptationService.Witness.AMPLITUDE;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SoundAdaptationService.Witness.AMPLITUDE_SAMPLING_RATE;

public final class SoundAdaptationServiceView extends AdaptationServiceView {
  private final Amplitude amplitude = new Amplitude();
  private final Witness witness;
  private final RegularNode witnessAmplitudeSamplingRateValue;
  private final RegularNode witnessAmplitudeValue;
  private final RegularNode witnessIsOn;

  public SoundAdaptationServiceView(RegularNode node) {
    super(node, new Beacon("sound"));
    this.witness = new Witness(beacon);

    final RegularNode witnessNode = service.child(WITNESS);
    witnessIsOn = witnessNode.child(IS_ON);

    final RegularNode witnessAmplitude = witnessNode.child(AMPLITUDE);
    witnessAmplitudeValue = witnessAmplitude.child(VALUE);

    final RegularNode witnessAmplitudeSamplingRate = witnessNode.child(AMPLITUDE_SAMPLING_RATE);
    witnessAmplitudeSamplingRateValue = witnessAmplitudeSamplingRate.child(VALUE);
  }

  @Override
  public String getDescriptor() {
    return "sound";
  }

  private void handleWitnessAmplitudeSamplingRateValueChange(AbstractNode<?> node) {}

  private void handleWitnessAmplitudeValueChange(AbstractNode<?> node) {
    try {
      serviceAddReading(witness, amplitude, now().toEpochMilli(), node.getValue().asString());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void serviceBeforeStart() throws VslException {
    super.serviceBeforeStart();
    witnessAmplitudeValue.subscribe(this::handleWitnessAmplitudeValueChange);
    witnessAmplitudeSamplingRateValue.subscribe(
        this::handleWitnessAmplitudeSamplingRateValueChange);
  }

  @Override
  protected void serviceBeforeStop() throws VslException {
    witnessAmplitudeSamplingRateValue.unsubscribe();
    witnessAmplitudeValue.unsubscribe();
    super.serviceBeforeStop();
  }
}
