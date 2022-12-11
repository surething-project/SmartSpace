package org.ds2os.vsl.kor.lists;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.ListOperationInvalidException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.Restrictions;
import org.ds2os.vsl.kor.VslKnowledgeRepository;
import org.ds2os.vsl.kor.VslNodeDatabase;
import org.ds2os.vsl.kor.VslNodeTree;

/**
 * This Handler is called when the delete (del) operation/virtualNode of the List is called it was
 * registered for.
 *
 * @author liebald
 */
public class ListDelHandler extends ListHandler implements VslVirtualNodeHandler {

    /**
     * The ID of the creator of the list.
     */
    private final String creatorID;

    /**
     * The minimum amount of entries a list must have.
     */
    private final int minEntries;

    /**
     * Constructor for a ListRootHandler.
     *
     * @param listRootAddress
     *            The address of the lists main/root node in the KOR. Needed in order to safely tell
     *            where the virtual Part of the address starts. (e.g. the first address element
     *            after this is the position in the list).
     * @param kor
     *            the ModelInstantiationHandler used for deleting a list element.
     * @param restrictions
     *            The restrictions of the list. .
     * @param creatorID
     *            The ID of the creator of the list. Only he can remove elements from the list.
     * @param nodeFactory
     *            The {@link VslNodeFactory} used to create VslNodes.
     */
    public ListDelHandler(final String listRootAddress, final VslKnowledgeRepository kor,
            final String creatorID, final String restrictions, final VslNodeFactory nodeFactory) {
        super(listRootAddress, kor, nodeFactory);
        this.minEntries = Restrictions.getMinListEntries(restrictions);
        this.creatorID = creatorID;
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        if (!address.startsWith(getListRootAddress() + "/del/")) {
            throw new ListOperationInvalidException(
                    "The given address isn't a valid addess for deleting nodes.");
        }
        // +5 for the /del/
        final String nodeName = address.substring(getListRootAddress().length() + 5,
                address.length());
        if (!identity.getAccessIDs().contains("system")
                && !identity.getAccessIDs().contains(creatorID)
                && !identity.getClientId().equals(creatorID)) {
            throw new ListOperationInvalidException(
                    "You don't have the necessary rights to remove the node " + nodeName
                            + " from the list. Required right: " + creatorID + " your rights: "
                            + identity.getAccessIDs().toString());
        }
        final List<String> elements = getListElements();

        // restriction checking
        if (elements.size() - 1 < minEntries) {
            throw new ListOperationInvalidException(
                    "The list already has its minimum amount of entries (" + minEntries
                            + ") Please add an entry and try again!");

        }

        if (!elements.contains(nodeName)) {
            throw new ListOperationInvalidException(
                    "The List doesn't contain an direct child  with relative address " + nodeName
                            + ". Make sure to use the Fully Qualified Name and not the position.");
        }

        getKor().removeNode(getListRootAddress() + "/" + nodeName);
        elements.remove(elements.indexOf(nodeName));

        getKor().set(getListRootAddress() + "/elements",
                getNodeFactory().createImmutableLeaf(
                        StringUtils.join(elements, VslNodeDatabase.LIST_SEPARATOR)),
                new ServiceIdentity(creatorID, VslNodeTree.SYSTEM_USER_ID));
        return getNodeFactory()
                .createImmutableLeaf("Removed " + nodeName + " from list " + getListRootAddress());
    }

}
