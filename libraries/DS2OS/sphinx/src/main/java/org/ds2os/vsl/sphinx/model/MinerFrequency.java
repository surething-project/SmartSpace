package org.ds2os.vsl.sphinx.model;

public class MinerFrequency extends Thread {

  int sleepDuration = 1;
  public boolean requested = true;

  public MinerFrequency() {
    if (StateX.testOnFile) {
      sleepDuration = 1;
    }
    this.start();
  }

  @Override
  public void run() {

    while (requested) {

      try {
        for (Vertice vert : StateX.flows.listServices) {

          for (EdgeX edge : vert.listConnections) {
            if (StateX.frequencyAD && edge.freqAnalizer.needUpdate) {
              edge.freqAnalizer.createModel();
            }
          }

        }
      } catch (Exception e) {
        // e.printStackTrace();
      }

      try {
        Thread.sleep(sleepDuration);
      } catch (InterruptedException e) {
        // e.printStackTrace();
      }

    }
  }

}
