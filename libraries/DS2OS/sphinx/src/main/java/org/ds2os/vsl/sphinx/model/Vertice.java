package org.ds2os.vsl.sphinx.model;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.ds2os.vsl.sphinx.algorithms.Formulas;
import org.ds2os.vsl.sphinx.comModel.BIRCHcluster;
import org.ds2os.vsl.sphinx.comModel.ComRelationship;
import org.ds2os.vsl.sphinx.comModel.CommunicationModel;
import org.ds2os.vsl.sphinx.datastructures.HashList;

/**
 * This class represents a vertice of the graph, hence, a service. The term
 * Vertice is used because for the class names solely the graph names are used.
 * Would be strange to have a class named "Service".
 *
 *
 * @author francois
 */
public class Vertice implements Serializable {
  static final long serialVersionUID = -4248500349625114541L;

  /** This is the name of the hosting ka. */
  public final String hostingKA;
  /** */
  public final String serviceID;
  /** */
  public String serviceAddress;
  /** */
  public String serviceType;

  /** */
  public final String location;

  /** */
  public final long startTime;
  /** */
  double timeOfLastNewEdge = 0;

  /** */
  public boolean allowed;
  /** */
  public boolean reviewedByUser;

  /**
   * The descriptor of the current behavior of the service.
   */
  public CommunicationModel descriptor;
  /**
   * The communication for this type of service generated/saved on site level.
   */
  public CommunicationModel siteComModel;
  /**
   * This gives if any packet arrived since the last passage of the miner com
   * model builder.
   */
  public boolean changedSinceLastPassage = false;

  /** */
  transient public HashList<String, EdgeX> listConnections = new HashList<String, EdgeX>(700);

  /**
   * String: address of service.
   */
  transient public HashList<String, OperationType> communicationPartners =
      new HashList<String, OperationType>(70);

  /**
   * String: type of service
   *
   * The integer value is represent the number of services of this type that it
   * accesses.
   */
  public transient HashList<String, Boolean> accessedTypes = new HashList<String, Boolean>(70);

  /** */
  public boolean ignoreTimeStamp = false;

  /** */
  double supportOfModel = 1.0;
  /** */
  ArrayList<Double> supportOfValues = new ArrayList<Double>();

  /**
   * constructor.
   * @param access access
   */
  public Vertice(final Access access) {
    serviceID = access.accessingID;
    startTime = access.timestamp;
    serviceAddress = access.accessingAddress;
    serviceType = access.accessingType;
    hostingKA = (access.accessedNodeAddress.split("/"))[1];

    location = access.accessingLocation;

    // should actually only be called for registerservice -> refine the
    // allowance
    if (StateX.learn || (access.operation == OperationType.REGISTERSERVICE)) {
      allowed = true;
    } else {
      allowed = false;
      System.out.println("would notify for a vertice.");
    }

    descriptor = new CommunicationModel(serviceType, new ArrayList<ComRelationship>());

    // send this newly created egde to the sphinx
    if (StateX.serviceCreated) {
      StateX.coHandler.addVertexToVSL(this);
    }

  }

