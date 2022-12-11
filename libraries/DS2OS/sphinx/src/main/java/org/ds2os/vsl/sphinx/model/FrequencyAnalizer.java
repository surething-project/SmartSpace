package org.ds2os.vsl.sphinx.model;

import java.util.ArrayList;

import org.ds2os.vsl.sphinx.algorithms.Cluster;
import org.ds2os.vsl.sphinx.algorithms.CustomKmeans;
import org.ds2os.vsl.sphinx.comModel.BIRCHcluster;
import org.ds2os.vsl.sphinx.datastructures.SlidingWindow;

/**
 * This class is used to save the time stamps and their analysis and so on.
 * 
 * made modular to test different methods
 * 
 * trains a model and use it to detect anomalous time stamps
 * 
 * first try with k-means on inter arrival times, challenge: cluster number
 * 
 * @author francois
 */
public class FrequencyAnalizer {

  private long lastTimestamp = 0;

  private SlidingWindow<Long> slidingWindow = new SlidingWindow<Long>(StateX.windowSizeFrequencyAD);

  private SlidingWindow<Double> anomalyValuesWindow = new SlidingWindow<Double>(6);

  public ArrayList<Cluster> clusterList;
  public ArrayList<BIRCHcluster> BIRCHclusterList;
  double distanceFactor = 2.5;

  int numberOfPointsInCluster;

  public boolean needUpdate = false;

  public EdgeX edge;

  public FrequencyAnalizer(long firstTimestamp, EdgeX edgeName) {
    lastTimestamp = firstTimestamp;
    clusterList = new ArrayList<Cluster>();
    BIRCHclusterList = new ArrayList<BIRCHcluster>();
    this.edge = edgeName;
  }

  /**
   * This method judges if a new time stamp is anomalous or not.
   * 
   * @param timestamp timestamp in some format, maybe unix timestamp??
   * @return true if normal
   */
  public boolean isTimestampNormal(long timestamp) {
    // long timeInterval = timestamp - lastTimestamp;

    if (judgeTimestampClustering(timestamp)) {
      return true;
    }

    return false;
  }

  /**
   * This method creates the model used to classify new time stamps.
   */
  public void createModel() {
    createClusters();
    needUpdate = false;
    int i = 0;
    for (Cluster clut : clusterList) {
      // System.out.println("Center: " + clut.center + " support: " +
      // clut.weight + " maxDist: " +
      // clut.maxDist );//+ "\n" + i
      i++;
    }
  }

  /**
   * This method is used to adapt the window size to accept more or less
   * packets. Should judge automatically if it is needed or not.
   */
  private void addaptWindowSize() {
    slidingWindow.setWindowSize(10);
  }

  private boolean judgeTimestampClustering(long timestamp) {
    long timeInterval = timestamp - lastTimestamp;
    double distToNearestCluster = -1;
    int clusterIndex = -1;
    int i = 0;
    boolean belongsToCluster = false;

    for (Cluster clust : clusterList) {
      double dist = Math.abs(timeInterval - clust.center);
      if (dist < distToNearestCluster || distToNearestCluster < 0) {
        distToNearestCluster = dist;
        clusterIndex = i;
      }
      if (dist < clust.maxDist * distanceFactor) {
        belongsToCluster = true;
        // TODO: addapt cluster max dist?
      }
      i++;
    }
    double relativeDistance = 0;
    if (clusterIndex != -1 && timeInterval > clusterList.get(clusterIndex).center) {
      relativeDistance = timeInterval / clusterList.get(clusterIndex).center;
    } else if (clusterIndex != -1) {
      relativeDistance = clusterList.get(clusterIndex).center / timeInterval;
    }
    // (distToNearestCluster / clusterList.get(clusterIndex).center)
    double anomalyValue = relativeDistance
        * ((double) numberOfPointsInCluster / (double) slidingWindow.size());
    // TODO: normalize the distanceToNearest ?

    // System.out.println("\n anomalyValue: "+anomalyValue);

    double anomalyThreshold = 5; // -> determin in a good way
    boolean conditionForAddmition = anomalyValue < anomalyThreshold;
    // use anomaly value, but should i also use the fact that the points can
    // lose their relevance?
    // what if no point is accepted during a long time, is it a long DoS or just
    // a change that was
    // not accepted?

    // TODO: add condition for the addition? -> yes, DoS flowding
    /*
     * if(conditionForAddmition || clusterIndex == -1 || slidingWindow.getSize()
     * < 30){ slidingWindow.add(timeInterval); needUpdate = true;
     * //System.out.println("added point : " + anomalyValue + " with value : " +
     * timeInterval); }
     */

    slidingWindow.add(timeInterval);
    needUpdate = true;

    anomalyValuesWindow.add(anomalyValue);

    if (anomalyValue > 100) {
      // System.out.println(edgeName + " Warning dos attack!!! " + anomalyValue
      // + " with value : " +
      // timeInterval);
    }

    if (anomalyValue > 100) {
      // System.out.println(anomalyValue + " for: " + timeInterval);
    }

    // This is a small meachanism to check if the last few were anomalous, if so
    // alarm!
    // might not be the best mechanism ever but it is for the demo
    double sum = 0;
    for (Double av : anomalyValuesWindow) {
      sum += av;
    }
    if (sum > 960) {
      // System.out.println("ANOMALY! ANOMALY!");
      new AnomalyReport(edge, Normality.ANOMALOUSF);
    }

    // this is just a try
    belongsToCluster = needUpdate;

    lastTimestamp = timestamp;
    return belongsToCluster;
  }

  private void createClusters() {
    CustomKmeans kmeans = new CustomKmeans(slidingWindow.toArray(new Long[slidingWindow.size()]));

    clusterList = kmeans.getClusters();
    numberOfPointsInCluster = kmeans.getNumberOfPointsInCluster();
    BIRCHclusterList = kmeans.getBIRCHClusters();
    // System.out.println("built clusters!! :)");
  }

  @Override
  public String toString() {

    String toReturn = "Clusters:";
    for (Cluster clust : clusterList) {
      toReturn += String.format(" %.2f", clust.center) + " : " + clust.weight; // +
                                                                               // "\n"
    }

    if (clusterList.isEmpty()) {
      toReturn += slidingWindow.toString();
    }

    /*
     * System.out.println("Clusters:"); for(Cluster clust : clusterList){
     * System.out.println(clust.center + " : " + clust.weight); }
     */

    return toReturn;
  }

  /*
   * Ideas: - update just as much as the inverse of the error coefficient? ->
   * not too much changes from anomalous packets
   * 
   * 
   */

}

/*
 * private void handleTimestamp(Access access){ // for the first try, just begin
 * with the clustering from the analysis and fix a distance threashold for
 * anomalies // this does not handle concept drift, well not really
 * 
 * long timeInterval = access.timestamp - lastTimestamp;
 * 
 * slidingWindow.add(timeInterval);
 * 
 * for(long time : slidingWindow){ System.out.println(time); }
 * 
 * }
 * 
 * System.out.println("center at:" + soleCenter + " 	interval : " +
 * timeInterval);
 * 
 * // detect anomalies if(Math.abs(timeInterval - soleCenter) >
 * acceptedThershold && centerCounter > 10){
 * System.out.println("Frequency anomaly!!!!	: " + Math.abs(timeInterval -
 * soleCenter)); return; }
 * 
 * 
 * // update center soleCenter = (soleCenter * centerCounter + timeInterval) /
 * (centerCounter + 1); centerCounter++;
 */
