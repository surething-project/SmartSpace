package org.ds2os.vsl.searchProvider;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.adapter.VirtualNodeAdapter;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.statistics.VslStatisticsDatapoint;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidAddressException;
import org.ds2os.vsl.exception.VslException;

/**
 * Class for handling requests to the /agentid/search virtualNode.
 *
 * @author liebald
 */
public class SearchProviderHandler extends VirtualNodeAdapter implements VslVirtualNodeHandler {

    /**
     * The {@link VslConnector} used for accessing the local KA.
     */
    private final VslConnector con;

    /**
     * The {@link VslStatisticsProvider} used to generate statistics.
     */
    private final VslStatisticsProvider statisticsProvider;

    /**
     * The {@link VslTypeSearchProvider} used for retrieving the mapping between types and
     * addresses.
     */
    private final VslTypeSearchProvider typeSearchProvider;

    /**
     * The root address of the VirtualNode this handler handles.
     */
    private final String vNodeAddress;

    /**
     * The address of the local agent (e.g. /agent1).
     */
    private final String localAgentAddress;

    /**
     * The separator used for handing multiple search queries to the searchProvider.
     */
    private static final String SEPERATOR_VIRTUAL_GET_PARAMETERS = "//";

    /**
     * The separator used for merging multiple matching addresses into one string.
     */
    private static final String SEPERATOR_SEARCH_RESULTS = "//";

    /**
     * {@link ExecutorService} for parallel resolving search requests.
     */
    final ExecutorService executorService;

    /**
     * Constructor.
     *
     * @param statisticsProvider
     *            The {@link VslStatisticsProvider} used to generate statistics.
     * @param con
     *            The {@link VslConnector} used for accessing the local KA.
     * @param typeSearchProvider
     *            The {@link VslTypeSearchProvider} used for retrieving the mapping between types
     *            and addresses.
     */
    public SearchProviderHandler(final VslConnector con,
            final VslStatisticsProvider statisticsProvider,
            final VslTypeSearchProvider typeSearchProvider) {
        this.con = con;
        this.statisticsProvider = statisticsProvider;
        this.typeSearchProvider = typeSearchProvider;
        this.vNodeAddress = con.getRegisteredAddress();
        this.localAgentAddress = vNodeAddress.substring(0,
                vNodeAddress.substring(1).indexOf("/") + 1);
        executorService = Executors.newFixedThreadPool(5);
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final VslStatisticsDatapoint dp = statisticsProvider
                .getStatistics(this.getClass(), "Search").begin();

        String completeSearchParameter = AddressParser.getSuffix(vNodeAddress, address);
        List<SearchParameter> searchParameters = getListOfSearchQueries(completeSearchParameter);

        try {
            return con.getNodeFactory().createImmutableLeaf(
                    StringUtils.join(intersectAdresses(doSequentiellLookup(searchParameters)),
                            SEPERATOR_SEARCH_RESULTS));
        } finally {
            dp.end();
        }
    }

    /**
     * Does a sequential search for all given searchProfiders.
     *
     * @param searchParameters
     *            The {@link SearchParameter}s to use for the searches
     * @return The list of lists of addresses that map the different searches.
     * @throws VslException
     *             Thrown in case of an {@link VslException}
     */
    private List<List<String>> doSequentiellLookup(final List<SearchParameter> searchParameters)
            throws VslException {
        List<List<String>> resultsAllSearchProviders = new LinkedList<List<String>>();

        for (SearchParameter searchParameter : searchParameters) {
            VslNode queryresult = querySearchprovider(searchParameter);
            resultsAllSearchProviders.add(new LinkedList<String>(
                    Arrays.asList(queryresult.getValue().split(SEPERATOR_VIRTUAL_GET_PARAMETERS))));
        }

        return resultsAllSearchProviders;

    }

    /**
     * Does a parallel search for all given searchProficers.
     *
     * @param searchParameters
     *            The {@link SearchParameter}s to use for the searches
     * @return The list of lists of addresses that map the different searches.
     * @throws VslException
     *             Thrown in case of an {@link VslException}
     */
    private List<List<String>> doParallelLookup(final List<SearchParameter> searchParameters)
            throws VslException {
        List<List<String>> resultsAllSearchProviders = new LinkedList<List<String>>();

        List<Future<List<String>>> futures = new LinkedList<Future<List<String>>>();

        for (SearchParameter searchParameter : searchParameters) {
            futures.add(executorService.submit(new SearchResolver(searchParameter, con,
                    typeSearchProvider, localAgentAddress)));
        }

        for (Future<List<String>> future : futures) {
            try {
                resultsAllSearchProviders.add(future.get(300, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        return resultsAllSearchProviders;
    }

    /**
     * Intersects the Strings (addresses) in all given Lists of Strings (addresses). m
     *
     * @param resultsAllSearchProviders
     *            List of Lists of addresses returned by all queried searchproviders.
     * @return Returns the list of all Strings/addresses that were included in all Lists.
     */
    private List<String> intersectAdresses(final List<List<String>> resultsAllSearchProviders) {

        List<String> result = resultsAllSearchProviders.get(0);
        for (int i = 1; i < resultsAllSearchProviders.size(); i++) {
            result.retainAll(resultsAllSearchProviders.get(i));
        }
        return result;
    }

    /**
     * Returns the result for the given searchParameter from the included searchProvider.
     *
     * @param searchParameter
     *            The searchParameter containing all required information.
     * @return The VslNode containing the result from the specified searchProvider.
     * @throws VslException
     *             Thrown if an issue when querying a Search provider occurs.
     */
    private VslNode querySearchprovider(final SearchParameter searchParameter) throws VslException {
        final String searchProviderRequest = getSearchProvider(
                searchParameter.getSearchProviderType()) + searchParameter.getRequest();
        return con.get(searchProviderRequest);
    }

    /**
     * Returns a list of all {@link SearchParameter} that were included/encoded in the virtual get
     * request to the {@link SearchProviderService}.
     *
     * @param completeParameters
     *            all given parameters as String.
     * @return all given Parameters as List of {@link SearchParameter} Objects.
     */
    private List<SearchParameter> getListOfSearchQueries(final String completeParameters) {
        List<SearchParameter> searchParameters = new LinkedList<SearchParameter>();

        for (String searchParameterString : completeParameters.split(SEPERATOR_SEARCH_RESULTS)) {
            if (searchParameterString.indexOf("/") != -1) {
                searchParameters.add(new SearchParameter(
                        "/searchProvider/" + searchParameterString.substring(0,
                                searchParameterString.indexOf("/")),
                        searchParameterString.substring(searchParameterString.indexOf("/"))));
            } else {
                searchParameters
                        .add(new SearchParameter("/searchProvider/" + searchParameterString, ""));
            }
        }

        return searchParameters;
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
