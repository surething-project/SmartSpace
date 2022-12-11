package org.ds2os.vsl.searchProvider;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.exception.InvalidAddressException;
import org.ds2os.vsl.exception.VslException;

/**
 * The {@link SearchResolver} takes a {@link SearchParameter} and {@link VslConnector}, executes the
 * (forward) search and returns the resulting list of addresses.
 *
 * @author liebald
 */
public final class SearchResolver implements Callable<List<String>> {

    /**
     * The separator used for handing multiple search queries to the searchProvider.
     */
    private static final String SEPERATOR_VIRTUAL_GET_PARAMETERS = "//";

    /**
     * The {@link SearchParameter} defining the Search that should be executed.
     */
    private final SearchParameter searchParameter;

    /**
     * The {@link VslConnector} to access the VSL.
     */
    private final VslConnector connector;

    /**
     * The {@link VslTypeSearchProvider} used for retrieving the mapping between types and
     * addresses.
     */
    private final VslTypeSearchProvider typeSearchProvider;

    /**
     * The address of the local agent (e.g. /agent1).
     */
    private final String localAgentAddress;

    /**
     * Constructor for a new {@link Callable} {@link SearchResolver}.
     *
     * @param searchParameter
     *            The {@link SearchParameter} defining the Search that should be executed.
     * @param connector
     *            The {@link VslConnector} to access the VSL.
     * @param typeSearchProvider
     *            The {@link VslTypeSearchProvider} used for retrieving the mapping between types
     *            and addresses.
     * @param localAgentAddress
     *            The address of the local agent (e.g. /agent1).
     */
    public SearchResolver(final SearchParameter searchParameter, final VslConnector connector,
            final VslTypeSearchProvider typeSearchProvider, final String localAgentAddress) {
        this.searchParameter = searchParameter;
        this.connector = connector;
        this.typeSearchProvider = typeSearchProvider;
        this.localAgentAddress = localAgentAddress;
    }

    @Override
    public List<String> call() throws Exception {
        VslNode queryresult = querySearchprovider();
        return new LinkedList<String>(
                Arrays.asList(queryresult.getValue().split(SEPERATOR_VIRTUAL_GET_PARAMETERS)));
    }

    /**
     * Returns the result for the given searchParameter from the included searchProvider. s
     *
     * @return The VslNode containing the result from the specified searchProvider.
     * @throws VslException
     *             Thrown if an issue when querying a Search provider occurs.
     */
    private VslNode querySearchprovider() throws VslException {
        final String searchProviderRequest = getSearchProvider(
                searchParameter.getSearchProviderType()) + searchParameter.getRequest();
        return connector.get(searchProviderRequest);
    }

    /**
     * Chooses the search provider if multiples are available. If a search provider is found on the
     * local agent, this one is preferred, otherwise a random one is selected. If none is found, an
     * Exception is thrown.
     *
     * @param typeOfSearch
     *            The kind of search provider that is required.
     * @return The address of the chosen search provider in the VSL.
     * @throws InvalidAddressException
     *             Thrown if no search provider is found.
     */
    private String getSearchProvider(final String typeOfSearch) throws InvalidAddressException {
        String searchProvider = "";

        final List<String> searchProviders = new LinkedList<String>(
                typeSearchProvider.getAddressesOfType(typeOfSearch));
        if (searchProviders.isEmpty()) {
            throw new InvalidAddressException("Could not find any search provider for "
                    + typeOfSearch + ". Usage: /search/<searchProvider>/<arguments>,"
                    + " e.g. /search/type/basic/text");
        } else {
            // check if one of the searchproviders is local
            for (final String next : searchProviders) {
                if (next.startsWith(localAgentAddress + "/")) {
                    searchProvider = next;
                    break;
                }
            }
            // otherwise choose one randomly
            if (searchProvider.isEmpty()) {
                final Random rnd = new Random();
                searchProvider = searchProviders.get(rnd.nextInt(searchProviders.size()));
            }
            return searchProvider;
        }
    }

}
