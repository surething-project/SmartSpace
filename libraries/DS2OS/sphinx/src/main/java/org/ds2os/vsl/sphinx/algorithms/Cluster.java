package org.ds2os.vsl.sphinx.algorithms;

/**
 * This class is used to describe clusters.
 *
 * @author francois
 */
public class Cluster {
  /** The center of the cluster, this is one dimensional clustering here. */
  public double center;
  /** The distance allowed from the center to farthest point belonging to it. */
  public double maxAllowedDist;
  /** The distance from the center to farthest point belonging to it. */
  public double maxDist;
  /** The number of points belonging to this cluster. */
  public int weight;

  /**
   * The constructor.
   * 
   * @param clusterMid
   *          param1
   * @param maxDist
   *          param2
   * @param weight
   *          param3
   * @param maxAllowedDist
   *          param4
   */
  public Cluster(final double clusterMid, final double maxDist, final int weight,
      final double maxAllowedDist) {
    this.center = clusterMid;
    this.maxDist = maxDist;
    this.weight = weight;
    this.maxAllowedDist = maxAllowedDist;
  }

}
