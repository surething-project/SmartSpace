/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.util;

public abstract class ConstantPool {
  public abstract static class AdaptationService {
    public static final String BEACON = "/beacon";
    public static final String FRAGMENT_COUNT = "/fragmentCount";
    public static final String FRAGMENT_CURRENT = "/fragmentCurrent";
    public static final String FRAGMENT_LENGTH = "/fragmentLength";
    public static final String IS_LOCKED = "/isLocked";
    public static final String IS_STARTED = "/isStarted";
    public static final String SEED = "/seed";
    public static final String WITNESS = "/witness";

    public abstract static class Beacon extends SmartDevice {}

    public abstract static class Witness extends SmartDevice {}
  }

  public abstract static class LightAdaptationService extends AdaptationService {
    public static final String BEACON = "/beacon";
    public static final String WITNESS = "/witness";

    public abstract static class Beacon extends AdaptationService.Beacon {}

    public abstract static class Witness extends AdaptationService.Witness {
      public static final String INTENSITY = "/intensity";
      public static final String INTENSITY_SAMPLING_RATE = "/intensitySamplingRate";

      public abstract static class Intensity extends Property {}

      public abstract static class IntensitySamplingRate extends Property {}
    }
  }

  public abstract static class LocalizationService {
    public static final String LOCATION = "/location";

    public abstract static class Location extends ConstantPool.Location {}
  }

  public abstract static class Location {
    public static final String VALUE = "/value";
  }

  public abstract static class Property {
    public static final String VALUE = "/value";
  }

  public abstract static class SmartDevice {
    public static final String IS_ON = "/isOn";
  }

  public abstract static class SoundAdaptationService extends AdaptationService {
    public static final String BEACON = "/beacon";
    public static final String WITNESS = "/witness";

    public abstract static class Beacon extends AdaptationService.Beacon {}

    public abstract static class Witness extends AdaptationService.Witness {
      public static final String AMPLITUDE = "/amplitude";
      public static final String AMPLITUDE_SAMPLING_RATE = "/amplitudeSamplingRate";

      public abstract static class Amplitude extends Property {}

      public abstract static class AmplitudeSamplingRate extends Property {}
    }
  }
}
