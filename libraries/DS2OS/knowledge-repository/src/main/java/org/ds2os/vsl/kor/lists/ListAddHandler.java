package org.ds2os.vsl.kor.lists;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.ListOperationInvalidException;
import org.ds2os.vsl.exception.NodeAlreadyExistingException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.Restrictions;
import org.ds2os.vsl.kor.VslKnowledgeRepository;
import org.ds2os.vsl.kor.VslNodeDatabase;
import org.ds2os.vsl.kor.VslNodeTree;

/**
 * This Handler is called when the add operation/virtualNode of the List is called it was registered
 * for.
 *
 * @author liebald
 */
public class ListAddHandler extends ListHandler implements VslVirtualNodeHandler {

    /**
     * The IDs that can add node to the list.
     */
    private final List<String> accessIDs;

    /**
     * The allowed Types that can be added to the List.
     */
    private final List<String> allowedTypes;

    /**
     * The ID of the creator of the list.
     */
    private final String creatorID;

    /**
     * The maximum amount of entries allowed in the list.
     */
    private final int maxEntries;

    /**
     * Constructor for a ListRootHandler.
     *
     * @param listRootAddress
     *            The address of the lists main/root node in the KOR. Needed in order to safely tell
     *            where the virtual Part of the address starts. (e.g. the first address element
     *            after this is the position in the list).
     * @param kor
     *            the ModelInstantiationHandler used for adding a model as list element.
     * @param creatorID
     *            The ID of the creator of the list.
     * @param restrictions
     *            The restrictions of the list.
     * @param accessIDs
     *            the IDs that can add nodes.
     * @param nodeFactory
     *            The {@link VslNodeFactory} used to create VslNodes.
     */
    public ListAddHandler(final String listRootAddress, final VslKnowledgeRepository kor,
            final String creatorID, final String restrictions, final List<String> accessIDs,
            final VslNodeFactory nodeFactory) {
        super(listRootAddress, kor, nodeFactory);
        this.creatorID = creatorID;
        this.maxEntries = Restrictions.getMaxListEntries(restrictions);
        this.allowedTypes = Restrictions.getAllowedListTypes(restrictions);
        this.accessIDs = accessIDs;

        if (!this.accessIDs.contains(VslNodeTree.SYSTEM_USER_ID)) {
            this.accessIDs.add(0, VslNodeTree.SYSTEM_USER_ID);
        }
        if (!this.accessIDs.contains(creatorID)) {
            this.accessIDs.add(0, creatorID);
        }

    }

    /**
     * Checks if one of the provided adderIds has the rights to add new nodes.
     *
     * @param adderIds
     *            IDs to check.
     * @return True if at least one ID matches, false if not.
     */
    private boolean canAddNode(final Collection<String> adderIds) {

        if (accessIDs.contains("*")) {
            return true;
        }
        for (final String id : accessIDs) {
            if (adderIds.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a random String of charactes (a-zA-Z) with length len.
     *
     * @param len
     *            of the resulting String.
     * @return A random String of characters of length n.
     */
    private String generateNodeName(final int len) {
        final char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        final StringBuilder sb = new StringBuilder();
        final Random random = new Random();
        for (int i = 0; i < len; i++) {
            final char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        if (!address.startsWith(getListRootAddress() + "/add/")) {
            // shouldn't happen if the handlers are registered correctly
            throw new ListOperationInvalidException(
                    "The given address isn't a valid addess for adding nodes.");
        }

        if (!canAddNode(identity.getAccessIDs())) {
            throw new ListOperationInvalidException(
                    "You don't have the necessary rights to add a node to the list.");
        }
        final List<String> elements = getListElements();

        if (elements.size() + 1 > maxEntries) {
            throw new ListOperationInvalidException(
                    "The list already has its maximum amount of entries (" + maxEntries
                            + ") Please remove an entry and try again!");

        }

        // +4 for the /add
        String suffix = address.substring(getListRootAddress().length() + 4, address.length());
        // extract the list element name or generate the new random name if none was included.
        String newNodeName;
        if (suffix.contains("//")) {
            newNodeName = suffix.split("//", 2)[1];
            if (!newNodeName.matches("^[a-zA-Z0-9\\.\\-\\_:]+$")) {
                throw new ListOperationInvalidException(
                        "Invalid custom nodename for new list element: " + newNodeName
                                + " . The name must match the regex: [a-zA-Z0-9\\.\\-\\_:]+");
            }
            suffix = suffix.split("//", 2)[0];

            if (elements.contains(newNodeName)) {
                throw new NodeAlreadyExistingException(
                        "The name you chose for the new list element (" + newNodeName
                                + ") was already assigned to another element.");
            }
        } else {
            newNodeName = generateNodeName(10);
            while (elements.contains(newNodeName)) {
                newNodeName = generateNodeName(10);
            }
        }

        String modelToAdd;
        int position;
        try {
            if (suffix.substring(1).contains("/")) {
                // necessary in case the type only contains one /, e.g. /type instead of
                // /type/subtype. In this case we assume no position is given.
                position = Integer.parseInt(suffix.substring(1, suffix.indexOf("/", 1)));
                modelToAdd = suffix.substring(suffix.indexOf("/", 1));
            } else {
                throw new NumberFormatException();
            }
        } catch (final NumberFormatException e) {
            position = elements.size(); // on error add the new element at the end of the list.
            modelToAdd = suffix; // if no number was given, the complete suffix is assumed to be the
                                 // modelID
        }
        // restriction checking

        if (allowedTypes != null && !allowedTypes.contains(modelToAdd)) {
            throw new ListOperationInvalidException(
                    "Only models of the following types can be added to this list: "
                            + allowedTypes.toString() + " The type " + modelToAdd
                            + " is not allowed!");
        }
        // add model
        getKor().addSubtreeFromModelID(getListRootAddress(), modelToAdd, creatorID, newNodeName);

        if (position > elements.size()) {
            position = elements.size();
        } else if (position < 0) {
            position = 0;
        }

        // set the elements string
        elements.add(position, newNodeName);
        getKor().set(getListRootAddress() + "/elements",
                getNodeFactory().createImmutableLeaf(
                        StringUtils.join(elements, VslNodeDatabase.LIST_SEPARATOR)),
                new ServiceIdentity(creatorID, VslNodeTree.SYSTEM_USER_ID));

        return getNodeFactory().createImmutableLeaf(getListRootAddress() + "/" + newNodeName);
    }
}
