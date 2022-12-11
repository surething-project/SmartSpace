package org.ds2os.vsl.kor.lists;

import java.util.List;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.ListOperationInvalidException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.VslKnowledgeRepository;
import org.ds2os.vsl.kor.VslNodeTree;

/**
 * This class handles list access in the KOR. (E.g. translating the position of an element in the
 * list to it's full address.)
 *
 * @author liebald
 */
public class ListRootHandler extends ListHandler implements VslVirtualNodeHandler {

    /**
     * Constructor for a ListRootHandler.
     *
     * @param listRootAddress
     *            The address of the lists main/root node in the KOR. Needed in order to safely tell
     *            where the virtual Part of the address starts. (e.g. the first address element
     *            after this is the position in the list).
     * @param kor
     *            the ModelInstantiationHandler used for adding a model as list element.
     * @param nodeFactory
     *            The {@link VslNodeFactory} used to create VslNodes.
     */
    public ListRootHandler(final String listRootAddress, final VslKnowledgeRepository kor,
            final VslNodeFactory nodeFactory) {
        super(listRootAddress, kor, nodeFactory);
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        // LOGGER.debug("get on address: {}", address);
        if (address.lastIndexOf("/") == getListRootAddress().lastIndexOf("/")) {
            // handle access on the actual Root node, not on any child
            return getKor().getVslNodeTree().get(address, params,
                    new ServiceIdentity(VslNodeTree.SYSTEM_USER_ID, VslNodeTree.SYSTEM_USER_ID));
        } else {
            // handle access on childs (e.g. translating positon to fully qualified name)
            final String suffix = address.substring(getListRootAddress().length() + 1,
                    address.length());
            final List<String> elements = getListElements();
            if (suffix.equals("size")) {
                return getNodeFactory().createImmutableLeaf(Integer.toString(elements.size()));
            }
            // LOGGER.debug(getFQNaddress(suffix, elements));
            return getKor().get(getFQNaddress(suffix, elements),
                    new ServiceIdentity(VslNodeTree.SYSTEM_USER_ID, VslNodeTree.SYSTEM_USER_ID));
        }
    }

    /**
     * Translates a given suffix (e.g. for /a/b/list/5/f the suffix would be 5/f) to a fully
     * qualified address of the node, based on name that is stored in the elements List at the
     * specified position.
     *
     * @param suffix
     *            Suffix/Subtree of the list.
     * @param elements
     *            list of all list elements
     * @return The fqn address of the node.
     * @throws NodeNotExistingException
     *             Throw when the node doesn't exist.
     */
    private String getFQNaddress(final String suffix, final List<String> elements)
            throws NodeNotExistingException {
        // LOGGER.debug("get FQN for suffix: {}", suffix);
        String position = suffix;
        if (suffix.contains("/")) {
            position = suffix.substring(0, suffix.indexOf("/"));
        }

        if (elements.contains(position)) {
            return suffix;
        }

        int pos = -1;
        try {
            pos = Integer.parseInt(position);
            if (pos < 0 || pos > elements.size() - 1) {
                throw new NumberFormatException();
            }
        } catch (final NumberFormatException e) {
            throw new NodeNotExistingException(
                    "Invalid Address, check that the nodes exist: " + position);
        }
        String fqn = getListRootAddress() + "/" + elements.get(pos);

        if (suffix.contains("/")) {
            fqn += suffix.substring(suffix.indexOf("/"));
        }
        return fqn;
    }

    @Override
    public final void set(final String address, final VslNode value, final VslIdentity identity)
            throws VslException {
        // LOGGER.debug("set on address: {}", address);

        if (address.lastIndexOf("/") == getListRootAddress().lastIndexOf("/")) {
            // handle access on the actual Root node, not on any child
            getKor().set(address, value,
                    new ServiceIdentity(VslNodeTree.SYSTEM_USER_ID, VslNodeTree.SYSTEM_USER_ID));
        } else {
            // handle access on childs (e.g. translating positon to fqn)
            final String suffix = address.substring(getListRootAddress().length() + 1,
                    address.length());
            final List<String> elements = getListElements();

            if (suffix.equals("size")) {
                throw new ListOperationInvalidException("Can't set the value of the list size!");
            }
            getKor().set(getFQNaddress(suffix, elements), value,
                    new ServiceIdentity(VslNodeTree.SYSTEM_USER_ID, VslNodeTree.SYSTEM_USER_ID));
        }
    }

}