  /**
   * This is used to add a connection having an just monitored an access
   * belonging to it.
   *
   * @param access access
   * @return anomalyValue anomalyValue
   */
  public double addConnection(final Access access) {

    if (serviceAddress.equals("none") && !access.accessingAddress.equals("none")) {
      serviceAddress = access.accessingAddress;
    }
    if (serviceType.equals("nk") && !access.accessingType.equals("nk")) {
      serviceType = access.accessingType;
    }

    // here look if the edge to be created is allowed or not
    boolean allowance = false;

    double anomalyValue = 0.0;
    double supportOfModel = 1.0;
    double supportOfRelationship = 0.97;
    double supportOfValue = 0.97;

    if (siteComModel != null) {
      supportOfModel = siteComModel.computeSupportValue(); // this is the
                                                           // support of the
                                                           // model
    }

    // in case this is the first flow of this vertex:

    // in case this is a new type of communication partner
    if (siteComModel != null && descriptor != null) {
      ComRelationship localRelation = descriptor.getRelatOfType(access.accessedServiceType);
      // careful! if accepted change manually the number of flows or add new relationship!!
      ComRelationship siteModelRelation = siteComModel.getRelatOfType(access.accessedServiceType);
      if (localRelation != null && siteModelRelation != null) {
        // if there is a comRelationship entry in both: descriptor and siteModel

        // here for new partner service , but need for new flow in the same
        // partner service too!
        supportOfRelationship = (double) siteModelRelation.numberOfPartners.getNumber()
            / (double) siteComModel.lWeight;

        supportOfValue = (Formulas.dist(localRelation.numberOfPartners.centroid()[0] + 1,
            siteModelRelation.numberOfPartners.centroid()[0]))
            / (StateX.factorTofMergingClusters * siteModelRelation.numberOfPartners.variance()[0]);
        System.out.println("Variance: " + siteModelRelation.numberOfPartners.variancePrint()[0]
            + "  Dist: "
            + Formulas.dist(localRelation.numberOfPartners.centroid()[0] + 1,
                siteModelRelation.numberOfPartners.centroid()[0])
            + " newValue: " + (localRelation.numberOfPartners.centroid()[0] + 1)
            + "  siteModel value: " + siteModelRelation.numberOfPartners.centroid()[0] + "  line: "
            + StateX.lineNumber);

      } else if (siteModelRelation != null) {
        /** if there is only an entry in the siteModel */
      } else if (localRelation != null) {
        /** if there is only an entry in the descriptor */
      } else {
        /** a completely new partner type, known by no one */
        supportOfValue = 9;
        supportOfRelationship = 1;
      }

    }

    anomalyValue = supportOfModel * supportOfRelationship * supportOfValue;
    System.out.println(String.format("%.2f from: %.2f   %.2f   %.2f", anomalyValue, supportOfModel,
        supportOfRelationship, supportOfValue));
    // in case this is a new operation type or data type on an existing flow

    if (anomalyValue > 1) {
      System.out
          .println("access: " + access.accessingAddress + " to " + access.accessedNodeAddress);
    }

    if (anomalyValue < StateX.anomalyThreshold) {
      allowance = true;
    }

    listConnections.put(access.accessedNodeAddress, new EdgeX(access, hostingKA, allowance));

    // adding the accessed service as a connection partner or updating the entry
    if (!communicationPartners.containsKey(access.accessedServiceAddress)) {
      if (listConnections.get(access.accessedNodeAddress).isAllowed()) {
        communicationPartners.put(access.accessedServiceAddress, access.operation);
      }
    } else {
      OperationType tmp = communicationPartners.get(access.accessedServiceAddress);
      if (tmp.getOverType() == OperationType.WRITE
          || access.operation.getOverType() == OperationType.WRITE) {
        tmp = OperationType.WRITE;
      }
      communicationPartners.remove(access.accessedServiceAddress);
      communicationPartners.put(access.accessedServiceAddress, tmp);
    }

    if (access.accessedServiceType.equals("nk")) {
      return anomalyValue;
    }
    // adding the accessed service as a connection partner or updating the entry
    if (!accessedTypes.containsKey(access.accessedServiceType)) {
      boolean allowedEdge = listConnections.get(access.accessedNodeAddress).isAllowed();
      accessedTypes.put(access.accessedServiceType, allowedEdge);
    } else {
      boolean tmp = accessedTypes.get(access.accessedServiceType);
      boolean allowedEdge = listConnections.get(access.accessedNodeAddress).isAllowed();
      accessedTypes.remove(access.accessedServiceType);
      accessedTypes.put(access.accessedServiceType, tmp || allowedEdge);
    }

    return anomalyValue;
  }

