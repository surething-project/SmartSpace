/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.location;

import pt.ulisboa.tecnico.surespace.common.domain.Object;
import pt.ulisboa.tecnico.surespace.common.location.exception.LocationException;

public abstract class Location<LocationType extends Location<LocationType>>
    extends Object<LocationType> {
  private static final long serialVersionUID = -5699681588040577899L;

  protected Location() {
    super();
  }

  public abstract String asString();

  public abstract LocationProximity proximityTo(LocationType location) throws LocationException;
}
