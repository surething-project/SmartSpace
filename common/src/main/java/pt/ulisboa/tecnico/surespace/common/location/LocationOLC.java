/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.location;

import com.google.openlocationcode.OpenLocationCode;
import com.google.openlocationcode.OpenLocationCode.CodeArea;
import pt.ulisboa.tecnico.surespace.common.location.exception.LocationException;

import java.util.ArrayList;
import java.util.Objects;

import static com.google.openlocationcode.OpenLocationCode.CODE_PRECISION_NORMAL;
import static org.apache.lucene.util.SloppyMath.haversinMeters;
import static pt.ulisboa.tecnico.surespace.common.location.LocationProximity.ProximityCode.MID;
import static pt.ulisboa.tecnico.surespace.common.location.LocationProximity.ProximityCode.NEAR;

// Open Location Code (OLC)
public final class LocationOLC extends Location<LocationOLC> {
  private static final int CODE_PRECISION = CODE_PRECISION_NORMAL + 1;
  private static final long serialVersionUID = -3710176424010080626L;
  private String code;

  private LocationOLC() {}

  public LocationOLC(double latitude, double longitude) {
    setCode(new OpenLocationCode(latitude, longitude, CODE_PRECISION));
  }

  public LocationOLC(String string) {
    setCode(new OpenLocationCode(string));
  }

  private static OpenLocationCode createOLC(double latitude, double longitude) {
    return new OpenLocationCode(latitude, longitude, CODE_PRECISION);
  }

  private static ArrayList<LocationOLC> getK1Neighbors(LocationOLC locationOLC)
      throws LocationException {
    if (locationOLC == null) throw new NullPointerException("Provided a null location");
    CodeArea area = locationOLC.getOpenLocationCode().decode();

    // Where we are going to store the neighboring codes.
    ArrayList<LocationOLC> neighborCodes = new ArrayList<>();
    // Compute all neighbors.
    for (NeighborCode code : NeighborCode.values()) neighborCodes.add(getNeighbor(area, code));

    return neighborCodes;
  }

  private static ArrayList<LocationOLC> getK2Neighbors(
      LocationOLC locationOLC, ArrayList<LocationOLC> k1Neighbors) throws LocationException {
    if (locationOLC == null || k1Neighbors == null) throw new NullPointerException();
    ArrayList<LocationOLC> k2Neighbors = new ArrayList<>();

    int i = 0;
    NeighborCode[] neighborCodes = NeighborCode.values();
    int numNeighbors = neighborCodes.length;

    for (LocationOLC k1Neighbor : k1Neighbors) {
      CodeArea area = k1Neighbor.getOpenLocationCode().decode();

      if (i % 2 != 0) {
        k2Neighbors.add(getNeighbor(area, neighborCodes[i - 1]));
        k2Neighbors.add(getNeighbor(area, neighborCodes[i]));
        k2Neighbors.add(getNeighbor(area, neighborCodes[(i++ + 1) % numNeighbors]));

      } else k2Neighbors.add(getNeighbor(area, neighborCodes[i++]));
    }

    return k2Neighbors;
  }

  private static LocationOLC getNeighbor(CodeArea area, NeighborCode code)
      throws LocationException {
    if (area == null) throw new NullPointerException("Provided a null area");
    if (code == null) throw new NullPointerException("Provided a null code");

    LocationOLC neighbor = new LocationOLC();
    switch (code) {
      case N:
        neighbor.code =
            createOLC(
                    area.getCenterLatitude() + area.getLatitudeHeight(), area.getCenterLongitude())
                .getCode();
        break;

      case NE:
        neighbor.code =
            createOLC(
                    area.getCenterLatitude() + area.getLatitudeHeight(),
                    area.getCenterLongitude() + area.getLongitudeWidth())
                .getCode();
        break;

      case E:
        neighbor.code =
            createOLC(
                    area.getCenterLatitude(), area.getCenterLongitude() + area.getLongitudeWidth())
                .getCode();
        break;

      case SE:
        neighbor.code =
            createOLC(
                    area.getCenterLatitude() - area.getLatitudeHeight(),
                    area.getCenterLongitude() + area.getLongitudeWidth())
                .getCode();
        break;

      case S:
        neighbor.code =
            createOLC(
                    area.getCenterLatitude() - area.getLatitudeHeight(), area.getCenterLongitude())
                .getCode();
        break;

      case SW:
        neighbor.code =
            createOLC(
                    area.getCenterLatitude() - area.getLatitudeHeight(),
                    area.getCenterLongitude() - area.getLongitudeWidth())
                .getCode();
        break;

      case W:
        neighbor.code =
            createOLC(
                    area.getCenterLatitude(), area.getCenterLongitude() - area.getLongitudeWidth())
                .getCode();
        break;

      case NW:
        neighbor.code =
            createOLC(
                    area.getCenterLatitude() + area.getLatitudeHeight(),
                    area.getCenterLongitude() - area.getLongitudeWidth())
                .getCode();
        break;

      default:
        throw new LocationException("Unrecognized neighbor code");
    }

    return neighbor;
  }

  @Override
  public String asString() {
    return code;
  }

  @Override
  public LocationOLC clone() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LocationOLC)) return false;
    LocationOLC that = (LocationOLC) o;
    return code.equals(that.code);
  }

  private OpenLocationCode getOpenLocationCode() {
    return new OpenLocationCode(code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  @Override
  public LocationProximity proximityTo(LocationOLC location) throws LocationException {
    if (location == null) throw new NullPointerException();
    LocationProximity stLocationProximity = new LocationProximity();

    // Two OLC locations are close if they are equal or a subarea of the other.
    if (this.equals(location) || this.code.contains(location.code)) {
      stLocationProximity.setCode(NEAR);
      stLocationProximity.setConfidence(1);
      return stLocationProximity;
    }

    // 1-nearest neighbors.
    ArrayList<LocationOLC> k1Neighbors = getK1Neighbors(this);
    if (k1Neighbors.contains(location)) {
      stLocationProximity.setCode(NEAR);
      stLocationProximity.setConfidence(0.5);
      return stLocationProximity;
    }

    // 2-nearest neighbors.
    ArrayList<LocationOLC> k2Neighbors = getK2Neighbors(location, k1Neighbors);
    if (k2Neighbors.contains(location)) {
      stLocationProximity.setCode(MID);
      stLocationProximity.setConfidence(1);
      return stLocationProximity;
    }

    // We already know it's not near. But how far is it? Let's base our answer on the distance, in
    // meters, between the center points of the two areas.
    CodeArea area = getOpenLocationCode().decode();
    CodeArea areaToCompare = location.getOpenLocationCode().decode();
    double distance =
        haversinMeters(
            area.getCenterLatitude(),
            area.getCenterLongitude(),
            areaToCompare.getCenterLatitude(),
            areaToCompare.getCenterLongitude());

    stLocationProximity.setConfidence(1 - Math.exp(-0.4 * distance));
    return stLocationProximity;
  }

  public void setCode(OpenLocationCode code) {
    if (code == null) throw new IllegalArgumentException("Provided a null code");
    if (!code.isFull()) throw new IllegalArgumentException("Provided a non-full code");

    this.code = code.getCode();
  }

  @Override
  public String toString() {
    return "LocationOLC{" + "code='" + code + '\'' + '}';
  }

  private enum NeighborCode {
    N,
    NE,
    E,
    SE,
    S,
    SW,
    W,
    NW
  }
}
