package org.ds2os.vsl.sphinx.comModel;

import java.io.Serializable;
import java.util.ArrayList;

import org.ds2os.vsl.sphinx.algorithms.Formulas;
import org.ds2os.vsl.sphinx.model.OperationType;
import org.ds2os.vsl.sphinx.model.StateX;
import org.ds2os.vsl.sphinx.model.ValueType;

/**
 * This describes the communication relationship between the service describes
 * by the CommunicationModel.
 *
 * @author francois
 */
public class ComRelationship implements Serializable {
  static final long serialVersionUID = 84783874254111L;

  /**
   * (Identifying) The destination partner of this communication relationship.
   */
  public final String parentType;
  /**
   * partnerType.
   */
  public final String partnerType;

  /**
   * [0] : the number of partners.
   */
  public BIRCHcluster numberOfPartners;
  /** [0] : the number of flows. */
  public BIRCHcluster numberOfFlows;
  /** [0] : the overall frequency in seconds. */
  public BIRCHcluster overallFrequency;
  /** [0] : the same location. */
  public BIRCHcluster sameLocation;

  /**
   * value type.
   */
  public BIRCHcluster valueType;
  /**
   * operation type.
   */
  public BIRCHcluster operationType;

  /**
   * the period clusters.
   */
  ArrayList<BIRCHcluster> aPeriodClusters = new ArrayList<BIRCHcluster>();

  /**
   * This is the number of descriptors merged to create the communicationModel
   * that contains this comRelationship.
   */
  private long lParentWeight;

  /**
   * This is the number of descriptors merged to create this one
   * comRelationship.
   */
  public long lWeight;

  /**
   * These are to be created by the MinerComDescriptor.
   *
   * @param parentType
   *          parentType
   * @param partnerType
   *          partnerType
   * @param numberOfPartners
   *          numberOfPartners
   * @param numberOfFlows
   *          numberOfFlows
   * @param overalFrequency
   *          in packet / second
   * @param sameLocation
   *          0 or 1
   * @param valueTypes
   *          valueTypes
   * @param operationTypes
   *          operationTypes
   */
  private ComRelationship(final String parentType, final String partnerType,
      final int numberOfPartners, final int numberOfFlows, final double[] overalFrequency,
      final double[] sameLocation, final double[][] valueTypes, final double[][] operationTypes) {
    this.parentType = parentType;
    this.partnerType = partnerType;

    double[][] values0 = { { numberOfPartners } };
    this.numberOfPartners = new BIRCHcluster(1, values0);

    double[][] values1 = { { numberOfFlows } };
    this.numberOfFlows = new BIRCHcluster(1, values1);

    double[][] values2 = { overalFrequency };
    this.overallFrequency = new BIRCHcluster(1, values2);

    double[][] values3 = { sameLocation };
    this.sameLocation = new BIRCHcluster(1, values3);

    this.valueType = new BIRCHcluster(ValueType.dimension, valueTypes);

    this.operationType = new BIRCHcluster(OperationType.DIMENSION, operationTypes);

    /*
     * String a = ""; for(int i = 0; i < ValueType.dimension; i++){ for(int j =
     * 0; j < numberOfFlows; j++){ a += valueTypes[i][j] + " "; } a += "\n"; }
     */
    // System.out.println(a);

    lParentWeight = 1;
    lWeight = 1;
  }

  /*
   * public ComRelationship(String parentType,String partnerType,int
   * numberOfPartners,int numberOfFlows,double[] overalFrequency,double[]
   * sameLocation, double[][] valueTypes,double[][]
   * operationTypes,ArrayList<Cluster> clusterList){
   * this(parentType,partnerType,numberOfPartners,numberOfFlows,overalFrequency,
   * sameLocation, valueTypes,operationTypes);
   *
   * for(Cluster clust : clusterList){ aPeriodClusters.add(new
   * BIRCHcluster(clust)); } }
   */

  /**
   * These are to be created by the MinerComDescriptor.
   *
   * @param parentType
   *          parentType
   * @param partnerType
   *          partnerType
   * @param numberOfPartners
   *          numberOfPartners
   * @param numberOfFlows
   *          numberOfFlows
   * @param overalFrequency
   *          in packet / second
   * @param sameLocation
   *          0 or 1
   * @param valueTypes
   *          valueTypes
   * @param operationTypes
   *          operationTypes
   * @param BIRCHclusterList birch cluster list???
   */
  public ComRelationship(final String parentType, final String partnerType,
      final int numberOfPartners, final int numberOfFlows, final double[] overalFrequency,
      final double[] sameLocation, final double[][] valueTypes, final double[][] operationTypes,
      final ArrayList<BIRCHcluster> BIRCHclusterList) {
    this(parentType, partnerType, numberOfPartners, numberOfFlows, overalFrequency, sameLocation,
        valueTypes, operationTypes);

    String a = "";
    for (BIRCHcluster clust : BIRCHclusterList) {
      aPeriodClusters.add(clust);
      a += "" + String.format(" %.2f", clust.aLS[0] / clust.lN) + ":" + clust.lN + ":"
          + String.format(" %.2f", clust.variance()[0]) + " | ";
    }
    // System.out.println(a);
  }

