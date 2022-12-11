package org.ds2os.vsl.sphinx.comModel;

import org.ds2os.vsl.sphinx.model.StateX;
import org.ds2os.vsl.sphinx.model.Vertice;

/**
 * This miner goes through all the vertex to gather their descriptors and merge
 * them into communication models for all the service types present on this
 * sphinx.
 *
 * @author francois
 */
public class MinerComModel extends Thread {

  /** The duration between two mining. */
  int sleepDuration = 400;
  /** If it is requested. */
  public boolean requested = true;

  /**
   * Constructor.
   */
  public MinerComModel() {
    if (StateX.testOnFile) {
      sleepDuration = 1;
    }
    this.start();
  }

  /** Last update. */
  long lastUpdate = 0;

  /** number of updates. */
  long numberOfUpdates = 0;

  /** Counter. */
  int counter = 0;

  @Override
  public final void run() {

    while (requested) {
      // System.out.println("heyOrig");

      if (StateX.actualTimestamp > lastUpdate + StateX.minerInterval) {
        lastUpdate = StateX.actualTimestamp;

        System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t model update: " + numberOfUpdates++);

        try {
          for (Vertice vert : StateX.flows.listServices) {
            // if(vert.changedSinceLastPassage){ // only use if the decriptor
            // changed
            // either merge the service descriptor with the exisitng definition
            if (StateX.comModels.containsKey(vert.serviceType)
                && !vert.descriptor.type.equals("nk")) {
              StateX.comModels.get(vert.serviceType).merge(vert.descriptor);
              // actualize the number of service instances:
              if (!StateX.comModels.get(vert.serviceType).aInstancesID
                  .containsKey(vert.serviceID)) {
                StateX.comModels.get(vert.serviceType).aInstancesID.put(vert.serviceID,
                    vert.startTime);
              }
              StateX.comModels.get(vert.serviceType).actualizeSupport();

              // actualize the model on the vertice:
              if (StateX.siteComModels.contains(vert.serviceType)) {
                vert.siteComModel = StateX.siteComModels.get(vert.serviceType);
              } else {
                vert.siteComModel = StateX.comModels.get(vert.serviceType);
              }

            } else if (!vert.descriptor.type.equals("nk")) { // or create a new
                                                             // communication
                                                             // model
                                                             // for the type we
                                                             // had not seen yet
              if (vert.descriptor != null) {
                StateX.comModels.put(vert.descriptor.type, vert.descriptor);
              }
            }
            vert.changedSinceLastPassage = false;
            // }

          }

        } catch (Exception e) { // ConcurrentModificationException |
                                // NullPointerException |
          e.printStackTrace();
        }

        // send all models to echidna:
        for (CommunicationModel comMod : StateX.comModels) {

          try {
            StateX.coHandler.sendComModel(comMod);
          } catch (Exception e) {
            // e.printStackTrace();
          }
          try {
            Thread.sleep(30); // sleepDuration
          } catch (InterruptedException e) {
            // e.printStackTrace();
          }
        }
        // System.out.println("heyAfter");

      } else {
        counter++;
        if (counter < 100) {
          // System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t should update at:"
          // + (lastUpdate +
          // StateX.minerInterval) + " it is: "+StateX.actualTimestamp);
          counter = 0;
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
