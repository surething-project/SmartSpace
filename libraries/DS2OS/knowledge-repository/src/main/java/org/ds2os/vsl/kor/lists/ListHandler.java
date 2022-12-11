package org.ds2os.vsl.kor.lists;

import java.util.LinkedList;

import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.adapter.VirtualNodeAdapter;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.VslKnowledgeRepository;
import org.ds2os.vsl.kor.VslNodeDatabase;
import org.ds2os.vsl.kor.VslNodeTree;

/**
 * Abstract ListHandler class that bundles functionality needed by all specific ListHandlers
 * (add/set/ListRoot).
 *
 * @author liebald
 */
public abstract class ListHandler extends VirtualNodeAdapter implements VslVirtualNodeHandler {

    // /**
    // * Get the logger instance for this class.
    // */
    // private static final Logger LOGGER = LoggerFactory.getLogger(ListHandler.class);

    /**
     * The {@link VslNodeFactory} used to create VslNodes.
     */
    private final VslNodeFactory nodeFactory;

    /**
     * A the VslKnowledgeRepository used for adding a model as list element.
     */
    private final VslKnowledgeRepository kor;

    /**
     * The address of the lists main/root node in the KOR.
     */
    private final String listRootAddress;

    /**
     * Set the Root Address of the List.
     *
     * @param listRootAddress
     *            The address of the lists main/root node in the KOR.
     * @param kor
     *            Reference to the KOR.
     * @param nodeFactory
     *            The {@link VslNodeFactory} used to create VslNodes.
     */
    public ListHandler(final String listRootAddress, final VslKnowledgeRepository kor,
            final VslNodeFactory nodeFactory) {
        this.listRootAddress = listRootAddress;
        this.kor = kor;
        this.nodeFactory = nodeFactory;
    }

    /**
     * Return a reference to the KOR.
     *
     * @return A reference to the KOR.
     */
    public final VslKnowledgeRepository getKor() {
        return kor;
    }

    /**
     * Returns the list of all current elements of the list (excluding the add, del and element
     * nodes).
     *
     * @return A Linked List including all root names of the elements which are part of the list.
     * @throws VslException
     *             Thrown when an exception on retrieving the Node from the KOR happens.
     */
    public final LinkedList<String> getListElements() throws VslException {
        final String elementString = kor.get(getListRootAddress() + "/elements",
                new ServiceIdentity("system", VslNodeTree.SYSTEM_USER_ID)).getValue();

        final LinkedList<String> elements = new LinkedList<String>();
        if (elementString != null && !elementString.isEmpty()) {
            if (!(elementString.contains(VslNodeDatabase.LIST_SEPARATOR)
                    || elementString.contains(","))) {
                elements.add(elementString);
            } else {
                for (final String string : elementString
                        .split(VslNodeDatabase.LIST_SEPARATOR + "|;|,")) {
                    elements.add(string);
                }
            }
        }
        return elements;
    }

    /**
     * Returns the root address of the list that is handled.
     *
     * @return Address as String
     */
    public final String getListRootAddress() {
        return listRootAddress;
    }

    /**
     * @return the nodeFactory
     */
    public final VslNodeFactory getNodeFactory() {
        return nodeFactory;
    }

}
