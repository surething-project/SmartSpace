package org.ds2os.vsl.sphinx.algorithms;

import java.util.ArrayList;
import java.util.Random;

import org.ds2os.vsl.sphinx.comModel.BIRCHcluster;

/**
 * The goal of this k means algorithm is to determine on its own the number of
 * clusters needed and deliver them as output.
 *
 * @author francois
 */
public class CustomKmeans {

  /**
   * This is the array containing the data set.
   */
  private Long[] dataset;

  /** This contains the clusterLabels. */
  private int[] clusterLabel;
  /** This contains the normalized dataset. */
  private double[] normalizedDataset;

  /** This list contains all the discovered clusters. */
  private ArrayList<Cluster> clusterList = new ArrayList<Cluster>();
  /**
   * This list contains all the discovered clusters in the form of BIRCH
   * clusters.
   */
  private ArrayList<BIRCHcluster> lBIRCHclusterList = new ArrayList<BIRCHcluster>();

  // private int numberOfClusters = 10;
  /** The maximum in the array. */
  long max = 0;
  /** The minimum in the array. */
  long min = 10000;

  /** Contains the total number of points belonging to a cluster. */
  private int numberOfPointsInClusters = 0;

  /** The size of the data. */
  private int dataSize;
  // we start with 10 clusters, could pick more, the goal is to have too much
  // and then to delete
  // some

  // density based clustering:
  /** */
  private double stepSize = 0.01;
  /** */
  private double densityTeta = 4; // 6% of the points in one cell of the grid
  /** */
  private int densityThreshold;
  /** */
  private int[] grid = new int[(int) (1 / stepSize)];

  /** */
  private int minimumMaxAllowedDist = 50;
  /** */
  private int minimumWeight = 2; // could be more, or be adjustable

  /**
   * a k-means clustering.
   * 
   * @param elementData
   *          the input data
   */
  public CustomKmeans(final Long[] elementData) {
    dataSize = elementData.length + 1;
    dataset = new Long[dataSize];
    double sum = 0;
    for (int i = 0; i < elementData.length; i++) {
      dataset[i] = elementData[i];
      sum += elementData[i];
    }
    // + 10000; this should/could be a percentage of the sum
    dataset[elementData.length] = (long) (sum / elementData.length);

    densityThreshold = (int) (dataSize * densityTeta * stepSize);

    clusterLabel = new int[dataSize];
    normalizedDataset = new double[dataSize];

    for (int i = 0; i < grid.length; i++) {
      grid[i] = 0;
    }

    densityBasedClusterPlacing();
    // initiateClusters(2);
    attributeToCluster();
    updateClusterPos();
    mergeClusters();
  }

  /**
   * Get clusters.
   * 
   * @return Clusters
   */
  public final ArrayList<Cluster> getClusters() {
    return clusterList;
  }

  /**
   * Get cluster in BIRCH form.
   * 
   * @return the clusters
   */
  public final ArrayList<BIRCHcluster> getBIRCHClusters() {
    return lBIRCHclusterList;
  }

  /**
   * Get the number the number of points.
   * 
   * @return number of points
   */
  public final int getNumberOfPointsInCluster() {
    return numberOfPointsInClusters;
  }

  /**
   * Finds the nearest cluster for each data point.
   */
  private void attributeToCluster() {
    numberOfPointsInClusters = 0;

    for (int i = 0; i < dataSize; i++) {
      double minDist = -1;
      int clusterIndex = -1;
      for (int j = 0; j < clusterList.size(); j++) {
        double dist = Math.abs(dataset[i] - clusterList.get(j).center);
        if ((minDist == -1 || minDist > dist) && dist < clusterList.get(j).maxAllowedDist) {
          minDist = dist;
          clusterIndex = j;
        }
      }
      // System.out.println("Data point:" + dataset[i] + " cluster: " +
      // clusterIndex);
      clusterLabel[i] = clusterIndex;
      numberOfPointsInClusters++;
    }
    // System.out.println(clusterList.get(0).maxAllowedDist);
  }

