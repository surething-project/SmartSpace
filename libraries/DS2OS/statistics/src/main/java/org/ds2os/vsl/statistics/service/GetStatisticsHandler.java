package org.ds2os.vsl.statistics.service;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.statistics.StatisticsDatapoint;
import org.ds2os.vsl.statistics.StatisticsProvider;

/**
 * VirtualNodeHandler responsible for calls to the availableStatistics virtual Node of the
 * statistics Service.
 *
 * @author liebald
 */
public class GetStatisticsHandler extends AbstractStatisticsHandler {

    /**
     * Constructor.
     *
     * @param nodeAddress
     *            The address of the virtualnode to extract parameters as suffix.
     * @param statisticsProvider
     *            The {@link StatisticsProvider} used to access KA statistics.
     * @param nodeFactory
     *            the {@link VslNodeFactory} used to construct results of the get operation.
     */
    public GetStatisticsHandler(final String nodeAddress,
            final StatisticsProvider statisticsProvider, final VslNodeFactory nodeFactory) {
        super(nodeAddress, statisticsProvider, nodeFactory);
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final VslMutableNode result = getNodeFactory().createMutableNode();
        int counter = 1;
        for (final StatisticsDatapoint dp : getStatistics(getVnodeParameter(address))
                .getDatapoints()) {
            final VslMutableNode dpNode = getNodeFactory()
                    .createMutableNode(Integer.toString(counter));
            dpNode.putChild("StartTime",
                    getNodeFactory().createMutableNode(Long.toString(dp.getStartTime())));
            dpNode.putChild("EndTime",
                    getNodeFactory().createMutableNode(Long.toString(dp.getEndTime())));
            dpNode.putChild("DurationMilli",
                    getNodeFactory().createMutableNode(Long.toString(dp.getDurationMilli())));
            dpNode.putChild("DurationNano",
                    getNodeFactory().createMutableNode(Long.toString(dp.getDurationNano())));
            result.putChild(Integer.toString(counter++), dpNode);
        }

        return result;
    }

}
