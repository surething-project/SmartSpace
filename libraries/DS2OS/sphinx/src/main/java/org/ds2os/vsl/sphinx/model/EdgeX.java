package org.ds2os.vsl.sphinx.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the class used to represent communication relationships.
 *
 *
 * @author francois
 */
public class EdgeX implements Serializable {
  static final long serialVersionUID = -5504675832092749331L;

  /** */
  public final String edgeName;

  /** */
  public final String hostingKA;
  /** */
  public final String accessingID; // (C) The ID of the service accessing
  /** */
  public final String accessingAddress; // The extracted address of the
                                        // accessing node
  /** */
  public final String accessingType; // The type of this node

  /** */
  public final String accessedServiceAddress; // the address of the service
                                              // containing the accessed
                                              // node
  /** */
  public final String accessedServiceType; // the type of this service
  /** */
  public final String accessedServiceLocation;
  /** */
  public final String accessedNodeAddress; // (C) the address that is accessed
  /** */
  public final String accessedNodeType; // the type of this address

  // private final Vertice sourceService;

  /** */
  public final long firstTimestamp;

  /** */
  public long totalNumberOfPackets = 0;

  /** */
  public final ValueType typeOfValue;
  /** */
  public List<OperationType> operationList;

  // frequency AD variables:
  /** */
  public long lastTimestamp = 0;
  /** */
  public FrequencyAnalizer freqAnalizer;

  /** */
  private boolean allowed = false;
  /** */
  public boolean reviewedByUser = false;

  /** */
  public boolean isUnderDoSAttack = false;

  /**
   *
   * @param access
   *          The first acces of the edge
   * @param hostingKA
   *          the number of the hosting ka
   * @param allowance
   *          if the edge is allowed
   */
  public EdgeX(final Access access, final String hostingKA, final boolean allowance) {
    accessingID = access.accessingID;
    accessingAddress = access.accessingAddress;
    accessingType = access.accessingType;

    accessedServiceAddress = access.accessedServiceAddress;
    accessedServiceType = access.accessedServiceType;
    accessedServiceLocation = access.accessedLocation;
    accessedNodeAddress = access.accessedNodeAddress;
    accessedNodeType = access.accessedNodeType;
    firstTimestamp = access.timestamp;
    lastTimestamp = access.timestamp;
    this.hostingKA = hostingKA;
    edgeName = accessingID.replace("/", "_") + "-" + accessedNodeAddress.replace("/", "_");

    this.allowed = allowance;

    typeOfValue = ValueType.parse(access.accessedNodeType);

    operationList = new ArrayList<OperationType>();
    operationList.add(access.operation);

    totalNumberOfPackets = 1;
    /*
     * if(StateX.flows.listServices.get(accessingID).startTime -
     * access.timestamp < StateX.durationOfLearning){ allowed = true; } else {
     * allowed = false; System.out.println("would notify for an edge."); }
     */

    if (StateX.frequencyAD) {
      freqAnalizer = new FrequencyAnalizer(lastTimestamp, this);
    }

    // send this newly created egde to the sphinx
    if (StateX.serviceCreated) {
      StateX.coHandler.addEdgeToVSL(this);
    }

  }

  /**
   * Return if the edge is allowed.
   *
   * @return if it is allowed.
   */
  public final boolean isAllowed() {
    return allowed;
  }

  /**
   * Returns if the access is allowed.
   *
   * @param access
   *          the access
   * @return if it is allowed
   */
  public final boolean isAccessAllowed(final Access access) {
    addAccess(access, true);

    boolean allowance = false;

    switch (StateX.sensibilityLevel) {
      case 0:
        allowance = this.operationList.contains(access.operation) && allowed;
        break;
      case 1:
        allowance = allowed;
        break;
      default:
        break;
    }

    return allowance;
  }

  /**
   * Actualize the info of this flow with the new access. Right now it is called
   * for every packet passing through, which adds latency to the system.
   *
   * @param access
   *          the access
   * @param ignoreTimeStamp
   *          if timestamps should be ignored
   */
  public void addAccess(final Access access, final boolean ignoreTimeStamp) {
    totalNumberOfPackets++;

    if (!operationList.contains(access.operation)) {
      operationList.add(access.operation);
    }

    // this means that the frequency anomalies are non blocking -> just label
    // anomalies
    if (StateX.frequencyAD) {
      boolean normFreq = freqAnalizer.isTimestampNormal(access.timestamp);
      if (!normFreq) {
        // new AnomalyReport(this,Normality.ANOMALOUSF);
      }
    }

    if (!ignoreTimeStamp) {
      lastTimestamp = access.timestamp;
    }
  }

  @Override
  public final String toString() {
    String tmp = "is not allowed";
    if (allowed) {
      tmp = "is allowed";
    }
    String a = accessingID + "  ->  " + accessedNodeAddress + "    " + totalNumberOfPackets + "   "
        + tmp;
    a += "\t" + this.freqAnalizer.toString();
    a += "\t[";
    for (OperationType op : operationList) {
      a += op + ",";
    }
    a += "]";
    a += typeOfValue.toString();
    a += "\n";

    return a;
  }

  // ......................... setters: important to use them to send the
  // updates ..................

  /**
   * Allow the edge.
   *
   * @param allowance
   *          true for allow
   */
  public final void allow(final boolean allowance) {
    allowed = allowance;

    StateX.flows.listServices.get(accessingID).changedAllowance(this.accessedServiceAddress,
        this.accessedServiceType);

  }

  /**
   * Change the total number of packets.
   *
   * @param totalNumberOfPackets
   *          the new value
   */
  public final void setTotalNumberOfPackets(final int totalNumberOfPackets) {
    this.totalNumberOfPackets = totalNumberOfPackets;
    StateX.coHandler.updateEdgeValue(edgeName, "totalNumberOfPackets",
        Integer.toString(totalNumberOfPackets));
  }
  /*
   * public void setReviewedByUser(boolean reviewedByUser) { this.reviewedByUser
   * = reviewedByUser; //StateX.coHandler.updateEdgeValue(edgeName,
   * "totalNumberOfPackets", Integer.toString(totalNumberOfPackets));
   *
   * }
   */

  /**
   * add an operation.
   *
   * @param operation
   *          the operation to be added
   */
  public void addOperationToList(final OperationType operation) {
    operationList.add(operation);
    StateX.coHandler.addOperationEdge(edgeName, operation);
    // StateX.coHandler.updateEdgeValue(edgeName, "totalNumberOfPackets",
    // Integer.toString(totalNumberOfPackets));
  }

}
