/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service.view;

import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.exception.OrchestrationServiceException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public abstract class AdaptationServiceViewFactory {
  private static final String GROUP = "service";
  private static final Pattern PATTERN = compile("^.*/(?<" + GROUP + ">\\w+)adaptationservice$");

  public static AdaptationServiceView getServiceView(RegularNode node)
      throws OrchestrationServiceException {
    final String serviceAddress = node.getAddress();

    final Matcher matcher = PATTERN.matcher(serviceAddress);
    if (!matcher.matches()) throw unknownService(serviceAddress);

    final String serviceAlias = matcher.group(GROUP);
    if (serviceAlias == null) throw unknownService(serviceAddress);

    switch (serviceAlias) {
      case "light":
        return new LightAdaptationServiceView(node);

      case "sound":
        return new SoundAdaptationServiceView(node);

      default:
        throw unknownService(serviceAddress);
    }
  }

  private static OrchestrationServiceException unknownService(String address) {
    return new OrchestrationServiceException("'%s' is not a known adaptation service", address);
  }
}
