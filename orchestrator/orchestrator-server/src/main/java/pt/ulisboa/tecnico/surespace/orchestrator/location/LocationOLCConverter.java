/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.orchestrator.location;

import pt.ulisboa.tecnico.surespace.common.location.LocationConverter;
import pt.ulisboa.tecnico.surespace.common.location.LocationGPS;
import pt.ulisboa.tecnico.surespace.common.location.LocationOLC;

public final class LocationOLCConverter implements LocationConverter<LocationOLC> {
  @Override
  public LocationOLC convert(LocationOLC location) {
    return location;
  }

  @Override
  public LocationOLC convert(LocationGPS location) {
    return new LocationOLC(location.getLatitude(), location.getLongitude());
  }
}
