package org.ds2os.vsl.core.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link VslAddressParameters} interface.
 *
 * @author liebald
 */
public class AddressParameters implements VslAddressParameters {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressParameters.class);
    /**
     * The {@link Map} that stores the given parameters.
     */
    private final Map<String, String> parameters;

    /**
     * Constructor with default values. Scope=VALUE, depth=0.
     */
    public AddressParameters() {
        parameters = new HashMap<String, String>();
    }

    /**
     * Constructor that takes a map of parameters and stores them for later access.
     *
     * @param parameters
     *            The parameters stored in this {@link AddressParameters} object.
     */
    public AddressParameters(final Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public final NodeInformationScope getNodeInformationScope() {
        final String scope = parameters.get("scope");
        if (scope == null) {
            return NodeInformationScope.VALUE;
        } else if (scope.equalsIgnoreCase("C") || scope.equalsIgnoreCase("COMP")
                || scope.equalsIgnoreCase("COMPLETE")) {
            return NodeInformationScope.COMPLETE;
        } else if (scope.equalsIgnoreCase("M") || scope.equalsIgnoreCase("META")
                || scope.equalsIgnoreCase("METADATA")) {
            return NodeInformationScope.METADATA;
        } else {
            return NodeInformationScope.VALUE; // default
        }
    }

    @Override
    public final int getDepth() {
        try {
            return Integer.parseInt(parameters.get("depth"));
        } catch (final Exception e) {
            return 0; // default.
        }
    }

    @Override
    public final int getRequestedVersion() {
        try {
            return Integer.parseInt(parameters.get("version"));
        } catch (final Exception e) {
            return -1; // default.
        }
    }

    @Override
    public final boolean areParamsGiven() {
        return !parameters.isEmpty();
    }

    @Override
    public final VslAddressParameters withNodeInformationScope(final NodeInformationScope scope) {
        // if the parameter is set to default, remove it from the list.
        if (scope.equals(NodeInformationScope.VALUE)) {
            parameters.remove("scope");
        } else {
            parameters.put("scope", scope.toString());
        }
        return this;
    }

    @Override
    public final VslAddressParameters withDepth(final int depth) {
        // if the parameter is set to default, remove it from the list.
        if (depth == 0) {
            parameters.remove("depth");
        } else {
            parameters.put("depth", Integer.toString(depth));
        }
        return this;
    }

    @Override
    public final VslAddressParameters withVersion(final int version) {
        if (version < 0) {
            parameters.remove("version");
        } else {
            parameters.put("version", Integer.toString(version));
        }
        return this;
    }

    @Override
    public final Map<String, String> getParametersAsMap() {
        return Collections.unmodifiableMap(parameters);
    }

}
