package org.ds2os.vsl.sphinx.model;

public enum Normality {

  NORMAL, UNKNOWN, ANOMALOUS, ANOMALOUSA, // access anomaly
  ANOMALOUSF, // frequency anomaly
  ANOMALOUSV, // value anomaly
  ANOMALOUSP;// pattern anomaly

  public static boolean isAnomalous(Normality n) {
    if (n != NORMAL && n != UNKNOWN) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isAnomalous() {
    if (this != NORMAL && this != UNKNOWN) {
      return true;
    } else {
      return false;
    }
  }

  public static Normality parse(String operation) {
    if (operation.equals("normal")) {
      return NORMAL;
    }
    if (operation.equals("unknown")) {
      return UNKNOWN;
    }
    if (operation.equals("none")) {
      return UNKNOWN;
    }
    if (operation.equals("anomalous")) {
      return ANOMALOUS;
    }
    if (operation.equals("anomalousA")) {
      return ANOMALOUSA;
    }
    if (operation.equals("anomalousF")) {
      return ANOMALOUSF;
    }
    if (operation.equals("anomalousV")) {
      return ANOMALOUSV;
    }
    if (operation.equals("anomalousP")) {
      return ANOMALOUSP;
    }

    return UNKNOWN;

  }

  @Override
  public String toString() {
    switch (this) {
      case NORMAL:
        return "normal";
      case UNKNOWN:
        return "unknown";
      case ANOMALOUS:
        return "anomalous";
      case ANOMALOUSA:
        return "anomalousA";
      case ANOMALOUSF:
        return "anomalousF";
      case ANOMALOUSV:
        return "anomalousV";
      case ANOMALOUSP:
        return "anomalousP";
      default:
        return "unknown";
    }
  }

}
