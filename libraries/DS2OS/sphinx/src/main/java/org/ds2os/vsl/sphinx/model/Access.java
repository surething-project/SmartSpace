package org.ds2os.vsl.sphinx.model;

/**
 * This class describes all the monitored characteristics of accesses. One
 * instance of the class is used to describe one access.
 *
 * @author francois
 */
public class Access {

  // (C) stands for crucial
  /** */
  public final String accessingID; // (C) The ID of the service accessing
  /** */
  final String accessingAddress; // The extracted address of the accessing node
  /** */
  final String accessingType; // The type of this node
  /** */
  final String accessingLocation; // The location of this node
  /** */
  final String accessedServiceAddress; // the address of the service containing
                                       // the accessed
                                       // node
  /** */
  final String accessedServiceType; // the type of this service
  /** */
  final String accessedLocation; // The location of this node
  /** */
  public final String accessedNodeAddress; // (C) the address that is accessed
  /** */
  final String accessedNodeType; // the type of this address
  /** */
  final OperationType operation; // (C) the operation performed
  /** */
  final String value; // the value exchanged
  /** */
  final long timestamp; // the time at which this communication took place
  /** */
  final Normality norm; // normal/anomalous
  /** */
  final String normalityString;

  /** */
  final ValueType typeOfValue;

  /**
   * This method uses an entry in csv form to construct an Access instance.
   *
   * @param csvForm
   *          String in csv form
   */
  public Access(final String csvForm) {

    String[] parts = csvForm.split(",");

    accessingID = parts[0];
    accessingAddress = parts[1];
    accessingType = parts[2];
    accessingLocation = parts[3];
    accessedServiceAddress = parts[4];
    accessedServiceType = parts[5];
    accessedLocation = parts[6];
    accessedNodeAddress = parts[7];
    accessedNodeType = parts[8];

    typeOfValue = ValueType.parse(accessedNodeType);
    operation = OperationType.parse(parts[9]);
    value = parts[10];
    timestamp = Long.parseLong(parts[11]);
    StateX.actualTimestamp = timestamp;

    if (StateX.labeledDataset) {
      norm = Normality.parse(parts[12]);
      normalityString = parts[12];
    } else {
      norm = Normality.UNKNOWN;
      normalityString = "";
    }

  }

}
