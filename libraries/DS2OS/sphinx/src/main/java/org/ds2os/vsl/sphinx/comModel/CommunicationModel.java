package org.ds2os.vsl.sphinx.comModel;

import java.io.Serializable;
import java.util.ArrayList;

import org.ds2os.vsl.sphinx.datastructures.HashList;
import org.ds2os.vsl.sphinx.model.StateX;

/**
 * This class is used to represent the communication behavior of a service
 * (called communication descriptor) or a type of service (called communication
 * model).
 *
 * The challenge here is to have a class that allows to merge two or more
 * instances and get a new one describing the whole thing. The question is:
 * should this also be possible between descriptors and models? It would be
 * great.
 *
 *
 * @author francois
 */
public class CommunicationModel implements Serializable {
  static final long serialVersionUID = -1248508327465114541L;

  /** Type of service. */
  public String type;

  /** The table of all communication relationships. */
  ArrayList<ComRelationship> relationTable = new ArrayList<ComRelationship>();

  /** This is the total number of descriptor merge to create this model. */
  public long lWeight = 1;
  /**
   * This BIRCHcluster saves the number of instances of the service type used
   * for this model (in N) and the duration of each instance (in [0]).
   *
   * The drawback of using this is that the merge process is not completely
   * universal anymore, but this is the sole way I found to do it.
   */
  BIRCHcluster supportCluster;
  /** */
  HashList<String, Long> aInstancesID = new HashList<String, Long>();

  /**
   * @param type
   *          the type of the service to be described
   * @param relationTable
   *          the relation table
   */
  public CommunicationModel(final String type, final ArrayList<ComRelationship> relationTable) {
    this.type = type;
    this.relationTable = relationTable;
  }

  /**
   * This merges two models and this one is the result of the merge.
   *
   * @param otherOne
   *          the other communication model
   */
  public void merge(final CommunicationModel otherOne) {

    try {
      for (ComRelationship relatOther : otherOne.relationTable) {
        boolean foundSameType = false;

        /*
         * if(type.contains("washingService")){
         * System.out.println("in the incomming descriptor: " +
         * relatOther.partnerType); }
         */

        for (ComRelationship relat : this.relationTable) {
          // either merge the relationship to the same type of service
          if (relatOther.partnerType.equals(relat.partnerType)) {
            foundSameType = true;
            relat.mergeTo(relatOther);
          }

        }
        if (!foundSameType) { // or add the existing to the relationtable -> but
                              // change the value?
                              // -> number should decrease
          // TODO
          this.relationTable.add(relatOther);
        }
      }
      lWeight += otherOne.lWeight;
      for (ComRelationship relat : this.relationTable) {
        relat.setIPratenWeight(lWeight);
      }
    } catch (NullPointerException e) {

    }

  }

  /**
   * This method actualizes the support NIRCHcluster of this model.
   *
   * right now it does not handle the disappearing of services
   */
  public void actualizeSupport() {
    int numberOfInstances = 0;

    for (String instanceID : this.aInstancesID.keyList()) {
      numberOfInstances++;
    }
    double[][] durations = new double[1][numberOfInstances];
    int k = 0;
    String a = "";
    for (long instanceStartTime : this.aInstancesID) {
      durations[0][k++] = Math.abs(StateX.actualTimestamp - instanceStartTime) / (60 * 60 * 1000);
      // a += " " + String.format(" %.2f",
      // Math.abs((double)StateX.actualTimestamp -
      // instanceStartTime) / (60 * 60 * 1000));
    }
    // if(type.equals("/lightControler")){System.out.println(a);}
    this.supportCluster = new BIRCHcluster(1, durations);

  }

  /**
   * This method actualizes the support NIRCHcluster of this model.
   * 
   * @return the support of the model.
   */
  public double computeSupportValue() {
    double supportOfModel = 0.0;

    // supportOfModel = (supportCluster.N * supportCluster.centroid()[0]) /
    // (StateX.thresholdNumberOfInstances *
    // StateX.thresholdDurationPerInstances); //* factor
    /*
     * System.out.println("Support of Model: " + String.format(" %.2f",
     * supportOfModel) + "\tN: " + supportCluster.N + "\tcenter: " +
     * String.format(" %.2f", supportCluster.centroid()[0])+
     * "\t  \t (printed from: " + this.getClass().getName() +" of:"+ type +")");
     */

    // This is better due to the possibility that the behavior can change: if
    // this value is to high
    // updates are never allowed.
    long tempLWeight = (lWeight < StateX.maxNumberDescriptor) ? lWeight
        : StateX.maxNumberDescriptor;
    supportOfModel = (double) tempLWeight / (double) StateX.maxNumberDescriptor;

    return supportOfModel;
  }

  /**
   * @return the instability
   */
  public final double getInstability() {
    double instability = 0.0;

    double meanNumberOfServices = 0.0;
    double meanNumberOfPacket = 0.0;
    double k = 0.0;
    for (ComRelationship comRelat : relationTable) {
      meanNumberOfServices += comRelat.numberOfPartners.centroid()[0];
      meanNumberOfPacket += comRelat.overallFrequency.centroid()[0];
      k++;
    }
    meanNumberOfServices = meanNumberOfServices / k;
    meanNumberOfPacket = meanNumberOfPacket / k;
    double connectionPerTime = (relationTable.size() * meanNumberOfServices)
        / (supportCluster.centroid()[0]);

    // the lower the more instable and so the more normal
    instability = (1 / connectionPerTime) * (meanNumberOfPacket) * 100;

    System.out.println(String.format("instability %.3f from: %.3f  %.3f", instability,
        (1 / connectionPerTime), meanNumberOfPacket));

    return instability;
  }

  @Override
  public final String toString() {
    String a = "CommunicationModel for " + type + ": \n";

    for (ComRelationship rela : relationTable) {
      a += rela.toString();
    }

    a += "\n";
    return a;
  }

  /**
   * returns the entry for this type.
   * 
   * @param typeName
   *          the name of the type
   * @return the relation type
   */
  public ComRelationship getRelatOfType(final String typeName) {
    int index = -1;
    for (int i = 0; i < this.relationTable.size(); i++) {
      if (relationTable.get(i).partnerType.equals(typeName)) {
        index = i;
        break;
      }
    }

    if (index != -1) {
      return relationTable.get(index);
    } else {
      return null;
    }

  }

}
