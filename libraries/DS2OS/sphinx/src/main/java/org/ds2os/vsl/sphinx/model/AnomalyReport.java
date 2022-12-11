package org.ds2os.vsl.sphinx.model;

/**
 * description of an anomaly, still in development, will be improved with the
 * time.
 *
 *
 * @author francois
 *
 */
public class AnomalyReport {

  /** */
  public final Normality anomalyType;

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
  public final String accessedNodeAddress; // (C) the address that is accessed
  /** */
  public final String accessedNodeType; // the type of this address

  /**
   *
   * @param edge
   *          e
   * @param anomalyType
   *          a
   */
  public AnomalyReport(final EdgeX edge, final Normality anomalyType) {
    this.anomalyType = anomalyType;

    this.accessingID = edge.accessingID.replace("/", "_");
    this.accessingAddress = edge.accessingAddress.replace("/", "_");
    this.accessingType = edge.accessingType.replace("/", "_");
    this.accessedServiceAddress = edge.accessedServiceAddress.replace("/", "_");
    this.accessedServiceType = edge.accessedServiceType.replace("/", "_");
    this.accessedNodeAddress = edge.accessedNodeAddress.replace("/", "_");
    this.accessedNodeType = edge.accessedNodeType.replace("/", "_");

    if (!StateX.testOnFile) {
      StateX.coHandler.reportAnomaly(this);
    }

  }

}