  /**
   * handles the new access on service level and returns the anomaly value.
   *
   * @param access access
   * @return if allowed
   */
  public boolean newAccess(Access access) {
    changedSinceLastPassage = true;
    // correct potential missing values
    if (serviceAddress.equals("none") && !access.accessingAddress.equals("none")) {
      serviceAddress = access.accessingAddress;
    }
    if (serviceType.equals("nk") && !access.accessingType.equals("nk")) {
      serviceType = access.accessingType;
    }

    supportOfValues = new ArrayList<Double>();

    double anomalyValue = 0.0;
    supportOfModel = 1.0;
    double supportOfRelationship = 0.97;
    double supportOfValue = 1.0;
    double tempSupportValues = 0;

    double weightRelationship = 0;

    // ArrayList<Double> supportOfValues = new ArrayList<Double>();

    if (siteComModel == null) { // if there is not site model we just add the
                                // flow, (in the current
                                // scenario this is all right)
      // latter on we will ask echidna to get the model from store
      addPartner(access);
      addFlow(access);
      addAccess(access);
    } else if (siteComModel != null && descriptor == null) {
      // this means that the service has no descriptor yet. Should actually
      // never happen.
    } else if (siteComModel != null && descriptor != null) { // verify we can
                                                             // access those
      supportOfModel = siteComModel.computeSupportValue();

      ComRelationship localRelation = descriptor.getRelatOfType(access.accessedServiceType);
      // careful! if accepted change manually the number of flows or add new relationship!!
      ComRelationship siteModelRelation = siteComModel.getRelatOfType(access.accessedServiceType);

      if (localRelation == null && siteModelRelation == null) {
        /**
         * if this partner was never seen for this type of service: a completely
         * new partner type, known by no one
         */

        // supportOfValue = siteComModel.getInstability();//9;
        supportOfValue = 20;
        supportOfRelationship = 1;
        // add a support of the "instability" of this type of service:
        // - light service very stabil, never new connections -> higher anomaly
        // - questioneur, not stabil at all, always new connections -> lower
        // anomaly
        // System.out.println("1");
        // System.out.println("\t\t instability:" + supportOfValue);
      } else if (localRelation != null && siteModelRelation == null) {
        /** if there is only an entry in the descriptor */
        // is bad if it happens, this means that man new connection to this type
        // are created but are
        // not in the model
        // so they are all done in a short amount of time
        System.out.println(
            "\t2\t\t\t from: " + access.accessingAddress + " to: " + access.accessedServiceAddress);
        // actually handle it just like the one of above, oder?
        // supportOfValue = siteComModel.getInstability();
        supportOfValue = 20;
      } else if (localRelation == null && siteModelRelation != null) {
        /** if there is only an entry in the siteModel */ // partner type known
                                                          // for this type of
        System.out.println(
            "\t\t3\t\t from: " + access.accessingAddress + " to: " + access.accessedServiceAddress);
        // handle like bellow! :
        supportOfValue = matchAccessToModel(access, localRelation, siteModelRelation);
        supportOfRelationship = siteModelRelation.getSupportOfRelationship();

      } else if (localRelation != null && siteModelRelation != null) {
        /** if the partner is known on both */
        // System.out.println("\t\t\t4");

        supportOfValue = matchAccessToModel(access, localRelation, siteModelRelation);

        supportOfRelationship = siteModelRelation.getSupportOfRelationship();
        weightRelationship = siteModelRelation.lWeight;
      }

      if (supportOfModel * supportOfRelationship > 10) {
        anomalyValue = supportOfModel * supportOfRelationship;
      } else {
        anomalyValue = supportOfValue;
      }

      anomalyValue = supportOfModel * Math.max(supportOfRelationship, supportOfValue);

    }

    if (anomalyValue > StateX.anomalyThreshold && !access.accessedServiceType.equals("none")) {
      String a = "";
      for (double value : supportOfValues) {
        a += String.format("%.2f, ", value);
      }
    }
    if (access.normalityString.contains("anomalous") && anomalyValue < StateX.anomalyThreshold) {
      // System.out.println("\nThe following was not detected: is
      // "+access.normalityString + " at: "
      // + access.timestamp);
      String a = "";
      for (double value : supportOfValues) {
        a += String.format("%.2f, ", value);
      }
    }

    String anomaly = String.format("%.2f", anomalyValue);
    // if true:
    if ((access.normalityString.contains("anomalous") && anomalyValue > StateX.anomalyThreshold)) {
      System.out.println(
          "found:" + anomaly + " is:" + access.normalityString + "for:" + access.accessingAddress
              + " to " + access.accessedNodeAddress + " time:" + access.timestamp);
    }

    // if false:
    if ((!access.normalityString.contains("anomalous") && anomalyValue > StateX.anomalyThreshold)
        || (access.normalityString.contains("anomalous")
            && anomalyValue < StateX.anomalyThreshold)) {
      String a = "";
      for (double value : supportOfValues) {
        a += String.format("%.2f, ", value);
      }
      a = "failed:" + anomaly + " was:" + access.normalityString + " for:" + access.accessingAddress
          + " to " + access.accessedNodeAddress + " time:" + access.timestamp + " " + a;
      a += "SoR:" + supportOfRelationship + " Rweight:" + weightRelationship + "\n";
      System.out.print(a);
      try {
        Files.write(
            Paths.get(
"/home/francois/Documents/Uni/bachelor_arbeit_2/sigcomm_iot/evaluation/outputDatasets/error.log"),
            a.getBytes(), StandardOpenOption.APPEND);
      } catch (IOException e) {
        System.out.println("error");
      }
    }

    /* The following conditions are used to test the algorithm offline. */
    if (access.normalityString.contains("anomalous") && anomalyValue > StateX.anomalyThreshold) {
      StateX.truePositive++;
    } else if (!access.normalityString.contains("anomalous")
        && anomalyValue < StateX.anomalyThreshold) {
      StateX.trueNegative++;
    } else if (!access.normalityString.contains("anomalous")
        && anomalyValue > StateX.anomalyThreshold) {
      StateX.falsePositive++;
    } else if (access.normalityString.contains("anomalous")
        && anomalyValue < StateX.anomalyThreshold) {
      StateX.falseNegative++;
      if (access.normalityString.contains("scan")) {
        StateX.failedScans++;
      }
      if (access.normalityString.contains("spying")) {
        StateX.failedSpying++;
      }
      if (access.normalityString.contains("Operation")) {
        StateX.failedOperation++;
      }
      if (access.normalityString.contains("Control")) {
        StateX.failedcontrol++;
      }
      if (access.normalityString.contains("SetUp")) {
        StateX.failedSetup++;
      }
      if (access.normalityString.contains("DoSattack")) {
        StateX.failedDoS++;
      }
      if (access.normalityString.contains("dataProbing")) {
        StateX.failedProbing++;
      }
    }
    addPartner(access);
    addFlow(access);
    addAccess(access);

    // return anomalyValue;
    return anomalyValue < StateX.anomalyThreshold;
  }


