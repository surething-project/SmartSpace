package org.ds2os.vsl.sphinx.main;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.sphinx.model.Access;
import org.ds2os.vsl.sphinx.model.StateX;
import org.ds2os.vsl.sphinx.vslconnection.SphinxService;

/**
 * The Sphinx is the module monitoring and blocking the accesses.
 *
 * The monitoring is done by saving the connections in the form of a graph, the
 * services correspond to vertices and the communication relationships between
 * services correspond to edges.
 *
 * The vertex are saved in a hashlist: StateX.flows then, each of them has a
 * hashlist of edges
 *
 *
 *
 * @author francois
 */
public class Sphinx {
  /**
   * This is where all the configurations for the sphinx are saved.
   */
  StateX stateX; // starting the configuration.
  /** The connector to access the vsl. */
  private VslConnector connector;

  /** The number of packets. */
  int numberOfPacket = 0;

  /**
   * Constructor.
   * 
   * @param agentName
   *          agentName
   * @param connector
   *          vsl connector
   */
  public Sphinx(final String agentName, final VslConnector connector) {
    stateX = new StateX();

    StateX.hostingKA = agentName;
    this.connector = connector;
  }

  /**
   * This starts the sphinx service that will be used to communicate with
   * Echidna, the coordniator module.
   */
  public void startAll() {
    System.out.println(" \n \n \n Creating sphinx service: \n ");
    StateX.myService = new SphinxService(connector);
  }

  /**
   * This method is called for every new packet/access that happens. It has to
   * look if it fits in an existing flow, if it does then it sends the packet to
   * this flow. If it doesn't then it does things.. like destroying the access
   *
   * @param access
   *          new access
   * @return if it is allowed
   */
  public boolean accessAnalyser(final Access access) {

    // this is to be sure that the sphinx does not log it self, but it is not
    // secure right now (all
    // services could include "sphinx" in their name...
    if (access.accessingID.contains("sphinx") || access.accessingID.contains("echidna")
        || access.accessedNodeAddress.contains("search")) {
      return true;
    }

    final String allowance = StateX.flows.isAccessAllowed(access);
    String detectedBinary;
    String detected;

    if (numberOfPacket <= 28) {
      numberOfPacket++;
    } // this "28" corresponds to the number of accesses that are done in the KA
      // befor a service can
      // be started.
    if (numberOfPacket == 28) {
      startAll(); // start the sphinx service
    }

    if (allowance.equals("allowed")) {
      detected = "normal";
      detectedBinary = "normal";
    } else if (allowance.equals("unexisting")) {
      StateX.flows.addTheFlow(access);
      detected = "notknown";
      detectedBinary = "anomalous";
    } else {
      if (!StateX.flows.wasReviewed(access)) {
        // System.out.println(access.accessingID + " : anomalous!! awaiting for
        // user approval");
      } else {
        // System.out.println(access.accessingID + " : anomalous!!");
      }
      detected = "anomalous"; // add the type of anomaly
      detectedBinary = "anomalous";
    }

    if (!StateX.blockingKA) {
      return true;
    }
    return !(allowance.contains("not") && StateX.blockingKA);
  }

}
