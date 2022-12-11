/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.view;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;

import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.LocalizationService.LOCATION;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.Location.VALUE;

public final class LocalizationServiceView extends ServiceView {
  private final RegularNode location;
  private final RegularNode locationValue;

  public LocalizationServiceView(RegularNode node) {
    super(node);

    location = service.child(LOCATION);
    locationValue = location.child(VALUE);
  }

  public String getLocation() throws VslException {
    return locationValue.getValue().asString();
  }
}
