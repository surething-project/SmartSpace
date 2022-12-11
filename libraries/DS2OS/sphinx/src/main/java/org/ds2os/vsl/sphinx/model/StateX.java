package org.ds2os.vsl.sphinx.model;

import org.ds2os.vsl.sphinx.comModel.CommunicationModel;
import org.ds2os.vsl.sphinx.comModel.MinerComDescriptor;
import org.ds2os.vsl.sphinx.comModel.MinerComModel;
import org.ds2os.vsl.sphinx.datastructures.HashList;
import org.ds2os.vsl.sphinx.vslconnection.ConnectionHandler;
import org.ds2os.vsl.sphinx.vslconnection.SphinxService;

//import visualisation.GraphVisu;

/**
 * This class contains the different options used to run the sphinx. Only one
 * instance is created for each sphinx.
 * 
 * 
 * @author francois
 */
public class StateX {

  // -- --- --- General configuration --- --- ---
  /**
   * This is the name of the hosting KA, it can be used to determin if a request
   * is remote incoming or local originated.
   */
  public static String hostingKA = null;

  /**
   * The most recent time stamp seen on the Sphinx.
   */
  public static long actualTimestamp = 0;

  /**
   * Hashlist where all the vertices are saved.
   */
  public static FlowAdjHashList flows = new FlowAdjHashList();

  /**
   * if true: the anomalous packets are blocked
   */
  public static boolean blockingKA = true;

  // -- --- --- Frequency detection parameters --- --- ---
  /**
   * If true then the frequency anomaly detection module is used.
   * 
   */
  public static boolean frequencyAD = true;
  /**
   * size of the window used for frequency detection
   */
  public static int windowSizeFrequencyAD = 200;

  public static boolean deleteOldEdges = true;
  /** It is 10 minutes right now. */
  public static long durationToKeepAnEdge = 60 * 60 * 1000; // 60 *

  // -- --- --- Communication Models --- --- ---
  // Miners:
  public static MinerFrequency minerFrequency;
  public static MinerComDescriptor minerComDescriptor;
  public static MinerComModel minerComModel;

  /**
   * The hashlist of the communication models for the different type of services
   * present on the sphinx.
   * 
   */
  public static HashList<String, CommunicationModel> comModels = new HashList<String, CommunicationModel>(
      10);

  /**
   * The hashlist of the communication models for the different type of services
   * present on the site. They are received from the echidna.
   */
  public static HashList<String, CommunicationModel> siteComModels = new HashList<String, CommunicationModel>(
      10);

  // -- --- --- Anomaly values : communication model --- --- ---

  /**
   * This threshold is used in case the communication models are used to
   * determine if an anomaly values is anomalous.
   */
  public static double anomalyThreshold = 7.0;

  /**
   * This corresponds to the required number of instances of a type of service
   * to consider statistics relevant.
   */
  public static double thresholdNumberOfInstances = 10.0;

  /**
   * This corresponds to the required duration per instances instances of a type
   * of service to consider statistics relevant. It is expressed in hours.
   */
  public static double thresholdDurationPerInstances = 1.0;

  /**
   * This is the multiplying factor for the allowed distance used to merge
   * cluesters. This is advised to be 3 in "Data Mining: The Textbook".
   */
  public static double factorTofMergingClusters = 3.0;

  public static long maxNumberDescriptor = 3000; // 2000;

  /** the interval at which the miner mine, expressed in milliseconds */
  public static long minerInterval = 1 * 60 * 1000;

  public static long lastDoSTimetamp = 0;
  public static long numberOfDoS = 0;

  // -- --- --- First learning configuration --- --- ---
  /**
   * If the system is in a learn state all and every thing is accepted, should
   * almost never be the case because we want to have the learning phases for
   * each service independently.
   * 
   */
  @Deprecated
  public static boolean learn = true;

  /**
   * Duration of the learning phase for each service (in milliseconds). This is
   * still the first variant where edge get allowed if they are whithin this
   * time.
   */
  @Deprecated
  public static final double durationOfLearning = 15000;

  /**
   * if true: consider the operation type first hand in the detection
   */
  public static boolean useOperationType = false;

  /**
   * Level 0: no changes to the flow description are allowed. on edge Level 1:
   * different operations than the already seen ones are allowed. on edge Level
   * 2: a service is allowed to access knowledge nodes that belong to a service
   * from which they are already accessing other knowledge nodes. on vertex
   * Level 3: level 1 and 2 on vertex Level 4: level 3 and if a service has
   * already a communication relationship with a particular type of service it
   * can access other services of the same type. on vertex Level 5: add a level
   * with the rooms? Level 6: add periodicity here?
   */
  @Deprecated
  public static int sensibilityLevel = 0;

  // -- --- --- Tests configurations --- --- ---
  /*
   * This is used to test the detection "offline". No need to use this
   * parametert when using the Sphinx on a running KA.
   */
  public static boolean testOnFile = false;
  public static boolean resultInFile = false;
  public static boolean labeledDataset = false;

  public static int lineNumber = 1;

  public static int falsePositive = 0;
  public static int falseNegative = 0;
  public static int truePositive = 0;
  public static int trueNegative = 0;

  public static int failedScans = 0;
  public static int failedSetup = 0;
  public static int failedOperation = 0;
  public static int failedSpying = 0;
  public static int failedcontrol = 0;
  public static int failedDoS = 0;
  public static int failedProbing = 0;

  // -- --- --- Configurations of the connections --- --- ---
  /**
   * This handles the connection to the vsl.
   */
  public static ConnectionHandler coHandler;

  public static SphinxService myService;

  /**
   * gives if the sphinx service is active at a given time.
   */
  public static boolean serviceCreated = false;

  // public static boolean notifyUser = false; //if true: wait to add the flow
  // that the user accepts

  public StateX() {
    minerFrequency = new MinerFrequency();
    minerComDescriptor = new MinerComDescriptor();
    minerComModel = new MinerComModel();
  }

  public static void stopMiners() {

    StateX.minerFrequency.requested = false;
    StateX.minerComDescriptor.requested = false;
    StateX.minerComModel.requested = false;

    try {
      StateX.minerFrequency.join();
      StateX.minerComDescriptor.join();
      StateX.minerComModel.join();
    } catch (InterruptedException e) {
      // e.printStackTrace();
    }
  }

  public static void printComModels() {
    for (CommunicationModel comModel : StateX.comModels) {
      System.out.println(comModel);
    }

  }
}