  /**
   * Update the cluster position.
   */
  private void updateClusterPos() {
    lBIRCHclusterList.clear();

    for (int j = 0; j < clusterList.size(); j++) {
      double maxDist = -1;
      clusterList.get(j).center = 0;
      clusterList.get(j).weight = 0;

      ArrayList<Double> pointsForBIRCH = new ArrayList<Double>();

      for (int i = 0; i < dataSize; i++) {
        if (clusterLabel[i] == j) {
          clusterList.get(j).center += dataset[i];
          clusterList.get(j).weight++;
          pointsForBIRCH.add((double) dataset[i]);
        }
      }

      clusterList.get(j).center = clusterList.get(j).center / clusterList.get(j).weight;

      for (int i = 0; i < dataSize; i++) {
        if (clusterLabel[i] == j) {
          if (Math.abs(dataset[i] - clusterList.get(j).center) > maxDist) {
            maxDist = Math.abs(dataset[i] - clusterList.get(j).center);
          }
        }
      }
      clusterList.get(j).maxDist = (long) maxDist;

      double[][] dataPoints = new double[1][pointsForBIRCH.size()];
      int k = 0;
      for (double point : pointsForBIRCH) {
        dataPoints[0][k++] = point;
      }

      BIRCHcluster tempClusterBIRCH = new BIRCHcluster(1, dataPoints);
      if (tempClusterBIRCH.centroid()[0] > 0.001) { // && dataset.length >
                                                    // ((double)StateX.windowSizeFrequencyAD
                                                    // * 0.8)){
                                                    // //){//
        lBIRCHclusterList.add(tempClusterBIRCH);
      }
      // BIRCHclusterList.add(tempClusterBIRCH);
    }

    for (int j = 0; j < clusterList.size(); j++) {
      if (clusterList.get(j).weight <= minimumWeight) {
        // System.out.println("removed: " + j + " ,weight was: " +
        // clusterList.get(j).weight);
        clusterList.remove(j);
        lBIRCHclusterList.remove(j);
        j--;
      }
    }

  }

  /**
   * Merge the clusters.
   */
  private void mergeClusters() {
    for (int j = 0; j < clusterList.size(); j++) {
      if (j + 1 < clusterList.size()) {
        if (Math.abs(clusterList.get(j).center - clusterList.get(j + 1).center) < 5) {
          clusterList.get(j).center = clusterList.get(j).center * clusterList.get(j).weight
              + clusterList.get(j + 1).center * clusterList.get(j + 1).weight;
          clusterList.get(j).center /= clusterList.get(j).weight + clusterList.get(j + 1).weight;
          clusterList.get(j).weight += clusterList.get(j + 1).weight;
          clusterList.remove(j + 1);
          j--;
        }

        // System.out.println("removed: " + j + " ,weight was: " +
        // clusterList.get(j).weight);
      }
    }
  }

  /**
   * Does the clustering.
   */
  private void densityBasedClusterPlacing() {
    normalize();
    boolean headOnCluster = false;
    int indexStartCluster = 0;

    for (double point : normalizedDataset) {
      if ((int) (point / stepSize) < grid.length) {
        grid[(int) (point / stepSize)]++;
      } else {
        grid[(int) (point / stepSize) - 1]++;
      }
    }

    // printGrid();

    // System.out.println("densityThreshold: " + densityThreshold + " data
    // size:" + dataSize);

    for (int i = 0; i < grid.length; i++) {
      if (grid[i] > densityThreshold) {
        if (!headOnCluster) {
          headOnCluster = true;
          indexStartCluster = i;
        }
        // System.out.println(indexStartCluster+ (i - indexStartCluster)/2*
        // stepSize* (double) (max
        // - min) + min);
        if (i == grid.length - 1) {
          // System.out.println("here");
          double clusterMid = (indexStartCluster + (i - indexStartCluster) / 2) * stepSize
              * (max - min) + min;
          clusterList.add(new Cluster(clusterMid, 0, 0,
              (long) ((i - indexStartCluster) / stepSize) + minimumMaxAllowedDist));
          // System.out.println("added center: "+ clusterMid);
        }
      } else {
        if (headOnCluster) {
          double clusterMid = (indexStartCluster + (i - indexStartCluster) / 2) * stepSize
              * (max - min) + min;
          clusterList.add(new Cluster(clusterMid, 0, 0,
              (long) ((i - indexStartCluster) / stepSize) + minimumMaxAllowedDist));
          /*
           * int index = indexStartCluster + (i - indexStartCluster)/2;
           * System.out.println(index); if(index < dataset.length){
           * clusterList.add(new Cluster(dataset[index],0,0)); } else {
           * clusterList.add(new Cluster(dataset[index - 1],0,0)); }
           */
          headOnCluster = false;
        }
      }
    }

  }

  /**
   * normalize the input data.
   */
  private void normalize() {
    for (long point : dataset) {
      if (point > max) {
        max = point;
      }
      if (point < min) {
        min = point;
      }
    }
    for (int i = 0; i < dataSize; i++) {
      normalizedDataset[i] = (double) (dataset[i] - min) / (double) (max - min);
      // System.out.println("original: " + dataset[i] + " -> " +
      // normalizedDataset[i]);
    }

  }

  /**
   * print the created grid.
   */
  private void printGrid() {
    for (int i = 0; i < grid.length; i++) {
      System.out.println("index : " + i + " - " + grid[i]);
    }
  }

  /**
   * Initianlize the clusters.
   * 
   * @param numberOfClusters
   *          the number of clusters
   */
  private void initiateClusters(final int numberOfClusters) {
    Random rn = new Random();
    for (int i = 0; i < numberOfClusters; i++) {
      int index = Math.abs(rn.nextInt() % dataSize);
      clusterList.add(new Cluster(dataset[index], 0, 0, -1));
    }
  }

}
