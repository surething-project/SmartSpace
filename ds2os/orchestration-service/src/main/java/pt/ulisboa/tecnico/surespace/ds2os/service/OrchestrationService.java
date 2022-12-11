/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.ds2os.service;

import org.ds2os.vsl.exception.VslException;
import pt.ulisboa.tecnico.surespace.common.async.AsyncListener;
import pt.ulisboa.tecnico.surespace.common.exception.BroadException;
import pt.ulisboa.tecnico.surespace.common.location.Location;
import pt.ulisboa.tecnico.surespace.common.location.LocationOLC;
import pt.ulisboa.tecnico.surespace.common.location.LocationProximity;
import pt.ulisboa.tecnico.surespace.common.location.exception.LocationException;
import pt.ulisboa.tecnico.surespace.common.manager.PropertyManagerInterface;
import pt.ulisboa.tecnico.surespace.common.proof.Beacon;
import pt.ulisboa.tecnico.surespace.common.proof.LocationProofProperties;
import pt.ulisboa.tecnico.surespace.ds2os.service.domain.RegularNode;
import pt.ulisboa.tecnico.surespace.ds2os.service.exception.OrchestrationServiceException;
import pt.ulisboa.tecnico.surespace.ds2os.service.manager.AdaptationServiceViewManager;
import pt.ulisboa.tecnico.surespace.ds2os.service.task.OrchestratorUpdateTask;
import pt.ulisboa.tecnico.surespace.ds2os.service.view.AdaptationServiceView;
import pt.ulisboa.tecnico.surespace.ds2os.service.view.LocalizationServiceView;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toCollection;
import static pt.ulisboa.tecnico.surespace.common.location.LocationProximity.ProximityCode.FAR;
import static pt.ulisboa.tecnico.surespace.ds2os.service.view.AdaptationServiceViewFactory.getServiceView;

public final class OrchestrationService extends Service {
  // Agent information - their location and supported adaptation services.
  private final ConcurrentHashMap<RegularNode, LocationOLC> agentsLocation;
  private final ConcurrentHashMap<RegularNode, LinkedHashSet<AdaptationServiceView>> agentsServices;

  // Timer to update agent information.
  private final Timer timer;

  public OrchestrationService(ServiceInitializer init, PropertyManagerInterface propertyManager)
      throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
          VslException {
    super(init.setServiceModelId("/services/orchestrationservice"));

    agentsLocation = new ConcurrentHashMap<>();
    agentsServices = new ConcurrentHashMap<>();

    int timerPeriod = propertyManager.get("ds2os", "service", "timer", "period").asInt();
    timer = new Timer();
    timer.schedule(new OrchestratorUpdateTask(this), 0, timerPeriod);
  }

  @Override
  public void close() {
    super.close();
    if (timer != null) timer.cancel();
  }

  private LinkedHashSet<AdaptationServiceView> filterEligibleServices(
      LinkedHashSet<AdaptationServiceView> eligibleBeacons) {
    return eligibleBeacons;
  }

  private RegularNode getClosestAgent(LocationOLC location)
      throws OrchestrationServiceException, LocationException {
    LOGGER.info("[*] Looking for agent closest to {}.", location);

    RegularNode agent = null;
    LocationProximity proximity = null;

    for (Entry<RegularNode, LocationOLC> entry : agentsLocation.entrySet()) {
      if (agent == null) {
        agent = entry.getKey();
        proximity = location.proximityTo(entry.getValue());
        continue;
      }

      RegularNode candidate = entry.getKey();
      LocationProximity candidateProximity = location.proximityTo(entry.getValue());
      int comparison = candidateProximity.compareTo(proximity);
      if (comparison > 0 || (comparison == 0 && candidate.compareTo(agent) < 0)) {
        proximity = candidateProximity;
        agent = candidate;
      }
    }

    if (agent == null) throw new OrchestrationServiceException("No agents were found");
    if (proximity.getCode() == FAR && proximity.getConfidence() >= 0.75)
      throw new OrchestrationServiceException("No suitable agent was found");

    return agent;
  }

