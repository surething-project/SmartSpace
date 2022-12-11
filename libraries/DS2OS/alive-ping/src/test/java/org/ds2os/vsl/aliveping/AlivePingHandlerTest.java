package org.ds2os.vsl.aliveping;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Time;
import java.util.Collections;
import java.util.HashSet;

import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslAlivePingSender;
import org.ds2os.vsl.core.VslKORHash;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.VslTransportManager;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.config.VslAgentName;
import org.ds2os.vsl.core.config.VslAlivePingConfig;
import org.ds2os.vsl.core.impl.AlivePing;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.exception.KorSyncAlreadyInProgressException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

/**
 * @author jay
 * @author Johannes Straßer
 */
public class AlivePingHandlerTest {

    /**
     * Test method the handleAlivePing() method.
     *
     * @throws KorSyncAlreadyInProgressException
     *             shouldn't happen
     */
    @Test
    public final void testHandleAlivePing() throws KorSyncAlreadyInProgressException {
        final VslTransportManager transportManager = mock(VslTransportManager.class);
        final VslKORHash korHash = mock(VslKORHash.class);
        final VslX509Authenticator certAuth = mock(VslX509Authenticator.class);
        final VslAgentRegistryService agentRegistry = mock(VslAgentRegistryService.class);
        final VslAlivePingConfig config = mock(VslAlivePingConfig.class);
        final VslKORSyncHandler korSync = mock(VslKORSyncHandler.class);

        when(config.getAlivePingIntervall()).thenReturn(1);
        final AlivePingHandler pingHandler = new AlivePingHandler(transportManager, certAuth,
                korHash, agentRegistry, config, korSync);

        // In this environment a merge should be triggered
        final VslAlivePing alivePing = new AlivePing("agent2", 10, "ca",
                Collections.<VslTransportConnector>emptySet(), "group1", "hash");
        when(agentRegistry.isAgentConnected("agent2")).thenReturn(false);
        when(certAuth.getCAPublicKey()).thenReturn("ca");
        final HashSet<VslTransportConnector> connectors = new HashSet<VslTransportConnector>();
        connectors.add(new TransportConnector("https://10.0.8.2:8080"));
        when(agentRegistry.getTransports("agent2")).thenReturn(connectors);
        when(agentRegistry.getAttribute("agent2", "numKAs")).thenReturn("10");
        when(agentRegistry.isLeader()).thenReturn(true);
        when(agentRegistry.getNetworkSize()).thenReturn(5);
        when(agentRegistry.isGroupReachable("group1", 10)).thenReturn(true);
        when(agentRegistry.getLeader(alivePing.getGroupID())).thenReturn("agent34");
        when(agentRegistry.getMulticastGroupKeyHash()).thenReturn("group0");

        pingHandler.handleAlivePing(alivePing, false);

        // In this environment the sender should be added to the connected list
        when(agentRegistry.getMulticastGroupKeyHash()).thenReturn("group1");

        pingHandler.handleAlivePing(alivePing, true);
        verify(agentRegistry).addAgentToConnectedKAList(any(String.class), any(Time.class),
                any(Time.class));
    }

    /**
     * Tests if pings are sent with the correct data in the correct intervals when activate is
     * called.
     */
    @Test
    public final void testPingSending() {
        final VslTransportManager transportManager = mock(VslTransportManager.class);
        final VslKORHash korHash = mock(VslKORHash.class);
        final VslX509Authenticator certAuth = mock(VslX509Authenticator.class);
        final VslAgentRegistryService agentRegistry = mock(VslAgentRegistryService.class);
        final VslAlivePingConfig config = mock(VslAlivePingConfig.class);
        when(config.getAlivePingIntervall()).thenReturn(1);
        final VslKORSyncHandler korSync = mock(VslKORSyncHandler.class);

        final AlivePingHandler pingHandler = new AlivePingHandler(transportManager, certAuth,
                korHash, agentRegistry, config, korSync);

        when(korHash.getCurrentKORHash()).thenReturn("korhash");
        when(config.getAgentName()).thenReturn("abc");
        when(agentRegistry.getNetworkSize()).thenReturn(1);
        when(agentRegistry.getMulticastGroupKeyHash()).thenReturn("hash");
        when(certAuth.getCAPublicKey()).thenReturn("get");

        final VslAlivePingSender pingSender = mock(VslAlivePingSender.class);
        doReturn(Collections.singleton(pingSender)).when(transportManager).getAlivePingSenders();
        doNothing().when(pingSender).sendAlivePing(Matchers.any(VslAlivePing.class));

        pingHandler.activate();
        final ArgumentCaptor<VslAlivePing> pingCaptor = ArgumentCaptor.forClass(VslAlivePing.class);
        verify(pingSender, Mockito.timeout(1500)).sendAlivePing(pingCaptor.capture());
        assertThat(pingCaptor.getValue().getAgentId(), is("abc"));
        assertThat(pingCaptor.getValue().getCaPub(), is("get"));
        assertThat(pingCaptor.getValue().getNumKAs(), is(1));
        assertThat(pingCaptor.getValue().getGroupID(), is("hash"));
        assertThat(pingCaptor.getValue().getKorHash(), is("korhash"));

        pingHandler.shutdown();
    }

}