  /**
   * matchAccessToModel.
   * @param access access
   * @param localRelation localRelation
   * @param siteModelRelation siteModelRelation
   * @return anomaly
   */
  private double matchAccessToModel(final Access access, final ComRelationship localRelation,
      final ComRelationship siteModelRelation) {

    double supportOfValue = 1.0;
    double tempSupportValues = 0;

    int index = -1;

    // new partner?
    if (!communicationPartners.containsKey(access.accessedServiceAddress)) {
      // it is a new service
      // number of partners:
      if (localRelation != null) {
        tempSupportValues = (Formulas.dist(localRelation.numberOfPartners.centroid()[0] + 1,
            siteModelRelation.numberOfPartners.centroid()[0]))
            / (StateX.factorTofMergingClusters * siteModelRelation.numberOfPartners.variance()[0]);
      } else {
        tempSupportValues = (Formulas.dist(+1, siteModelRelation.numberOfPartners.centroid()[0]))
            / (StateX.factorTofMergingClusters * siteModelRelation.numberOfPartners.variance()[0]);
      }
      supportOfValues.add(tempSupportValues);

      // location of partners:
      double location = (this.location.equals(access.accessedLocation)) ? 1 : 0;
      tempSupportValues = (Formulas.dist(location, siteModelRelation.sameLocation.centroid()[0]))
          / (StateX.factorTofMergingClusters * siteModelRelation.sameLocation.variance()[0]);
      supportOfValues.add(tempSupportValues);
    }
    // new flow?
    if (!listConnections.containsKey(access.accessedNodeAddress)) {
      // it is a new flow
      // number of flows:
      if (localRelation != null) {
        tempSupportValues = (Formulas.dist(localRelation.numberOfFlows.centroid()[0] + 1,
            siteModelRelation.numberOfFlows.centroid()[0]))
            / (StateX.factorTofMergingClusters * siteModelRelation.numberOfFlows.variance()[0]);
      } else {
        tempSupportValues = (Formulas.dist(1, siteModelRelation.numberOfFlows.centroid()[0]))
            / (StateX.factorTofMergingClusters * siteModelRelation.numberOfFlows.variance()[0]);
      }
      supportOfValues.add(tempSupportValues);
    } else {

      tempSupportValues = siteModelRelation.getFrequencyAnomaly(
          access.timestamp - listConnections.get(access.accessedNodeAddress).lastTimestamp);

      // supportOfValues.add(tempSupportValues);

      tempSupportValues = isEdgeUnderDoS(access, localRelation, siteModelRelation);

      /*
       * if(listConnections.get(access.accessedNodeAddress).isUnderDoSAttack){
       * tempSupportValues = 600.0; }
       */

      if (tempSupportValues * supportOfModel > StateX.anomalyThreshold
          && tempSupportValues * supportOfModel < 600) {
        // the second condition is like a security in order to not ignore all
        // the timestamps
        ignoreTimeStamp = true;
      } else {
        ignoreTimeStamp = false;
      }

    }
    if (true) {
      // it is just a new access in an existing connection

      // location of partners: (done here because it is ignored else due to the
      // fact that no
      // descriptor exist
      double location = (this.location.equals(access.accessedLocation)) ? 1 : 0;
      tempSupportValues = (Formulas.dist(location, siteModelRelation.sameLocation.centroid()[0]))
          / (siteModelRelation.sameLocation.variance()[0] * 3 + 0.05);

      supportOfValues.add(tempSupportValues * 4);

      // operation type:
      // first find the one here
      index = -1;
      int k = 0;
      for (double value : access.operation.binarization()) {
        if (value >= 0.99) {
          index = k;
        }
        k++;
      }
      if (index != -1) { // should always be true!!
        tempSupportValues = (Formulas.dist(1, siteModelRelation.operationType.centroid()[index]))
            / (siteModelRelation.operationType.centroid()[index] + 0.05);

        double[][] operationType = new double[OperationType.DIMENSION][1];
        for (int i = 0; i < OperationType.DIMENSION; i++) {
          operationType[i][0] = access.operation.binarization()[i];
        }
        try {
          this.descriptor.getRelatOfType(access.accessedServiceType).operationType
              .addTo(new BIRCHcluster(4, operationType));
        } catch (Exception e) {
          // e.printStackTrace();
        }

        supportOfValues.add(tempSupportValues);

      } else {
        System.out.println("\t\t\t\t\t\t  Stranger behavior operation type!!");
      }

      // data type:
      index = -1;
      k = 0;
      for (double value : access.typeOfValue.binarization()) {
        if (value >= 0.99) {
          index = k;
        }
        k++;
      }
      if (index != -1) { // should always be true!!
        tempSupportValues = (Formulas.dist(1, siteModelRelation.valueType.centroid()[index]))
            / (siteModelRelation.valueType.centroid()[index] + 0.05);
        // / (StateX.factorTofMergingClusters *
        // siteModelRelation.valueType.variance()[index]);
        if (siteModelRelation.valueType.getNumber() > 100) {
          // tempSupportValues /=
          // Formulas.logb(siteModelRelation.valueType.getNumber(),6);
        }

        supportOfValues.add(tempSupportValues);
      } else {
        System.out.println("\t\t\t\t\t\t  Stranger behavior value type!!");
      }
    }

    /*
     * for(double value : supportOfValues){ if(value > 0.1){ supportOfValue *=
     * value * 1;} else { supportOfValue *= 0.1; } // keep this 0.001? }
     */
    supportOfValue = 0;
    for (double value : supportOfValues) {
      supportOfValue = Math.max(value, supportOfValue);
    }

    return supportOfValue;
  }

