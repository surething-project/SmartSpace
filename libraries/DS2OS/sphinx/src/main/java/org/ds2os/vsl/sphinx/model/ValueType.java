package org.ds2os.vsl.sphinx.model;

import java.util.ArrayList;

/**
 * This enum defines which type of value can be send via the network.
 *
 *
 * @author francois
 */
public enum ValueType {

  /**
   * the things.
   */
  NUMBER, TEXT, LIST, COMPOSED, OTHER;

  /** */
  private static final double[] numberBinary = { 1.0, 0.0, 0.0, 0.0, 0.0 };
  /** */
  private static final double[] textBinary = { 0.0, 1.0, 0.0, 0.0, 0.0 };
  /** */
  private static final double[] listBinary = { 0.0, 0.0, 1.0, 0.0, 0.0 };
  /** */
  private static final double[] composedBinary = { 0.0, 0.0, 0.0, 1.0, 0.0 };
  /** */
  private static final double[] otherBinary = { 0.0, 0.0, 0.0, 0.0, 1.0 };

  /** */
  public static final int dimension = 5;

  /**
   * parse.
   * @param type type
   * @return ValueType
   */
  public static ValueType parse(String type) {

    if (type.equals("/basic/number")) {
      return NUMBER;
    }
    if (type.equals("/basic/text")) {
      return TEXT;
    }
    if (type.equals("/basic/list")) {
      return COMPOSED;
    }
    if (type.equals("/basic/composed")) {
      return COMPOSED;
    }

    return OTHER;

  }

  /**
   * toString.
   * @param type type
   * @return String
   */
  public static String toString(final ValueType type) {
    switch (type) {
      case NUMBER:
        return "/basic/number";
      case TEXT:
        return "/basic/text";
      case COMPOSED:
        return "/basic/composed";
      case LIST:
        return "/basic/list";
      default:
        // return OTHER;
        return "other";
    }
  }

  /**
   * binarization.
   * @return double[]
   */
  public double[] binarization() {
    switch (this) {
      case NUMBER:
        return numberBinary;
      case TEXT:
        return textBinary;
      case COMPOSED:
        return composedBinary;
      case LIST:
        return listBinary;
      default:
        // return OTHER;
        return otherBinary;
    }
  }

  /**
   * reverseBinarization.
   * @param binary binary
   * @return types
   */
  public static ArrayList<ValueType> reverseBinarization(final double binary) {
    ArrayList<ValueType> types = new ArrayList<ValueType>();
    return types;
  }

}
