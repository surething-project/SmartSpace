package org.ds2os.vsl.typeSearch.service;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.adapter.VirtualNodeAdapter;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.statistics.VslStatisticsDatapoint;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidAddressException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.typeSearch.TypeSearchProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TypeSearchProvider responsible for the forward type search (mapping a type to addresses).
 *
 * @author liebald
 */
public class TypeSearchHandler extends VirtualNodeAdapter implements VslVirtualNodeHandler {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeSearchHandler.class);

    /**
     * The {@link TypeSearchProvider} used for mapping an type to corresponding addresses.
     */
    private final VslTypeSearchProvider typeSearchProvider;

    /**
     * The address of the virtualNode this handler is responsible for.
     */
    private final String vNodeAddress;

    /**
     * The {@link VslStatisticsProvider} for this handler.
     */
    private final VslStatisticsProvider statisticsProvider;

    /**
     * The {@link VslConnector} to access to Vsl.
     */
    private final VslConnector connector;

    /**
     * Constructor.
     *
     * @param vNodeAddress
     *            The address of the virtualNode this handler is responsible for.
     * @param statisticsProvider
     *            The {@link VslStatisticsProvider} for this handler.
     * @param typeSearchProvider
     *            The {@link VslTypeSearchProvider} used for mapping an type to corresponding
     *            addresses.
     * @param con
     *            The {@link VslConnector} to access to Vsl.
     */
    public TypeSearchHandler(final String vNodeAddress,
            final VslStatisticsProvider statisticsProvider,
            final VslTypeSearchProvider typeSearchProvider, final VslConnector con) {
        this.vNodeAddress = vNodeAddress;
        this.statisticsProvider = statisticsProvider;
        this.connector = con;
        this.typeSearchProvider = typeSearchProvider;
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final VslStatisticsDatapoint statistic = statisticsProvider
                .getStatistics(this.getClass(), "Get").begin();
        //TODO: access control
        final VslNode result = connector.getNodeFactory()
                .createImmutableLeaf(Arrays.asList("/basic/text"), StringUtils.join(
                        typeSearchProvider.getAddressesOfType(getQueriedParameters(address)),
                        "//"));
        statistic.end();
        LOGGER.debug("virtual get on {}, {}", address, result.getValue());
        return result;

    }

    /**
     * Extracts the queried type from the used address.
     *
     * @param fullAddress
     *            The full address used to query the virtualNode.
     * @return The suffix/parameter added to the virtual nodes address.
     * @throws VslException
     *             Thrown if no parameter was added.
     */
    public final String getQueriedParameters(final String fullAddress) throws VslException {
        if (fullAddress.length() <= vNodeAddress.length() + 1) {
            throw new InvalidAddressException("The given address" + fullAddress
                    + " doesn't contain the required Parameters required for the VirtualNode in "
                    + "order to work. Usage: get " + vNodeAddress + "/<requestedType>");
        }
        // LOGGER.debug(fullAddress.substring(vNodeAddress.length()));
        return fullAddress.substring(vNodeAddress.length());
    }

}
