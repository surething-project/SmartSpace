/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.common.location.Location;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.LocalizationService.LOCATION;
import static pt.ulisboa.tecnico.surespace.ds2os.service.util.ConstantPool.LocalizationService.Location.VALUE;

public final class LocalizationService extends Service {
  public LocalizationService(ServiceInitializer init, Location<?> location)
      throws CertificateException, NoSuchAlgorithmException, KeyStoreException, VslException,
          IOException {
    super(init.setServiceModelId("/services/localizationservice"));

    // Update agent location.
    getNode(LOCATION, VALUE).setValue(location);
  }
}
