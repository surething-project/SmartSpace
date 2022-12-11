package org.ds2os.vsl.sphinx.algorithms;

/**
 * Some formulas used everywhere.
 * 
 * @author francois
 */
public class Formulas {

  /**
   * Gives the distance.
   * 
   * @param point1
   *          first point
   * @param point2
   *          second point
   * @return the dist
   */
  public static double dist(final double point1, final double point2) {
    double dist = Math.abs(point1 - point2);
    return dist;
  }

  /**
   * A costum logarithm.
   *
   * @param number
   *          the number to use
   * @return the result
   */
  public static double myLog(final double number) {
    return logb(number, 1.2);
  }

  /**
   * A costum logarithm you give the base.
   * 
   * @param number
   *          the number to use
   * @param base
   *          the base to use
   * @return the result
   */
  public static double logb(final double number, final double base) {
    return Math.log(number) / Math.log(base);
  }

}
