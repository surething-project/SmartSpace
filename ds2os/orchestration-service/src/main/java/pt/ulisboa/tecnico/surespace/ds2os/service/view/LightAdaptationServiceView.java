/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.view;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.common.proof.Beacon;
import pt.ulisboa.tecnico.surespace.common.proof.Witness;
import pt.ulisboa.tecnico.surespace.common.signal.property.Intensity;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.AbstractNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.LightAdaptationService;

import static java.time.Instant.now;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.LightAdaptationService.Witness.INTENSITY;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.LightAdaptationService.Witness.INTENSITY_SAMPLING_RATE;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.Property.VALUE;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.SmartDevice.IS_ON;

public final class LightAdaptationServiceView extends AdaptationServiceView {
  private final Intensity intensity = new Intensity();
  private final Witness witness;
  private final RegularNode witnessIntensitySamplingRateValue;
  private final RegularNode witnessIntensityValue;
  private final RegularNode witnessIsOn;

  public LightAdaptationServiceView(RegularNode node) {
    super(node, new Beacon("light"));
    this.witness = new Witness(beacon);

    final RegularNode witnessNode = service.child(LightAdaptationService.WITNESS);
    witnessIsOn = witnessNode.child(IS_ON);

    final RegularNode witnessIntensity = witnessNode.child(INTENSITY);
    witnessIntensityValue = witnessIntensity.child(VALUE);

    final RegularNode witnessIntensitySamplingRate = witnessNode.child(INTENSITY_SAMPLING_RATE);
    witnessIntensitySamplingRateValue = witnessIntensitySamplingRate.child(VALUE);
  }

  @Override
  public String getDescriptor() {
    return "light";
  }

  private void handleWitnessIntensityValueChange(AbstractNode<?> node) {
    try {
      serviceAddReading(witness, intensity, now().toEpochMilli(), node.getValue().asString());

    } catch (VslException e) {
      LOGGER.error("Light: could not retrieve intensity value.");
    }
  }

  @Override
  protected void serviceBeforeStart() throws VslException {
    super.serviceBeforeStart();
    witnessIntensityValue.subscribe(this::handleWitnessIntensityValueChange);
  }

  @Override
  protected void serviceBeforeStop() throws VslException {
    witnessIntensityValue.unsubscribe();
    super.serviceBeforeStop();
  }
}
