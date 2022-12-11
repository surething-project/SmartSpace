package org.ds2os.vsl.sphinx.comModel;

import java.util.ArrayList;

import org.ds2os.vsl.sphinx.algorithms.Cluster;
import org.ds2os.vsl.sphinx.model.EdgeX;
import org.ds2os.vsl.sphinx.model.OperationType;
import org.ds2os.vsl.sphinx.model.StateX;
import org.ds2os.vsl.sphinx.model.ValueType;
import org.ds2os.vsl.sphinx.model.Vertice;

/**
 * This miner goes through all the vertices all the time to create them a
 * communication descriptor.
 *
 * @author francois
 */
public class MinerComDescriptor extends Thread {

  /** The duration between two mining. */
  int sleepDuration = 400;
  /** If it is requested. */
  public boolean requested = true;

  /**
   * Constructor.
   */
  public MinerComDescriptor() {
    if (StateX.testOnFile) {
      sleepDuration = 1;
    }
    this.start();
  }

  /** The last update. */
  long lastUpdate = 0;

  /** The number of updates. */
  long numberOfUpdates = 0;

  @Override
  public final void run() {

    while (requested) {

      if (StateX.actualTimestamp > lastUpdate + StateX.minerInterval) {
        lastUpdate = StateX.actualTimestamp;

        // System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t descriptor update: " +
        // numberOfUpdates++);

        try {
          for (Vertice vert : StateX.flows.listServices) {
            // does it make sense to create the whole descriptor each time?
            CommunicationModel tempDescriptor;
            ArrayList<ComRelationship> relationTable = new ArrayList<ComRelationship>();

            vert.deleteOldEdge();

            for (String type : vert.accessedTypes.keyList()) {
              int numberOfFlows = 0;
              int numberOfPartner = 0;
              ArrayList<String> comPartners = new ArrayList<String>();
              ArrayList<Double> overalFrequencies = new ArrayList<Double>();
              ArrayList<Double> sameLocations = new ArrayList<Double>();
              ArrayList<double[]> valueTypes = new ArrayList<double[]>();
              ArrayList<double[]> operationTypes = new ArrayList<double[]>();
              double location;

              ArrayList<Cluster> clusterList = new ArrayList<Cluster>();
              ArrayList<BIRCHcluster> lBIRCHclusterList = new ArrayList<BIRCHcluster>();

              for (EdgeX edge : vert.listConnections) {
                // true || System.out.println("diff:"+(StateX.actualTimestamp -
                // edge.lastTimestamp)
                // +"edge.lastTimestamp:" + edge.lastTimestamp + "
                // StateX.actualTimestamp:" +
                // StateX.actualTimestamp);
                if (edge.accessedNodeAddress.contains("/agent1/lightcontrol1/lig")
                    && edge.accessingAddress.contains("/agent6/washingmachin")) {
                  // System.out.println("last timestamp:" + edge.lastTimestamp +
                  // " sum:" +
                  // (edge.lastTimestamp + StateX.durationToKeepAnEdge) + "
                  // actualTimestamp:" +
                  // StateX.actualTimestamp);
                }
                if (edge.accessedNodeAddress.contains("/agent1/lightcontrol1/lig")
                    && edge.accessingAddress.contains("/agent6/washingmachin")) {
                  // System.out.println("parent type:" + vert.serviceType + "
                  // partner type:" +
                  // type);
                }
                /*
                 * if(edge.lastTimestamp + StateX.durationToKeepAnEdge <
                 * StateX.actualTimestamp){
                 *
                 * }
                 */
                if (edge.lastTimestamp + StateX.durationToKeepAnEdge > StateX.actualTimestamp) {

                  if (edge.accessedServiceType.equals(type)) {
                    if (!comPartners.contains(edge.accessedServiceAddress)) {
                      numberOfPartner++;
                      comPartners.add(edge.accessedServiceAddress);
                    }
                    numberOfFlows++;

                    double averalFreq = 1000 * (double) edge.totalNumberOfPackets
                        / (StateX.actualTimestamp - edge.firstTimestamp);
                    // propably more accurate with StateX.actualTimestamp than
                    // edge.lasttimestamp
                    if (averalFreq < Double.POSITIVE_INFINITY) {
                      overalFrequencies.add(averalFreq);
                    } else {
                      overalFrequencies.add(0.0);
                    }

                    if (vert.location.equals(edge.accessedServiceLocation)) {
                      location = 1;
                    } else {
                      location = 0;
                    }
                    sameLocations.add(location);

                    valueTypes.add(edge.typeOfValue.binarization());
                    for (OperationType operation : edge.operationList) {
                      operationTypes.add(operation.binarization());
                    }
                    for (Cluster clust : edge.freqAnalizer.clusterList) {
                      clusterList.add(clust);
                    }
                    for (BIRCHcluster clust : edge.freqAnalizer.BIRCHclusterList) {
                      lBIRCHclusterList.add(clust);
                    }
                  }
                }
              }

              double[] overalFrequency = new double[numberOfFlows];
              for (int i = 0; i < numberOfFlows; i++) {
                overalFrequency[i] = overalFrequencies.get(i);
              }
              double[] sameLocation = new double[numberOfFlows];
              for (int i = 0; i < numberOfFlows; i++) {
                sameLocation[i] = sameLocations.get(i);
              }

              double[][] valueType = new double[ValueType.dimension][numberOfFlows];
              for (int i = 0; i < ValueType.dimension; i++) {
                for (int j = 0; j < numberOfFlows; j++) {
                  valueType[i][j] = valueTypes.get(j)[i];
                }
              }

              double[][] operationType = new double[OperationType.DIMENSION][numberOfFlows];
              for (int i = 0; i < OperationType.DIMENSION; i++) {
                for (int j = 0; j < numberOfFlows; j++) {
                  operationType[i][j] = operationTypes.get(j)[i];
                }
              }
              // ComRelationship tempFlow = new
              // ComRelationship(vert.serviceType,type,numberOfPartner,numberOfFlows,overalFrequency,
              // sameLocation,valueType,operationType,clusterList);
              ComRelationship tempFlow = new ComRelationship(vert.serviceType, type,
                  numberOfPartner, numberOfFlows, overalFrequency, sameLocation, valueType,
                  operationType, lBIRCHclusterList);
              relationTable.add(tempFlow);
            }

            // tempDescriptor = new
            // CommunicationModel(vert.serviceType,relationTable);
            // vert.descriptor = tempDescriptor;
            vert.descriptor = new CommunicationModel(vert.serviceType, relationTable);
          }
        } catch (Exception e) { // ConcurrentModificationException |
                                // NullPointerException e) {
          // e.printStackTrace();
          System.out.println("\t \t \t problem!");
          e.printStackTrace();
        }
      }

      try {
        Thread.sleep(30); // sleepDuration
      } catch (InterruptedException e) {
        // e.printStackTrace();
      }

    }
  }

}
