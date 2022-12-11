/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.location;

import java.text.DecimalFormat;
import java.util.Objects;

import static org.apache.lucene.util.SloppyMath.haversinMeters;
import static pt.ulisboa.tecnico.surespace.common.location.LocationProximity.ProximityCode.*;

// Global Positioning System (GPS)
public final class LocationGPS extends Location<LocationGPS> {
  private static final long serialVersionUID = -3558474679173658534L;
  private double latitude = .0;
  private double longitude = .0;
  private double threshold = 0;

  private LocationGPS() {}

  public LocationGPS(double latitude, double longitude, double threshold) {
    setLatitude(latitude);
    setLongitude(longitude);
    setThreshold(threshold);
  }

  @Override
  public String asString() {
    DecimalFormat decimalFormat = new DecimalFormat();
    decimalFormat.setMaximumFractionDigits(6);

    return String.format("%s, %s", decimalFormat.format(latitude), decimalFormat.format(longitude));
  }

  @Override
  public LocationGPS clone() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LocationGPS)) return false;
    LocationGPS that = (LocationGPS) o;
    return Double.compare(that.latitude, latitude) == 0
        && Double.compare(that.longitude, longitude) == 0
        && Double.compare(that.threshold, threshold) == 0;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    if (latitude < -90 || latitude > 90)
      throw new IllegalArgumentException(
          "Invalid latitude '" + latitude + "': out of range [-90, 90]");

    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    if (longitude < -180 || longitude > 180)
      throw new IllegalArgumentException(
          "Invalid longitude '" + longitude + "': out of range [-180, 180]");

    this.longitude = longitude;
  }

  public double getThreshold() {
    return threshold;
  }

  public void setThreshold(double threshold) {
    if (threshold < 0)
      throw new IllegalArgumentException(
          "Invalid threshold '" + threshold + "': must not be negative");

    this.threshold = threshold;
  }

  @Override
  public int hashCode() {
    return Objects.hash(latitude, longitude, threshold);
  }

  @Override
  public LocationProximity proximityTo(LocationGPS location) {
    double lat1 = getLatitude();
    double long1 = getLongitude();
    double lat2 = location.getLatitude();
    double long2 = location.getLongitude();
    double distance = haversinMeters(lat1, long1, lat2, long2);

    LocationProximity locationProximity = new LocationProximity();
    if (distance < threshold) {
      locationProximity.setCode(NEAR);
      locationProximity.setConfidence(-(1.0 / threshold) * distance + 1);

    } else if (distance < 2 * threshold) {
      locationProximity.setCode(MID);
      locationProximity.setConfidence(-(1.0 / threshold) * (distance - threshold) + 1);

    } else {
      locationProximity.setCode(FAR);
      locationProximity.setConfidence(1.0 / ((distance - 2 * threshold) * 0.05 + 1));
    }

    return locationProximity;
  }

  @Override
  public String toString() {
    return "LocationGPS{"
        + "latitude="
        + latitude
        + ", longitude="
        + longitude
        + ", threshold="
        + threshold
        + '}';
  }
}