  /**
   * isEdgeUnderDoS.
   * @param access access
   * @param localRelation localRelation
   * @param siteModelRelation siteModelRelation
   * @return if it is
   */
  private double isEdgeUnderDoS(final Access access, final ComRelationship localRelation,
      final ComRelationship siteModelRelation) {

    try {
      listConnections.get(access.accessedNodeAddress).freqAnalizer.createModel();
    } catch (Exception e) {
    }

    double clusterAnomaly = siteModelRelation.getClusterGroupAnomaly(
        listConnections.get(access.accessedNodeAddress).freqAnalizer.BIRCHclusterList);

    /*
     * // just a small debug code used when the algorithm is tested on a dataset
     * if(access.normalityString.contains("DoSattack")){ String a = "";
     * for(BIRCHcluster clust :
     * listConnections.get(access.accessedNodeAddress).freqAnalizer.
     * BIRCHclusterList){ a += String.format("%.2f", clust.centroid()[0]) + ":"
     * + clust.getNumber() + ","; } System.out.printf("%.2f "+a,clusterAnomaly);
     * }
     */

    if (clusterAnomaly > 1000) {
      // System.out.println(clusterAnomaly + " ddoS ? at: " + access.timestamp);
      listConnections.get(access.accessedNodeAddress).isUnderDoSAttack = true;
    } else {
      listConnections.get(access.accessedNodeAddress).isUnderDoSAttack = false;
    }

    return clusterAnomaly;
  }

