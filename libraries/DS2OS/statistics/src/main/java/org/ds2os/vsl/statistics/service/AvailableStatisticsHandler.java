package org.ds2os.vsl.statistics.service;

import java.util.Iterator;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.adapter.VirtualNodeAdapter;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.statistics.StatisticsProvider;

/**
 * VirtualNodeHandler responsible for calls to the availableStatistics virtual Node of the
 * statistics Service.
 *
 * @author liebald
 */
public class AvailableStatisticsHandler extends VirtualNodeAdapter
        implements VslVirtualNodeHandler {

    /**
     * The {@link StatisticsProvider} used to access KA statistics.
     */
    private final StatisticsProvider statisticsProvider;

    /**
     * The {@link VslNodeFactory} used to construct results of the get operation.
     */
    private final VslNodeFactory nodeFactory;

    /**
     * Constructor.
     *
     * @param statisticsProvider
     *            The {@link StatisticsProvider} used to access KA statistics.
     * @param nodeFactory
     *            the {@link VslNodeFactory} used to construct results of the get operation.
     */
    public AvailableStatisticsHandler(final StatisticsProvider statisticsProvider,
            final VslNodeFactory nodeFactory) {
        this.statisticsProvider = statisticsProvider;
        this.nodeFactory = nodeFactory;
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final Iterator<String> iter = statisticsProvider.getAvailableStatistics().iterator();
        final StringBuilder sb = new StringBuilder();
        if (iter.hasNext()) {
            sb.append(iter.next());
            while (iter.hasNext()) {
                sb.append(",").append(iter.next());
            }
        }
        return nodeFactory.createImmutableLeaf(sb.toString());
    }

}
