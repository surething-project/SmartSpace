package org.ds2os.vsl.sphinx.vslconnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
//import java.util.Base64;

import javax.xml.bind.DatatypeConverter;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslSubscriber;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.sphinx.comModel.CommunicationModel;
import org.ds2os.vsl.sphinx.model.AnomalyReport;
import org.ds2os.vsl.sphinx.model.EdgeX;
import org.ds2os.vsl.sphinx.model.FlowAdjHashList;
import org.ds2os.vsl.sphinx.model.OperationType;
import org.ds2os.vsl.sphinx.model.StateX;
import org.ds2os.vsl.sphinx.model.Vertice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class ConnectionHandler extends Thread {
  /**
   * The connector instance used by this service. Needed to request and set VSL
   * context.
   */
  private final VslConnector c;

  /** Stores the root node of the service in the VSL. */
  private final String myKnowledgeRoot;

  /** You can use the logger for logs. */
  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionHandler.class);

  /** * The {@link VslNodeFactory} used to generate VslNodes. */
  private final VslNodeFactory nodeFactory;

  private int parseState = 0;

  private String echidnaAddress = "";

  /**
   * @param connector
   *          The connector used to interact with the VSL.
   * @param myKnowledgeRoot
   *          The root address of this service.
   * @throws VslException
   *           Thrown if a operation with the VSL didn't work.
   */
  public ConnectionHandler(VslConnector connector, String myKnowledgeRoot) throws VslException {
    this.c = connector;
    this.myKnowledgeRoot = myKnowledgeRoot;
    nodeFactory = connector.getNodeFactory();

    c.set(myKnowledgeRoot + "/blockingKA",
        nodeFactory.createImmutableLeaf(Boolean.toString(StateX.blockingKA)));

    registerSubscriptions();
    this.start();
  }

  /*
   * This is only used to discover echidna and subscribe to its nodes.
   */
  @Override
  public void run() {
    while (true) {
      try {
        String value = c.get("/search/type/detection/echidna").getValue();
        // looks like: /agent1/echidna

        if (value.contains("/")) {
          String[] addresses = value.split("//");

          // System.out.println("\n \n " + addresses[0]);
          if (!echidnaAddress.equals(addresses[0])) {

            // subscribe to echidna
            c.subscribe(addresses[0] + "/comModel", new VslSubscriber() {
              @Override
              public void notificationCallback(String address) throws VslException {
                recieveComModel(address);
              }
            });

            // System.out.println("\n \n registered!!! \n \n");
            echidnaAddress = addresses[0];
          }

        }

      } catch (VslException e) {
        e.printStackTrace();
      }
      try {
        Thread.sleep(4000); // 2 seconds should be enought but not too much
      } catch (InterruptedException e) {
        // e.printStackTrace();
      }
    }
  }

  // c.set(myKnowledgeRoot + "/movement",
  // nodeFactory.createImmutableLeaf(Boolean.toString(movement)));
  // c.set(myKnowledgeRoot + "/lastChange",
  // nodeFactory.createImmutableLeaf(Double.toString(d.getTime())));

  /**
   * Register all necessary subscriptions. To know which nodes exist have a look
   * at the model specified with the Id in myServiceModelId. TODO: Change to
   * connect to your subscription handlers
   * 
   * @throws VslException
   *           If one of the subscriptions didn't work.
   */
  public void registerSubscriptions() throws VslException {
    // Register a callback for each node you want to be notified for.

    /*
     * c.subscribe(myKnowledgeRoot + "/regularNode", new VslSubscriber() {
     * 
     * @Override public void notificationCallback(String address) throws
     * VslException { //regularNodeHandler(address); } });
     */

    c.subscribe(myKnowledgeRoot + "/blockingKA", new VslSubscriber() {
      @Override
      public void notificationCallback(String address) throws VslException {
        routingKApolicyChanged(address);
      }
    });
  }

  private void routingKApolicyChanged(String address) {
    System.out.println("this is called!!!! : routingKApolicyChanged(String address)");
    try {
      StateX.blockingKA = Boolean.parseBoolean(c.get(address).getValue());

      if (StateX.blockingKA == false) {
        restDetectionState(); // could be done with another node
      }

      // System.out.println("changed to : " + StateX.blockingKA);

      /*
       * if(StateX.blockingKA){ StateX.blockingKA = false; } else {
       * StateX.blockingKA = true; } c.set(myKnowledgeRoot+"/blockingKA",
       * nodeFactory.createImmutableLeaf(Boolean.toString(StateX.blockingKA)));
       */
    } catch (VslException e) {
      // e.printStackTrace();
    }
  }

  private void restDetectionState() {
    // reset flow localy
    StateX.flows = new FlowAdjHashList();

    // reset flow list:
    try {
      // add the new edge
      // String retur = c.get(myKnowledgeRoot +
      // "/edgeList/del/washingmachine-_agent1_washingmachine").getValue();//" +
      // "paul"
      // retur = c.get(myKnowledgeRoot +
      // "/vertexList/del/washingmachine").getValue();//" + "paul"
    } catch (Exception e) {

    }
    // StateX.myService = new SphinxService();

  }

  // ........................... changing the VSL "sending"
  // ...........................

  public void addEdgeToVSL(EdgeX edge) {
    try {
      // add the new edge
      String retur = c.get(myKnowledgeRoot + "/edgeList/add/0/detection/edge//" + edge.edgeName)
          .getValue(); // " + "paul"
      System.out.println(retur);

      // change it
      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/hostingKA",
          nodeFactory.createImmutableLeaf(edge.hostingKA));
      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/accessingID",
          nodeFactory.createImmutableLeaf(edge.accessingID));
      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/accessingAddress",
          nodeFactory.createImmutableLeaf(edge.accessingAddress));
      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/accessedServiceAddress",
          nodeFactory.createImmutableLeaf(edge.accessedServiceAddress));
      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/accessedServiceType",
          nodeFactory.createImmutableLeaf(edge.accessedServiceType));
      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/accessedNodeAddress",
          nodeFactory.createImmutableLeaf(edge.accessedNodeAddress));
      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/accessedNodeType",
          nodeFactory.createImmutableLeaf(edge.accessedNodeType));

      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/firstTimestamp",
          nodeFactory.createImmutableLeaf(Double.toString(edge.firstTimestamp)));
      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/totalNumberOfPackets",
          nodeFactory.createImmutableLeaf(Double.toString(edge.totalNumberOfPackets)));

      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/typeOfValue",
          nodeFactory.createImmutableLeaf(edge.typeOfValue.toString()));

      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/allowed",
          nodeFactory.createImmutableLeaf(Boolean.toString(edge.isAllowed())));
      c.set(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/reviewedByUser",
          nodeFactory.createImmutableLeaf(Boolean.toString(edge.reviewedByUser)));

      for (OperationType op : edge.operationList) {
        c.get(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/operationList/add/0/basic/text//"
            + op.toString()).getValue();
      }

      // c.set(myKnowledgeRoot+"/edgeList/"+edge.edgeName+"/hostingKA",nodeFactory.createImmutableLeaf(edge.hostingKA));

      // subscribe to changes on two fields that echidna can change
      c.subscribe(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/allowed", new VslSubscriber() {
        @Override
        public void notificationCallback(String address) throws VslException {
          changeOnEdge(address);
        }
      });
      c.subscribe(myKnowledgeRoot + "/edgeList/" + edge.edgeName + "/reviewedByUser",
          new VslSubscriber() {
            @Override
            public void notificationCallback(String address) throws VslException {
              changeOnEdge(address);
            }
          });

      c.set(myKnowledgeRoot + "/addedEdge",
          nodeFactory.createImmutableLeaf(myKnowledgeRoot + "/edgeList/" + edge.edgeName));

    } catch (VslException e) {
      // e.printStackTrace();
    }

  }

  /**
   * analyses the changes on one of the fields of an edge (from subscriptions)
   * 
   * TODO: add all the values that echidna can change
   * 
   * @param address
   */
  private void changeOnEdge(String address) {
    try {
      String valeursActuelles = c.get(address).getValue(); // I am sorry for
                                                           // this variable name
                                                           // in
                                                           // french, I wanted
                                                           // to call it current
                                                           // value but the
                                                           // french translation
                                                           // of
                                                           // this is
                                                           // "valeurActuelle"
                                                           // and the
                                                           // variable name is
                                                           // the name of a
                                                           // journal
                                                           // in france,
                                                           // therefore i choose
                                                           // this
                                                           // name

      String[] parts = address.split("/");

      // TODO : this nessessits that there is no service id nor node address
      // with - or _ ....
      String edgeName = parts[4].replaceAll("_", "/");
      String changedVariable = parts[5];
      String verticeID = edgeName.split("-")[0];
      String edgeID = edgeName.split("-")[1];

      if (changedVariable.equals("allowed")) {
        StateX.flows.listServices.get(verticeID).listConnections.get(edgeID)
            .allow((Boolean.parseBoolean(valeursActuelles)));
      } else if (changedVariable.equals("reviewedByUser")) {
        StateX.flows.listServices.get(verticeID).listConnections
            .get(edgeID).reviewedByUser = (Boolean.parseBoolean(valeursActuelles));
        System.out.println("\n \n just changed reviewed by user to: "
            + Boolean.parseBoolean(valeursActuelles) + " on: " + edgeName + "\n \n");

      }

      // System.out.println("\n \n \n");
      // System.out.println("changed: " + changedVariable + ", on : " + edgeName
      // + " to: " +
      // valeursActuelles + (Boolean.parseBoolean(valeursActuelles)));

    } catch (VslException e) {
      // e.printStackTrace();
    }
  }

  private void changeOnVertex(String address) {
    try {
      String valeursActuelles = c.get(address).getValue(); // I am sorry for
                                                           // this variable name
                                                           // in
                                                           // french, I wanted
                                                           // to call it current
                                                           // value but the
                                                           // french translation
                                                           // of
                                                           // this is
                                                           // "valeurActuelle"
                                                           // and the
                                                           // variable name is
                                                           // the name of a
                                                           // journal
                                                           // in france,
                                                           // therefore i choose
                                                           // this
                                                           // name

      String[] parts = address.split("/");

      // TODO : this nessessits that there is no service id nor node address
      // with - or _ ....
      String changedVariable = parts[5];
      String verticeID = parts[4].replaceAll("_", "/");

      if (changedVariable.equals("allowed")) {
        StateX.flows.listServices.get(verticeID).allow(Boolean.parseBoolean(valeursActuelles));
      } else if (changedVariable.equals("reviewedByUser")) {
        StateX.flows.listServices.get(verticeID).reviewedByUser = (Boolean
            .parseBoolean(valeursActuelles));
      }

    } catch (VslException e) {
      // e.printStackTrace();
    }
  }

  public void addVertexToVSL(Vertice vertex) {
    try {
      // add the new vertex
      String retur = c
          .get(myKnowledgeRoot + "/vertexList/add/0/detection/vertex//" + vertex.serviceID)
          .getValue();// " + "paul"
      System.out.println(retur);

      // change it
      c.set(myKnowledgeRoot + "/vertexList/" + vertex.serviceID + "/hostingKA",
          nodeFactory.createImmutableLeaf(vertex.hostingKA));
      c.set(myKnowledgeRoot + "/vertexList/" + vertex.serviceID + "/serviceID",
          nodeFactory.createImmutableLeaf(vertex.serviceID.replace("/", "_")));
      c.set(myKnowledgeRoot + "/vertexList/" + vertex.serviceID + "/serviceAddress",
          nodeFactory.createImmutableLeaf(vertex.serviceAddress));
      c.set(myKnowledgeRoot + "/vertexList/" + vertex.serviceID + "/serviceType",
          nodeFactory.createImmutableLeaf(vertex.serviceType));

      c.set(myKnowledgeRoot + "/vertexList/" + vertex.serviceID + "/startTime",
          nodeFactory.createImmutableLeaf(Double.toString(vertex.startTime)));

      c.set(myKnowledgeRoot + "/vertexList/" + vertex.serviceID + "/allowed",
          nodeFactory.createImmutableLeaf(Boolean.toString(vertex.allowed)));
      c.set(myKnowledgeRoot + "/vertexList/" + vertex.serviceID + "/reviewedByUser",
          nodeFactory.createImmutableLeaf(Boolean.toString(vertex.reviewedByUser)));

      // subscribe to changes on two fields that echidna can change
      c.subscribe(myKnowledgeRoot + "/vertexList/" + vertex.serviceID + "/allowed",
          new VslSubscriber() {
            @Override
            public void notificationCallback(String address) throws VslException {
              changeOnVertex(address);
            }
          });

      c.set(myKnowledgeRoot + "/addedVertex",
          nodeFactory.createImmutableLeaf(myKnowledgeRoot + "/vertexList/" + vertex.serviceID));

    } catch (VslException e) {
      // e.printStackTrace();
    }

  }

  /**
   * Sends a communication model on the VSL ba writing it on the node of sphinx.
   *
   * @param comMod  communication model?
   */
  public void sendComModel(CommunicationModel comMod) {

    try {
      String string = toString(comMod);
      // System.out.println(" Encoded serialized version " );
      // System.out.println( string );

      c.set(myKnowledgeRoot + "/comModel", nodeFactory.createImmutableLeaf(string));

      // System.out.println("sent comModel of type: " + comMod.type);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Parse a communication model recived from echidna.
   *
   * @param address echidna's address?
   */
  public void recieveComModel(String address) {
    try {
      String serializedModel = c.get(address).getValue();
      CommunicationModel some = (CommunicationModel) fromString(serializedModel);

      // System.out.println("recieved comModel of type: " + some.type);

      // if type add to the existing ones and so on
      if (StateX.comModels.contains(some.type)) {
        StateX.siteComModels.remove(some.type);
        StateX.siteComModels.put(some.type, some);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /** Read the object from Byte string. */
  private static Object fromString(String s) throws IOException, ClassNotFoundException {
    byte[] data = DatatypeConverter.parseBase64Binary(s);
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
    Object o = ois.readObject();
    ois.close();
    return o;
  }

  /** Write the object to a Byte string. */
  private static String toString(Serializable o) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(o);
    oos.close();
    byte[] message = (baos.toByteArray());
    return DatatypeConverter.printBase64Binary(message);
  }

  public void addOperationEdge(String edgeName, OperationType operation) {
    try {
      c.get(myKnowledgeRoot + "/edgeList/" + edgeName + "/operationList/add/0/basic/text//"
          + operation.toString()).getValue();
    } catch (VslException e) {
      // e.printStackTrace();
    }
  }

  public void updateEdgeValue(String edgeName, String valueName, String value) {
    try {
      c.set(myKnowledgeRoot + "/edgeList/" + edgeName + "/" + valueName,
          nodeFactory.createImmutableLeaf(value));
    } catch (VslException e) {
      // e.printStackTrace();
    }
  }

  public void reportAnomaly(AnomalyReport anomaly) {
    System.out.println("sending anomaly!!!");

    try {
      c.set(myKnowledgeRoot + "/currentAnomaly/anomalyType",
          nodeFactory.createImmutableLeaf(anomaly.anomalyType.toString()));
      c.set(myKnowledgeRoot + "/currentAnomaly/accessingID",
          nodeFactory.createImmutableLeaf(anomaly.accessingID));
      c.set(myKnowledgeRoot + "/currentAnomaly/accessingAddress",
          nodeFactory.createImmutableLeaf(anomaly.accessingAddress));
      c.set(myKnowledgeRoot + "/currentAnomaly/accessingType",
          nodeFactory.createImmutableLeaf(anomaly.accessingType));
      c.set(myKnowledgeRoot + "/currentAnomaly/accessedServiceAddress",
          nodeFactory.createImmutableLeaf(anomaly.accessedServiceAddress));
      c.set(myKnowledgeRoot + "/currentAnomaly/accessedServiceType",
          nodeFactory.createImmutableLeaf(anomaly.accessedServiceType));
      c.set(myKnowledgeRoot + "/currentAnomaly/accessedNodeAddress",
          nodeFactory.createImmutableLeaf(anomaly.accessedNodeAddress));
      c.set(myKnowledgeRoot + "/currentAnomaly/accessedNodeType",
          nodeFactory.createImmutableLeaf(anomaly.accessedNodeType));

      System.out.println("sent anomaly!!!");
    } catch (VslException e) {
      // e.printStackTrace();
    }
  }

}
