/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.location;

public interface LocationConverter<LocationType extends Location<?>> {
  LocationType convert(LocationOLC location);

  LocationType convert(LocationGPS location);
}
