package org.ds2os.vsl.sphinx.comModel;

import java.io.Serializable;

import org.ds2os.vsl.sphinx.algorithms.Cluster;
import org.ds2os.vsl.sphinx.algorithms.Formulas;

/**
 * BIRCH lcuster description.
 *
 * @author francois
 */
public class BIRCHcluster implements Serializable {
  static final long serialVersionUID = 847836384541L;

  /** number of dimension. */
  private int dimension;
  /** number of points in cluster. */
  long lN;
  /** values. */
  double[] aLS;
  /** squared values. */
  double[] aSS;

  /** Is created from a cluster. */
  boolean bIsFormClusetr = true; // not normal
  /** Max distance to center. */
  double dMaxAllowedDist;

  /**
   *
   * @param dimension
   *          the number of dimensions
   * @param values
   *          the first dimension (values[i][]) is for the dimensions and the
   *          second (values[][j]) is for the different values
   */
  public BIRCHcluster(final int dimension, final double[][] values) {
    this.dimension = dimension;
    lN = values[0].length; // / dimension;
    aLS = new double[dimension];
    aSS = new double[dimension];

    String a = "";
    for (int i = 0; i < dimension; i++) {
      double sum = 0;
      double sumSq = 0;

      for (int j = 0; j < lN; j++) {
        a += values[i][j] + " ";
        sum += values[i][j];
        sumSq += (values[i][j] * values[i][j]);
      }

      aLS[i] = sum;
      aSS[i] = sumSq;
      a += "\t" + sum;
      a += "\n";
    }
    /*
     * if(dimension == ValueType.dimension){ System.out.println(a); }
     */
    this.dMaxAllowedDist = variance()[0] * 3;

    if (lN < 0) {
      System.out.println("problem: " + dimension);
    }
  }

  /**
   * @param periodCluster
   *          the normal cluster.
   */
  public BIRCHcluster(final Cluster periodCluster) {
    this.dimension = 2; // for now this is the way it is.
    lN = 1; // periodCluster.weight; // / dimension;
    aLS = new double[dimension];
    aSS = new double[dimension];

    aLS[0] = periodCluster.center;
    aSS[0] = periodCluster.center * periodCluster.center;
    aLS[1] = periodCluster.weight;
    aSS[1] = periodCluster.weight * periodCluster.weight;

    dMaxAllowedDist = periodCluster.maxAllowedDist;
    // System.out.println(dMaxAllowedDist);
    bIsFormClusetr = true;
  }

  /**
   * Adds another BIRCHcluster to this one.
   *
   * @param otherOne
   *          the other cluster to be merged
   */
  public void addTo(final BIRCHcluster otherOne) {
    if (this.dimension != otherOne.dimension) {
      throw new IllegalArgumentException();
    }

    this.lN = this.lN + otherOne.lN;

    for (int i = 0; i < this.dimension; i++) {
      this.aLS[i] = this.aLS[i] + otherOne.aLS[i];
      this.aSS[i] += otherOne.aSS[i];
    }

    this.dMaxAllowedDist = variance()[0] * 3;
    if (bIsFormClusetr) {
      // this.dMaxAllowedDist = Math.max(otherOne.dMaxAllowedDist,
      // this.dMaxAllowedDist);
      // this the good way?
      this.dMaxAllowedDist = variance()[0] * 3;
    }
  }

  /**
   * Returns the center of the cluster.
   *
   * @return the centers for each dimentions
   */
  public double[] centroid() {
    double[] centroids = new double[dimension];

    for (int i = 0; i < dimension; i++) {
      centroids[i] = aLS[i] / lN;
    }

    return centroids;
  }

  /**
   * @return the variance for each dimention.
   */
  public final double[] variance() {
    double[] variances = new double[dimension];
    double[] centroids = this.centroid();

    for (int i = 0; i < dimension; i++) {
      // System.out.println(N * centroids[i] * centroids[i] + " , " + SS[i] + "
      // , " + 2 *
      // centroids[i] * LS[i] + " , " + N + " , "
      // + (N * centroids[i] * centroids[i] + SS[i] - 2 * centroids[i] *
      // LS[i]));
      variances[i] = Math
          .sqrt((lN * centroids[i] * centroids[i] + aSS[i] - 2 * centroids[i] * aLS[i]) / (lN));
      // System.out.println(variances[i]);
      if (variances[i] < 0.01) {
        // variances[i] = 0.01;
        variances[i] = 0.001; // (1.0 / Formulas.myLog(N+1));
                              // //Formulas.myLog((double)(1.0 /
                              // (double)(N+1)));
      }
    }
    return variances;
  }

  /**
   * @return the variance for each dimention.
   */
  public final double[] variancePrint() {
    double[] variances = new double[dimension];
    double[] centroids = this.centroid();

    for (int i = 0; i < dimension; i++) {
      System.out.println(lN * centroids[i] * centroids[i] + " , " + aSS[i] + " , "
          + 2 * centroids[i] * aLS[i] + " , " + lN + " , "
          + (lN * centroids[i] * centroids[i] + aSS[i] - 2 * centroids[i] * aLS[i]));
      variances[i] = Math
          .sqrt((lN * centroids[i] * centroids[i] + aSS[i] - 2 * centroids[i] * aLS[i]) / (lN));
      // System.out.println(variances[i]);
      if (variances[i] < 0.01) {
        // variances[i] = 0.01;
        variances[i] = (1.0 / Formulas.myLog(lN + 1)); // Formulas.myLog((double)(1.0
                                                       // /
                                                       // (double)(N+1)));
      }
    }
    return variances;
  }

  /**
   * @return number of points in cluster.
   */
  public final long getNumber() {
    return lN;
  }

}
