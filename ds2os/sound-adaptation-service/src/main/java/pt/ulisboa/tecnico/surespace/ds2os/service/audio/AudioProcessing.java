/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.audio;

import pt.ulisboa.tecnico.surespace.common.async.AsyncListener;
import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;

public final class AudioProcessing {
  private int period;
  private int proofLength;
  private Timer timer;
  private WavFile wavFile;

  private void cancelTimer() {
    if (timer != null) timer.cancel();
  }

  private String getFilenameFromSongId(int songId) {
    return new DecimalFormat("0000").format(songId) + ".wav";
  }

  private int getFramesPerSample(long framesToRead) {
    return (int) Math.floor(framesToRead / ((proofLength * 1.0) / period));
  }

  private long getFramesTotal() {
    return (long) Math.ceil((proofLength * wavFile.getSampleRate()) / 1000.0);
  }

  private WavFile getWavFromResource(String resourceFilename)
      throws URISyntaxException, IOException, BroadException {
    // Get resource in the first place.
    URL resource = getClass().getClassLoader().getResource(resourceFilename);
    if (resource == null)
      throw new FileNotFoundException("Could not find resource file '" + resourceFilename + "'");

    return WavFile.openWavFile(new File(resource.toURI()));
  }

  public void init(int songId, int proofLength, int witnessPeriod)
      throws BroadException, IOException, URISyntaxException {
    this.proofLength = proofLength;
    this.period = witnessPeriod;

    // Get WAV file name.
    String name = getFilenameFromSongId(songId);
    // Get WAV file from resources.
    wavFile = getWavFromResource(name);
  }

  private void reset() {
    timer = null;
    proofLength = -1;
    period = -1;

    if (wavFile != null) {
      try {
        wavFile.close();

      } catch (IOException e) {
        e.printStackTrace();

      } finally {
        wavFile = null;
      }
    }
  }

  public void start(AsyncListener<Double, BroadException> listener) {
    this.timer = new Timer();

    long framesTotal = getFramesTotal();
    int framesPerSample = getFramesPerSample(framesTotal);

    timer.schedule(new AudioProcessingTask(listener, framesTotal, framesPerSample), 0, period);
  }

  public void stop() {
    cancelTimer();
    reset();
  }

  private class AudioProcessingTask extends TimerTask {
    private final int framesPerPeriod;
    private final long framesTotal;
    private final AsyncListener<Double, BroadException> listener;
    private long framesRead = 0;

    public AudioProcessingTask(
        AsyncListener<Double, BroadException> listener, long framesTotal, int framesPerPeriod) {
      this.listener = listener;
      this.framesTotal = framesTotal;
      this.framesPerPeriod = framesPerPeriod;
    }

    private double calculateAverage(double[] buffer, int readSize) {
      double sum = 0.0;
      for (int i = 0; i < readSize; i++) sum += abs(buffer[i]);
      return sum / (1.0 * readSize);
    }

    private int getFramesToRead() {
      if (framesRead + framesPerPeriod <= framesTotal) return framesPerPeriod;
      else return (int) (framesTotal - framesRead);
    }

    @Override
    public void run() {
      try {
        int framesToRead = getFramesToRead();
        if (framesToRead == 0) cancelTimer();
        else {
          double[] buffer = new double[framesToRead];
          int currentFramesRead = wavFile.readFrames(buffer, framesToRead);
          framesRead += currentFramesRead;

          listener.onComplete(calculateAverage(buffer, currentFramesRead));
          if (framesToRead < framesPerPeriod) cancelTimer();
        }

      } catch (BroadException e) {
        e.printStackTrace();
        listener.onError(e);

      } catch (Exception e) {
        e.printStackTrace();
        listener.onError(new BroadException(e.getMessage()));
      }
    }
  }
}