  private LinkedHashSet<AdaptationServiceView> getEligibleServices(
      Location<?> location, ArrayList<Beacon> supportedBeacons)
      throws LocationException, OrchestrationServiceException {
    if (!(location instanceof LocationOLC))
      throw new LocationException("Unsupported location type");

    // Get nearest agent.
    RegularNode agent = getClosestAgent(((LocationOLC) location));

    // Get services registered to that agent.
    LinkedHashSet<AdaptationServiceView> agentServices = agentsServices.get(agent);
    if (agentServices == null || agentServices.isEmpty())
      throw new OrchestrationServiceException("No services are available");

    // Map services to the respective String identifiers.
    ArrayList<Beacon> agentBeacons = new ArrayList<>(new LinkedHashSet<>(supportedBeacons));

    // Intersect both sets.
    return agentServices.parallelStream()
        .filter(view -> agentBeacons.contains(new Beacon(view.getDescriptor())))
        .collect(toCollection(LinkedHashSet::new));
  }

  public LinkedHashSet<AdaptationServiceView> getSelectedServices(
      Location<?> location, ArrayList<Beacon> supportedBeacons)
      throws LocationException, OrchestrationServiceException {
    return filterEligibleServices(getEligibleServices(location, supportedBeacons));
  }

  public void proveLocation(
      LocationProofProperties proofProperties,
      LinkedHashSet<AdaptationServiceView> beacons,
      AsyncListener<LinkedHashSet<AdaptationServiceView>, BroadException> listener)
      throws OrchestrationServiceException {
    AdaptationServiceViewManager manager = new AdaptationServiceViewManager(proofProperties);
    manager.serviceAdd(beacons);

    if (!manager.servicesLock()) throw new OrchestrationServiceException("Could not lock services");
    if (!manager.servicesStart(listener))
      throw new OrchestrationServiceException("Could not start services");
  }

  public synchronized void updateAgentInformation()
      throws VslException, OrchestrationServiceException {
    updateAgentsLocation();
    updateAgentsServices();

    LOGGER.info("[+] Services: {}.", agentsServices);
  }

  private void updateAgentsLocation() throws VslException {
    // Find all existing agents offering a location service.
    Set<RegularNode> servicesNodes = searchService.searchByType("/services/localizationservice");

    // Remove previous results and add new ones.
    agentsLocation.clear();

    for (RegularNode serviceNode : servicesNodes) {
      RegularNode agent = serviceNode.parent();
      LocalizationServiceView service = new LocalizationServiceView(serviceNode);

      try {
        // Get the OLC location.
        LocationOLC location = new LocationOLC(service.getLocation());
        agentsLocation.putIfAbsent(agent, location);
        LOGGER.info("[+] Agent '{}' is at '{}'.", agent, location);

      } catch (VslException e) {
        LOGGER.error("[-] Could not retrieve agent '{}' location.", agent);
      }
    }
  }

  private void updateAgentsServices() throws VslException, OrchestrationServiceException {
    // Find all adaptation services offered by all agents.
    Set<RegularNode> servicesNodes = searchService.searchByType("/services/adaptationservice");

    // Remove previous results and add new ones.
    agentsServices.clear();

    for (RegularNode serviceNode : servicesNodes) {
      RegularNode agent = serviceNode.parent();

      // Ignore unknown agent.
      if (!agentsLocation.containsKey(agent)) continue;

      // Make sure the service set exists.
      agentsServices.putIfAbsent(agent, new LinkedHashSet<>());

      // Get the respective service.
      AdaptationServiceView serviceView = getServiceView(serviceNode);
      agentsServices.get(agent).add(serviceView);
      LOGGER.info("[+] Registered service '{}'.", serviceView);
    }
  }
}
