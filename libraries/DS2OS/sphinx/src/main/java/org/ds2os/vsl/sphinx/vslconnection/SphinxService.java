package org.ds2os.vsl.sphinx.vslconnection;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.sphinx.model.StateX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This interface of the echidna with the vsl
 * 
 * 
 * @author francois
 *
 */
public class SphinxService {

  /**
   * * The model that is used for your service. //TODO: add your own modelId
   * here.
   */
  private static final String myServiceModelId = "/detection/sphinx";

  /** * The connector instance used by this service. */
  private VslConnector connector;

  /** * Stores the sub tree root that is belonging to this service. */
  private String myKnowledgeRoot;

  /** * You can use the logger for logs. */
  private static final Logger LOGGER = LoggerFactory.getLogger(SphinxService.class);

  private static final VslServiceManifest MANIFEST = new VslServiceManifest() {

    @Override
    public String getModelId() {
      return myServiceModelId;
    }

    @Override
    public String getModelHash() {
      return null;
    }

    @Override
    public String getBinaryHash() {
      return null;
    }
  };

  /**
   * The constructor that takes a connector instance and tries to register to a
   * local agent instance. TODO: Give your service a speaking name.
   * 
   * @param connector
   *          The connector used to interact with the VSL.
   */
  public SphinxService(VslConnector connector) {
    int numberKA = Integer.parseInt(StateX.hostingKA.split("t")[1]);
    // System.out.println(numberKA);

    this.connector = connector;

    try {
      this.connector = connector;
      myKnowledgeRoot = connector.registerService(MANIFEST);
      // System.out.println("registered service: " + myKnowledgeRoot);

      StateX.coHandler = new ConnectionHandler(connector, myKnowledgeRoot);
      // System.out.println("created ServiceHandler");

      StateX.serviceCreated = true;

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * /** Register all necessary VirtualNodeHandlers To know which nodes exist
   * have a look at the model specified with the Id in myServiceModelId. TODO:
   * Change to connect to your subscription handlers
   * 
   * @throws VslException If one of the VirtualNodehandlers couldn't be
   * registered correctly. / public void registerVirtualNodeHandlers() throws
   * VslException {
   * 
   * connector.registerVirtualNode(myKnowledgeRoot + "/messageBin", new
   * VslVirtualNodeHandler() {
   * 
   * @Override public void set(String address, VslNode value, VslIdentity
   * identity) throws VslException { // h.doSomething();
   * h.parseRecievedCommand(value.getValue()); }
   * 
   * @Override public VslNode get(String address, VslAddressParameters params,
   * VslIdentity identity) throws VslException { return null; }
   * 
   * @Override public void subscribe(String address) throws
   * SubscriptionNotSupportedException, VslException { }
   * 
   * @Override public void unsubscribe(String address) throws VslException { }
   * });
   * 
   * }
   */

  // https://127.0.0.1:8080
  /// home/francois/Documents/Uni/bachelor_arbeit_2/bachelor_arbeit_2/Simulation/certificates/cert_measurments/lightControl1.jks
  // 2
  // final String agentUrl = "https://127.0.0.1:808" + numberKA;
  // final String agentUrl = "https://10.0.97." + numberKA + ":808" + numberKA;

  // final String keystore =
  // "/home/francois/Documents/Uni/bachelor_arbeit_2/bachelor_arbeit_2/Simulation/certificates/cert_simulation/sphinx"
  // + numberKA + ".jks";
  // final String keystore =
  // "/home/francois/Documents/Uni/bachelor_arbeit_2/bachelor_arbeit_2/Simulation/certificates/cert_measurments/sphinx"
  // + numberKA + ".jks";
  // final String keystore =
  // "/home/francois/Documents/Uni/bachelor_arbeit_2/vsl/java7-ka/certificates/sphinx1.jks";
  // final String keystore = "./certificates/sphinx" + numberKA + ".jks";

  // final ServiceConnector connector = new ServiceConnector(agentUrl, keystore,
  // "K3yst0r3");
  /*
   * try { connector.activate(); } catch (Exception e2) {
   * LOGGER.error("Couldn't activate connector, shutting down.", e2);
   * 
   * System.out.println("Problem!!! \n \n Problem!!!");
   * 
   * System.exit(1); }
   */
}