  /**
   * This merges two existing model and keeps this one as the result.
   *
   * @param otherOne
   *          descriptor
   */
  public void mergeTo(final ComRelationship otherOne) {
    // double[][] values2 = {{0}};
    // this.overalFrequency = new BIRCHcluster(1, values2);

    if (partnerType.contains("lightControler") && parentType.contains("washingService")) {
      // System.out.println("did a merge!");
    }

    numberOfPartners.addTo(otherOne.numberOfPartners);
    numberOfFlows.addTo(otherOne.numberOfFlows);
    overallFrequency.addTo(otherOne.overallFrequency);
    sameLocation.addTo(otherOne.sameLocation);
    valueType.addTo(otherOne.valueType);
    operationType.addTo(otherOne.operationType);

    for (BIRCHcluster clust : otherOne.aPeriodClusters) {
      // if(clust.centroid()[0] > 0.001) //: this is actually not a great
      // work around since
      // there should be no negative cluster...
      this.aPeriodClusters.add(clust);
    }
    mergePeriodClusters();

    lWeight += otherOne.lWeight;
  }

  /**
   * This method goes through all the period clusters, based on the maxdistance.
   */
  private void mergePeriodClusters() {
    // System.out.println("called!!!!");
    double[][] x = { { 0 } };
    ArrayList<BIRCHcluster> aTempClusters = new ArrayList<BIRCHcluster>();

    // test if merge really works!

    String a = "";
    a += "periods: ";
    for (BIRCHcluster clust : aPeriodClusters) {
      a += "" + String.format(" %.0f", clust.aLS[0] / clust.lN) + ":" + clust.lN + ":"
          + clust.dMaxAllowedDist + " | ";
      // a += "" + clust.LS[0] + ":" + clust.N + " | "; / lWeight
    }

    BIRCHcluster pivotCluster;
    ArrayList<BIRCHcluster> aPeriodClustersCP = aPeriodClusters;

    // for(int i = 0; i < aPeriodClusters.size(); i++){
    while (!aPeriodClustersCP.isEmpty()) {
      pivotCluster = aPeriodClustersCP.remove(0);
      // i--;
      ArrayList<BIRCHcluster> aTempLoopClusters = new ArrayList<BIRCHcluster>();
      for (BIRCHcluster clust : aPeriodClustersCP) {
        aTempLoopClusters.add(clust);
      }

      int j = 0;
      // for(int j = 0; j < aPeriodClusters.size(); j++){
      while (!aTempLoopClusters.isEmpty()) {
        BIRCHcluster clust2 = aTempLoopClusters.remove(0);
        // if they are both in the max distance of the other then merge
        if (pivotCluster.aLS[0] / pivotCluster.lN < clust2.aLS[0] / clust2.lN
            + clust2.dMaxAllowedDist
            && pivotCluster.aLS[0] / pivotCluster.lN > clust2.aLS[0] / clust2.lN
                - clust2.dMaxAllowedDist) {
          pivotCluster.addTo(clust2);
          aPeriodClustersCP.remove(clust2);
        }
        if (clust2.aLS[0] / clust2.lN < pivotCluster.aLS[0] / pivotCluster.lN
            + pivotCluster.dMaxAllowedDist
            && clust2.aLS[0] / clust2.lN > pivotCluster.aLS[0] / pivotCluster.lN
                - pivotCluster.dMaxAllowedDist) {
          pivotCluster.addTo(clust2);
          aPeriodClustersCP.remove(clust2);
        }
      }

      aTempClusters.add(pivotCluster);
    }

    aPeriodClusters = aTempClusters;

    // String a = "";
    a += "\nresult: ";
    for (BIRCHcluster clust : aTempClusters) {
      a += "" + String.format(" %.0f", clust.aLS[0] / clust.lN) + ":" + clust.lN + ":"
          + clust.dMaxAllowedDist + " | ";
      // a += "" + clust.LS[0] + ":" + clust.N + " | "; / lWeight
    }

    // if(parentType.equals("/demo/washingService") && aPeriodClusters.size() <
    // 10)
    // System.out.println(a);

  }

