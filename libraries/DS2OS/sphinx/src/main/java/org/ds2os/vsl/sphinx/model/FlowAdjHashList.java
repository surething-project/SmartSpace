package org.ds2os.vsl.sphinx.model;

import org.ds2os.vsl.sphinx.datastructures.HashList;

/**
 * This is the list of all the vertices contained in a hashlist.
 * 
 * 
 * @author francois
 */
public class FlowAdjHashList {

  public HashList<String, Vertice> listServices = new HashList<String, Vertice>(70);

  public FlowAdjHashList() {

  }

  /**
   * This method returns if an access is allowed. It is called for every new
   * packet. O(1) (if tables well balanced)
   * 
   * Moreover it add the access to the edge datatype. It should return a boolean
   * but, if it were the case two methods would have to be called. For
   * performance purpose it returns a string.
   * 
   * @param access access object?
   * @return if the access is allowed ('allowed'/'notallowed') and 'unexisting'
   *         if it does not exist
   */
  public String isAccessAllowed(Access access) {
    String answer = "unexisting";

    if (isFlowKnown(access)) {
      answer = checkAllowance(access);
    } else if (isServiceKnown(access)) {
      if (addTheFlow(access)) {
        answer = checkAllowance(access);
      }
    } else {
      if (addTheService(access)) {
        if (addTheFlow(access)) {
          answer = checkAllowance(access);
        }
      }
    }
    return answer;
  }

  private String checkAllowance(Access access) {
    boolean a = listServices.get(access.accessingID).newAccess(access);
    if (a) {
      return "allowed";
    } else {
      return "notallowed";
    }
  }

  /**
   * This method is here to tell if an access is part of an already known
   * connection. O(1) (if tables well balanced)
   * 
   * @param access access object?
   * @return if this access is part of a known flow
   */
  public boolean isFlowKnown(Access access) {
    boolean partOfFlow = false;

    if (listServices.containsKey(access.accessingID)) {
      partOfFlow = listServices.get(access.accessingID).listConnections
          .containsKey(access.accessedNodeAddress);
    }

    return partOfFlow;
  }

  /**
   * returns if the vertex is known, actually if it is not known then the
   * service is not from this ka.
   * 
   * @param access
   * @return
   */
  public boolean isServiceKnown(Access access) {
    if (listServices.containsKey(access.accessingID)) {
      return true;
    }
    return false;
  }

  /**
   * This method is here to tell if an access is part of an already known
   * connection. O(1) (if tables well balanced)
   * 
   * @param access access object???
   * @return if this access is part of a known flow
   */
  public boolean wasReviewed(Access access) {
    boolean flowReviewed = false;

    if (listServices.containsKey(access.accessingID)) {
      if (listServices.get(access.accessingID).listConnections
          .containsKey(access.accessedNodeAddress)) {
        flowReviewed = listServices.get(access.accessingID).listConnections
            .get(access.accessedNodeAddress).reviewedByUser;
      }
    }

    return flowReviewed;
  }

  /**
   * Adds the flow of this access to the flowlist.
   * 
   * returns true if the flow was added and false if it was not added
   * 
   * Only if it originates from this KA!!
   * 
   * @param access access object???
   *
   * @return true if succesfully added, else false
   */
  public boolean addTheFlow(Access access) {

    // see if the service exists
    if (listServices.containsKey(access.accessingID)) {
      double value = listServices.get(access.accessingID).addConnection(access);

      return true;
    }

    return false;
  }

  public boolean addTheService(Access access) {

    if (access.operation == OperationType.REGISTERSERVICE) { // only then we
                                                             // want to add the
                                                             // service, it
                                                             // means that it is
                                                             // on our
                                                             // ka
      // add the system services that do not register

      if (StateX.hostingKA == (access.accessedNodeAddress.split("/"))[1]) {
        System.out.println("BAD! BAD!! BAD!!!");
      }

      Vertice newService = new Vertice(access);
      listServices.put(access.accessingID, newService);

      return true;
    } else {
      return false;
    }

  }

  // the following bit can be used to get the anomaly value of the accesses.
  /*
   * public String isAccessAllowed(Access access){ String answer = "0.0";
   * 
   * if(isServiceKnown(access)){ answer = getAnomalyValue(access); // TODO:
   * handle here the fact that we do not log the things comming from womewhere
   * else } else { if(addTheService(access)){ answer = getAnomalyValue(access);
   * // TODO: handle here the fact that we do not log the things comming from
   * womewhere else } else { System.out.println("..." + StateX.lineNumber +
   * "   " + access.accessingAddress + "  was not registered"); } } return
   * answer; }
   * 
   * private String getAnomalyValue(Access access){
   * //listServices.get(access.accessingID).listConnections.get(access.
   * accessedNodeAddress). addAccess(access); //boolean a =
   * listServices.get(access.accessingID).listConnections.get(access.
   * accessedNodeAddress).isAllowed( ); double a =
   * listServices.get(access.accessingID).newAccess(access); return
   * Double.toString(a); }
   */

  @Override
  public String toString() {
    String a = "";
    int i = 0;
    for (Vertice ver : listServices) {
      a += ver.toString();
      i++;
    }
    a += "number of monitored services: " + i;

    return a;
  }

}
