package org.ds2os.vsl.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.adapter.VirtualNodeAdapter;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.exception.InvalidAddressException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.statistics.Statistics;
import org.ds2os.vsl.statistics.StatisticsProvider;

/**
 * Abstract class for statistic handlers that provides some common functionality.
 *
 * @author liebald
 *
 */
public abstract class AbstractStatisticsHandler extends VirtualNodeAdapter
        implements VslVirtualNodeHandler {

    /**
     * The {@link StatisticsProvider} used to access KA statistics.
     */
    private final StatisticsProvider statisticsProvider;

    /**
     * The address of the virtualnode to extract parameters as suffix.
     */
    private final String nodeAddress;

    /**
     * The {@link VslNodeFactory} used to construct results of the get operation.
     */
    private final VslNodeFactory nodeFactory;

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
    public AbstractStatisticsHandler(final String nodeAddress,
            final StatisticsProvider statisticsProvider, final VslNodeFactory nodeFactory) {
        this.statisticsProvider = statisticsProvider;
        this.nodeAddress = nodeAddress;
        this.nodeFactory = nodeFactory;
    }

    /**
     * Getter for the {@link StatisticsProvider} which contains access to all local statistics.
     *
     * @return the statisticsProvider
     */
    public final StatisticsProvider getStatisticsProvider() {
        return statisticsProvider;
    }

    /**
     * Getter for the Node address of the virtualNode this Handler is registered for.
     *
     * @return the nodeAddress
     */
    public final String getNodeAddress() {
        return nodeAddress;
    }

    /**
     * Extracts the parameter added to the queried virtual node. (e.g. for
     * /.../statstics/getStatisticsFor/statstics/identifier statstics/identifier would be returned.
     *
     * @param fullAddress
     *            The full address used to query the virtualNode.
     * @return The suffix/parameter added to the virtual nodes address.
     * @throws VslException
     *             Thrown if no parameter was added.
     */
    public final String getVnodeParameter(final String fullAddress) throws VslException {
        if (fullAddress.length() <= nodeAddress.length() + 1) {
            throw new InvalidAddressException("The given address" + fullAddress
                    + "doesn't contain the required Parameters required for the VirtualNode in "
                    + "order to work. Usage: " + nodeAddress + "/<requestedStatisticsName>");
        }
        return fullAddress.substring(nodeAddress.length() + 1);
    }

    /**
     * Returns the {@link Statistics} object identified by the given identifier.
     *
     * @param identifier
     *            Identifies the requested {@link Statistics} object.
     * @return The requested {@link Statistics} object.
     * @throws VslException
     *             Thrown of no {@link Statistics} object could be found for the identifier.
     */
    public final Statistics getStatistics(final String identifier) throws VslException {
        final Statistics statistics = statisticsProvider.getStatistics(identifier);
        if (statistics == null) {
            throw new InvalidAddressException("There are no statistics for " + identifier
                    + ". A List of all available Statistics is available via " + nodeAddress
                    + "/availableStatistics");
        }
        return statistics;
    }

    /**
     * Rounding utilitiy function for doubles.
     *
     * @param value
     *            The double to be rounded.
     * @param places
     *            Round to that many decimal places.
     * @return rounded decimal.
     */
    public static double round(final double value, final int places) {
        if (places < 0) {
            return value;
        }
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Get the Vsl node factory.
     *
     * @return the {@link VslNodeFactory}.
     */
    protected final VslNodeFactory getNodeFactory() {
        return nodeFactory;
    }
}