  /**
   * This method goes through all the period clusters, based on the maxdistance.
   */
  /*
   * private void mergePeriodClusters(){ ArrayList<BIRCHcluster> aTempClusters =
   * new ArrayList<BIRCHcluster>();
   *
   * boolean somethingChanged = false;
   *
   * for(int i = 0; i < aPeriodClusters.size(); i++){ //BIRCHcluster clust1 :
   * this.aPeriodClusters){ BIRCHcluster clust1 = aPeriodClusters.get(i);
   * aPeriodClusters.remove(i); aTempClusters.add(clust1);
   *
   *
   * for(int j = 0; j < aPeriodClusters.size(); j++){ //BIRCHcluster clust2 :
   * this.aPeriodClusters){ BIRCHcluster clust2 = aPeriodClusters.get(j); // if
   * they are both in the max distance of the other then merge if(clust1.LS[0] <
   * clust2.LS[0] + clust2.dMaxAllowedDist && clust1.LS[0] > clust2.LS[0] -
   * clust2.dMaxAllowedDist){ if(clust2.LS[0] < clust1.LS[0] +
   * clust1.dMaxAllowedDist && clust2.LS[0] > clust1.LS[0] -
   * clust1.dMaxAllowedDist){ clust1.addTo(clust2); aPeriodClusters.remove(j);
   * //aTempClusters.get(aTempClusters.indexOf(clust1)).addTo(clust2); j--;
   * somethingChanged = true; //this.aPeriodClusters.remove(clust2); } } }
   * //if(somethingChanged){ i--; } aPeriodClusters.add(i,clust1);
   * //aTempClusters.add(clust1); } // aPeriodClusters = aTempClusters; //
   * aPeriodClusters.clear(); // for(BIRCHcluster clust : aTempClusters){ //
   * aPeriodClusters.add(clust); // } if(somethingChanged){
   * mergePeriodClusters(); } }
   *
   * aTempClusters.add(new BIRCHcluster(1, x)); aTempClusters.add(new
   * BIRCHcluster(1,x)); System.out.println(aTempClusters.size());
   *
   * for(int i = 0; i < aTempClusters.size(); i++){ aTempClusters.add(new
   * BIRCHcluster(1, x)); System.out.println(aTempClusters.size()); }
   */

  /**
   * set praten weight.
   * 
   * @param lWeight2
   *          lWeight2
   */
  final void setIPratenWeight(final long lWeight2) {
    this.lParentWeight = lWeight2;
  }

  /**
   * getSupportOfRelationship.
   * 
   * @return support
   */
  public double getSupportOfRelationship() {
    long supportOfParent = lParentWeight;
    if (lParentWeight > StateX.maxNumberDescriptor) {
      supportOfParent = StateX.maxNumberDescriptor;
    }

    return ((double) supportOfParent / (double) (lWeight));
  }

  /**
   * getFrequencyAnomaly.
   * 
   * @param timeInterval
   *          timeInterval
   * @return anomalyValue
   */
  public final double getFrequencyAnomaly(final long timeInterval) {
    double anomalyValue = 0.0;
    double anomalyValue2 = 0.0;
    double minDist = -1;
    int index = 0, i = 0;
    try {
      for (BIRCHcluster clust : aPeriodClusters) {
        if (minDist == -1) {
          minDist = Formulas.dist(clust.centroid()[0], timeInterval);
          index = 0;
        } else {
          minDist = Math.min(minDist, Formulas.dist(clust.centroid()[0], timeInterval));
          index = i;
        }
        i++;
      }
      anomalyValue = minDist * ((double) this.lWeight / (double) aPeriodClusters.get(index).lN);
      anomalyValue2 = (minDist / aPeriodClusters.get(index).dMaxAllowedDist)
          * (this.lWeight * 100.0 / aPeriodClusters.get(index).lN);
    } catch (Exception e) {
      // e.printStackTrace();
      // return getFrequencyAnomaly(timeInterval);
      // use semaphore?
    }
    // System.out.printf("%.2f or %.2f for %d with
    // %.2f\n",anomalyValue,anomalyValue2,timeInterval,minDist);

    return anomalyValue2;
  }