  /** Only does it if it is not already done.
   * @param access access
   */
  private void addPartner(final Access access) {
    if (access.accessedServiceType.equals("nk")) {
      return;
    }
    if (!accessedTypes.containsKey(access.accessedServiceType)) {
      accessedTypes.put(access.accessedServiceType, true);
    }
    if (!communicationPartners.containsKey(access.accessedServiceAddress)) {
      communicationPartners.put(access.accessedServiceAddress, OperationType.OTHER);
    }
  }

  /** Only does it if it is not already done.
   * @param access access
   */
  private void addFlow(final Access access) {
    if (!this.listConnections.containsKey(access.accessedNodeAddress)) {
      listConnections.put(access.accessedNodeAddress, new EdgeX(access, hostingKA, true));
    }
  }

  /** Only does it if it is not already done.
   * @param access access
   */
  private void addAccess(final Access access) {
    if (this.listConnections.containsKey(access.accessedNodeAddress)) {
      listConnections.get(access.accessedNodeAddress).addAccess(access, false);
    }
  }

  /**
   * changedAllowance.
   * @param addressService addressService
   * @param type type
   */
  public void changedAllowance(final String addressService, final String type) {
    // adding the accessed service as a connection partner or updating the entry
    if (!communicationPartners.containsKey(addressService)) {
      // allowedCommunicationPartners.put(addressService, true);
    } else {
      communicationPartners.remove(addressService);
      // allowedCommunicationPartners.put(addressService, true);
    }
    // adding the accessed service as a connection partner or updating the entry
    if (!accessedTypes.containsKey(type)) {
      accessedTypes.put(type, true);
    } else {
      communicationPartners.remove(type);
      accessedTypes.put(type, true);
    }
  }

  @Override
  public final String toString() {
    String a = "ID:" + serviceID + "\nlist of flows:\n";
    for (EdgeX edg : listConnections) {
      // a += listConnections.ge
      a += edg.toString();

    }

    a += descriptor.toString();

    a += "\n";
    return a;
  }

  /**
   * This function splits an address and returns the service address on one side
   * and the the node in the service on the other.
   *
   * @param address
   * @return string[]
   */
  private static String[] splitAddresses(String address) {
    String serviceAddress = "none";
    String nodeAddress = "none";

    if (address.equals("none")) {
      String[] a = {"none", "none"};
      return a;
    }

    String[] parts = address.split("/");

    if (parts.length < 3) {
      serviceAddress = "/" + parts[1];
    } else if (parts.length < 4) {
      serviceAddress = "/" + parts[1] + "/" + parts[2];
    } else {
      serviceAddress = "/" + parts[1] + "/" + parts[2];
      nodeAddress = "";
      for (int i = 3; i < parts.length; i++) {
        nodeAddress += "/" + parts[i];
      }
    }
    String[] a = {serviceAddress, nodeAddress};
    return (a);
  }

  /**
   * deleteOldEdge.
   */
  public void deleteOldEdge() {
    ArrayList<String> toDelete = new ArrayList<String>();
    if (StateX.deleteOldEdges) {
      for (EdgeX edge : listConnections) {
        if (edge.lastTimestamp + StateX.durationToKeepAnEdge < StateX.actualTimestamp) {
          // listConnections.remove(edge.accessedNodeAddress);
          toDelete.add(edge.accessedNodeAddress);
          this.communicationPartners.remove(edge.accessedServiceAddress);
          this.accessedTypes.remove(edge.accessedServiceType);
        }
      }

    }

    for (String name : toDelete) {
      listConnections.remove(name);
    }

  }

  /**
   * allow.
   * @param allowance allowance
   */
  public void allow(final boolean allowance) {
    this.allowed = allowance;
    for (EdgeX ed : listConnections) {
      ed.allow(allowance);
    }
  }

  /**
   * reviewedByUser.
   */
  public void reviewedByUser() {
    this.reviewedByUser = true;
    for (EdgeX ed : listConnections) {
      ed.reviewedByUser = true;
    }
  }

}
