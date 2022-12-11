package org.ds2os.vsl.searchProvider;

/**
 * POJO that contains all information for an individual search queries. This includes the
 * responsible search provider type (e.g. typesearch) and the request (e.g.type)
 *
 * @author liebald
 */
public class SearchParameter {

    /**
     * The type of the search provider responsible for the request (e.g./searchProvider/type).
     */
    private final String searchProviderType;

    /**
     * The request for the searchprovider (e.g. for typeSearch a type like /basic/text).
     */
    private final String request;

    /**
     * The Constructor for a {@link SearchParameter} object. Takes the type of the responsible
     * search provider and the request for that provider.
     *
     * @param searchProviderType
     *            The type of the search provider responsible for the request
     *            (e.g./searchProvider/type).
     * @param request
     *            The request for the searchprovider (e.g. for typeSearch a type like /basic/text).
     */
    public SearchParameter(final String searchProviderType, final String request) {
        this.searchProviderType = searchProviderType;
        this.request = request;
    }

    /**
     * Returns the type of the search provider responsible for the request
     * (e.g./searchProvider/type).
     *
     * @return Type of the search provider as String.
     */
    public String getSearchProviderType() {
        return searchProviderType;
    }

    /**
     * Returns the request for the searchprovider (e.g. for typeSearch a type like /basic/text).
     *
     * @return request for the search provider as String.
     */
    public String getRequest() {
        return request;
    }

}