  /**
   * getClusterGroupAnomaly.
   * 
   * @param edgeClusters
   *          edgeClusters
   * @return anomalyValue
   */
  public final double getClusterGroupAnomaly(final ArrayList<BIRCHcluster> edgeClusters) {
    double anomalyValue = 0.0;
    double anomalyValue2 = 0.0;
    double minDist = -1;
    double sumMinDist = 0;
    int index = 0, i = 0;

    double sumOfClusterWeights = 0;
    double factorReduceWeight = 0;

    try {
      for (BIRCHcluster clust1 : aPeriodClusters) {
        sumOfClusterWeights += clust1.lN;
      }
      factorReduceWeight = StateX.windowSizeFrequencyAD / sumOfClusterWeights;

      for (BIRCHcluster clust0 : aPeriodClusters) {
        minDist = -1;
        i = 0;
        for (BIRCHcluster clust1 : edgeClusters) {
          if (minDist == -1) {
            minDist = Formulas.dist(clust0.centroid()[0], clust1.centroid()[0]);
            index = 0;
          } else {
            minDist = Math.min(minDist, Formulas.dist(clust0.centroid()[0], clust1.centroid()[0]));
            index = i;
          }
          i++;
        }
        if (minDist < clust0.dMaxAllowedDist * 3
            || minDist < edgeClusters.get(index).dMaxAllowedDist * 3) {

        } else {
          minDist *= (this.lWeight * 100.0 / clust0.lN * factorReduceWeight)
              * (edgeClusters.get(index).lN);
          sumMinDist += minDist / (clust0.dMaxAllowedDist);
        }
      }

      // anomalyValue = minDist * ((double)this.lWeight /
      // (double)aPeriodClusters.get(index).N);
      // anomalyValue2 = (minDist / aPeriodClusters.get(index).dMaxAllowedDist)
      // *
      // ((double)this.lWeight * 100.0 / (double)aPeriodClusters.get(index).N);
    } catch (Exception e) {

      // use semaphore?
    }
    // System.out.printf("%.2f or %.2f for %d with
    // %.2f\n",anomalyValue,anomalyValue2,timeInterval,minDist);

    anomalyValue2 = sumMinDist;

    try {
      if (anomalyValue2 > 1000 || StateX.actualTimestamp == 1520040030050L
          || StateX.actualTimestamp == 1520040032040L) {
        // StateX.failedcontrol++;
        String a = "";
        for (BIRCHcluster clust : aPeriodClusters) {
          a += "" + String.format(" %.2f", clust.aLS[0] / clust.lN) + ":" + clust.lN + ":"
              + String.format(" %.2f", clust.dMaxAllowedDist) + " | ";
        }
        System.out.println("Model: " + a);
        a = "";
        for (BIRCHcluster clust : edgeClusters) {
          a += "" + String.format(" %.2f", clust.aLS[0] / clust.lN) + ":" + clust.lN + ":"
              + String.format(" %.2f", clust.dMaxAllowedDist) + " | ";
        }
        System.out.println("Descriptor: " + a);
      }
    } catch (Exception e) {

      // use semaphore?
    }
    // System.out.println(anomalyValue2);

    return anomalyValue2;
  }

  @Override
  public final String toString() {
    String a = "ComRelationship with " + partnerType + " " + numberOfFlows.lN + " : ";

    a += "numberOfPartners: " + String.format(" %.2f", numberOfPartners.aLS[0] / lParentWeight)
        + "\t"; // / numberOfFlows.N
    a += "numberOfFlows: " + String.format(" %.2f", numberOfFlows.aLS[0] / lWeight) + "\t";
    // / (numberOfPartners.LS[0] / numberOfFlows.N)

    a += "overalFrequencies: ";
    for (int i = 0; i < overallFrequency.aLS.length; i++) {
      a += "" + String.format(" %.2f", overallFrequency.aLS[i] / lWeight) + " ";
      // a += "" + String.format(" %.2f", overalFrequency.SS[i] / lWeight) + "
      // ";
    }
    // a += "sameLocation: ";
    for (int i = 0; i < sameLocation.aLS.length; i++) {
      // a += "" + String.format(" %.2f", sameLocation.LS[i] / lWeight) + " ";
    }

    // a += "valueType: "; //: devide by the number of flows? -> yes and
    // makes sence
    for (int i = 0; i < valueType.aLS.length; i++) {
      // a += "" + String.format(" %.2f", valueType.LS[i] / numberOfFlows.LS[0])
      // + " ";
    }

    // a += "operationType: ";
    for (int i = 0; i < operationType.aLS.length; i++) {
      // a += "" + String.format(" %.2f", operationType.LS[i] /
      // numberOfFlows.LS[0]) + " ";
    }

    a += "periods: ";
    for (BIRCHcluster clust : aPeriodClusters) {
      // a += "" + String.format(" %.2f", clust.LS[0] / clust.N) + ":" +
      // String.format(" %.2f",
      // clust.LS[1] / lWeight) + ":" + String.format(" %.2f",
      // clust.dMaxAllowedDist) + " | ";
      a += "" + String.format(" %.2f", clust.aLS[0] / clust.lN) + ":" + clust.lN + ":"
          + String.format(" %.2f", clust.dMaxAllowedDist) + " | ";
      // a += "" + clust.LS[0] + ":" + clust.N + " | "; / lWeight+ ":" +
      // clust.dMaxAllowedDist
    }

    a += "\n";
    return a;
  }

  /**
   * getPartnerType.
   * 
   * @return partner type
   */
  public final String getPartnerType() {
    return partnerType;
  }

}
