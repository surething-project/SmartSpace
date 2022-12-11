package org.ds2os.vsl.kor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.ds2os.vsl.core.config.VslKORDatabaseConfig;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.kor.dataStructures.MetaNode;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class OrientdbDatabase implements VslNodeDatabase {

    /**
     * Connection to server instance.
     */
    private OServerAdmin serverInstance;

    /**
     * Default URL of server instance.
     */
    private final String OrientURL = "remote:localhost";

    private OrientGraphFactory graphFactory;

    private final VslKORDatabaseConfig configService;

    public OrientdbDatabase(final VslKORDatabaseConfig configService) {
        this.configService = configService;
    }

    @Override
    public void activate() {
        try {
            this.serverInstance = new OServerAdmin(OrientURL).connect("admin", "123");
            if (!serverInstance.existsDatabase("vsldb", "plocal")) {
                serverInstance.createDatabase("vsldb", "graph", "plocal");
            }

            // think how to deal with situation when cluster already exists
            this.graphFactory = new OrientGraphFactory("remote:localhost/vsldb", "admin", "123");
            final OrientGraphNoTx graph = graphFactory.getNoTx();
            graph.command(new OCommandSQL("CREATE CLASS vslNode IF NOT EXISTS EXTENDS V"))
                    .execute();

            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.address STRING")).execute();
            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.value STRING")).execute();
            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.types EMBEDDEDLIST")).execute();
            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.readerIDs EMBEDDEDLIST"))
                    .execute();
            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.writerIDs EMBEDDEDLIST"))
                    .execute();
            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.restriction STRING")).execute();
            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.cacheParameters STRING"))
                    .execute();
            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.version LONG")).execute();
            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.archive EMBEDDEDMAP")).execute();
            graph.command(new OCommandSQL("CREATE PROPERTY vslNode.timestamp DATETIME")).execute();

            graph.shutdown();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addNode(final String address, final List<String> types, final List<String> readerIDs,
            final List<String> writerIDs, final String restriction, final String cacheParameters) {

        final Map<Long, String> archive = new TreeMap<Long, String>();
        final long version = -1L;
        archive.put(version, "");

        final OrientGraph graph = graphFactory.getTx();
        try {
            graph.command(new OCommandSQL(
                    "CREATE VERTEX vslNode SET address = ?, types = ?, readerIDs = ?, writerIDs = ?,"
                            + " restriction = ?, cacheParameters = ?, version = ?, archive = ?"))
                    .execute(address, types, readerIDs, writerIDs, restriction, cacheParameters,
                            -1L, archive);

            final String parentAddress = AddressParser.getParentAddress(address);
            final Iterable<Vertex> parentVertex = graph
                    .command(new OCommandSQL("SELECT FROM vslNode WHERE address = ?"))
                    .execute(parentAddress);
            if (parentVertex.iterator().hasNext()) {
                graph.command(
                        new OCommandSQL("CREATE EDGE FROM (SELECT FROM vslNode WHERE address = ?) "
                                + "TO (SELECT FROM vslNode WHERE address = ?) "))
                        .execute(address, parentAddress);
            }
            graph.commit();
        } catch (final Exception e) {
            graph.rollback();
        } finally {
            graph.shutdown();
        }

    }

    @Override
    public List<String> getAddressesOfType(final String rootAddress, final String type) {
        final OrientGraphNoTx graph = graphFactory.getNoTx();
        final List<String> addressesOfType = new ArrayList<String>();
        try {

            final Iterable<Vertex> queryResults = graph
                    .command(new OCommandSQL(
                            "SELECT * FROM vslNode WHERE types IN  [?] AND address LIKE ?"))
                    .execute(type, rootAddress + "%");

            for (final Vertex v : queryResults) {
                addressesOfType.add((String) v.getProperty("address"));
            }
        } catch (final Exception e) {
            System.out.println(e.getMessage());
        } finally {
            graph.shutdown();
        }
        return addressesOfType;
    }

    @Override
    public String getHashOfSubtree(final String rootAddress, final List<String> exludeSubtrees) {
        long result = 17;

        final String cmd = "SELECT FROM vslNode WHERE address LIKE '" + rootAddress + "%'";
        final List<String> exludeSubtreesFormated = makeSubtreeAddressAbsolute(rootAddress,
                exludeSubtrees);
        for (final String excludedAddress : exludeSubtreesFormated) {
            cmd.concat(" AND NOT (address LIKE '" + excludedAddress + "%')");
        }
        cmd.concat(" ORDER BY address ASC");

        final OrientGraphNoTx graph = graphFactory.getNoTx();
        try {
            final Iterable<Vertex> query = graph.command(new OCommandSQL(cmd)).execute();
            final Iterator<Vertex> results = query.iterator();
            while (results.hasNext()) {
                final Vertex currentVertex = results.next();

                final String currentAddress = currentVertex.getProperty("address");
                final List<String> currentTypes = (List<String>) currentVertex.getProperty("types");
                final String currentTypesFormated = StringUtils.join(currentTypes, LIST_SEPARATOR);

                result = 37 * result + currentAddress.hashCode();
                result = 37 * result + currentTypesFormated.hashCode();

            }
        } catch (final Exception e) {
            System.out.println(e.getMessage());
            graph.rollback();
        } finally {
            graph.shutdown();
        }

        return Long.toHexString(result);
    }

    private List<String> makeSubtreeAddressAbsolute(final String rootAddress,
            final List<String> exludeSubtrees) {
        final List<String> subtreesFormated = new ArrayList<String>();

        for (final String subtree : exludeSubtrees) {
            final String subtreeFormated = rootAddress + "/" + subtree;
            subtreesFormated.add(subtreeFormated);
        }

        return subtreesFormated;
    }

    @Override
    public TreeMap<String, MetaNode> getNodeMetaData(final String address, final boolean includeSubtree)
            throws NodeNotExistingException {
        if (!nodeExists(address)) {
            throw new NodeNotExistingException("node does not exist");
        }
        final OrientGraphNoTx graph = graphFactory.getNoTx();
        final TreeMap<String, MetaNode> metaNodes = new TreeMap<String, MetaNode>();
        try {
            final Iterable<Vertex> query = graph
                    .command(new OCommandSQL("SELECT FROM vslNode WHERE address = ?"))
                    .execute(address);
            if (query.iterator().hasNext()) {
                final Vertex rootNode = query.iterator().next();

                if (includeSubtree) {
                    final Iterable<Vertex> subtree = graph
                            .command(new OCommandSQL("SELECT FROM (TRAVERSE in() FROM ?)"))
                            .execute(rootNode.getId());
                    for (final Vertex v : subtree) {
                        final String currentNodeAdress = v.getProperty("address");
                        metaNodes.put(currentNodeAdress, createMetaNodeFromVertex(v));
                    }
                }
                metaNodes.put(address, createMetaNodeFromVertex(rootNode));
            } else {
                throw new NodeNotExistingException("node does not exist");
            }
        } catch (final Exception e) {
            System.out.println(e.getMessage());
        } finally {
            graph.shutdown();
        }
        return metaNodes;
    }

    private MetaNode createMetaNodeFromVertex(final Vertex v) {
        return new MetaNode((List<String>) v.getProperty("types"),
                (List<String>) v.getProperty("readerIDs"),
                (List<String>) v.getProperty("writerIDs"), (String) v.getProperty("restriction"),
                (String) v.getProperty("cacheParameters"));
    }

    @Override
    public boolean nodeExists(final String address) {
        final OrientGraphNoTx graph = graphFactory.getNoTx();
        try {
            final Iterable<Vertex> queryResults = graph
                    .command(new OCommandSQL("SELECT * FROM vslNode WHERE address = ?"))
                    .execute(address);
            if (queryResults.iterator().hasNext()) {
                return true;
            }
            return false;
        } catch (final Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    @Override
    public void removeNode(final String address) {
        final OrientGraph graph = graphFactory.getTx();
        try {
            graph.command(new OCommandSQL("DELETE VERTEX vslNode WHERE address like ?"))
                    .execute(address + "%");
        } catch (final Exception e) {
            graph.rollback();
        } finally {
            graph.shutdown();
        }

    }

    @Override
    public void setValueTree(final Map<String, String> values) throws NodeNotExistingException {

        final List<String> affectedNodes = new LinkedList<String>();

        // gather all affected nodes. Every node will be only counted once, even if he
        // is the parent of more then one changed node.
        for (final String address : values.keySet()) {
            if (!affectedNodes.contains(address)) {
                affectedNodes.add(address);
            }
            for (final String parent : AddressParser.getAllParentsOfAddress(address, 2)) {
                if (!affectedNodes.contains(parent)) {
                    affectedNodes.add(parent);
                }
            }
        }

        System.out.println(affectedNodes.toString());

        final OrientGraph graph = graphFactory.getTx();
        try {
            for (final String address : affectedNodes) {

                final Iterable<Vertex> query = graph
                        .command(new OCommandSQL("SELECT FROM vslNode WHERE address = ?"))
                        .execute(address);
                final Vertex currentNode = query.iterator().next();

                final Map<Long, String> archive = currentNode.getProperty("archive");

                if (values.containsKey(address)) {
                    final String valueToSet = values.get(address);

                    long currentVersion = currentNode.getProperty("version");
                    currentVersion++;
                    archive.put(currentVersion, valueToSet);

                    graph.command(new OCommandSQL(
                            "UPDATE vslNode SET value = ?, version = ?, archive = ? WHERE address = ?"))
                            .execute(valueToSet, currentVersion, archive, address);

                    graph.command(
                            new OCommandSQL("UPDATE vslNode SET timestamp = ? WHERE address = ?"))
                            .execute(new Date(), address);

                } else {
                    long currentVersion = currentNode.getProperty("version");
                    currentVersion++;
                    final String currentValue = currentNode.getProperty("value");
                    archive.put(currentVersion, currentValue);

                    graph.command(new OCommandSQL(
                            "UPDATE vslNode SET version = ?, archive = ? WHERE address = ?"))
                            .execute(currentVersion, archive, address);

                    graph.command(
                            new OCommandSQL("UPDATE vslNode SET timestamp = ? WHERE address = ?"))
                            .execute(new Date(), address);
                }

            }

            graph.commit();
        } catch (final Exception e) {
            System.out.println(e.getMessage());
            graph.rollback();
        } finally {
            graph.shutdown();
        }
    }

    @Override
    public void shutdown() {
        try {
            serverInstance.dropDatabase("vsldb", "plocal");
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void cacheVslNode(final String address, final VslNode nodes) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeCachedNode(final String address) {
        // TODO Auto-generated method stub

    }

    @Override
    public TreeMap<String, InternalNode> getNodeRecord(final String address, final VslAddressParameters params)
            throws NodeNotExistingException {

        final TreeMap<String, InternalNode> results = new TreeMap<String, InternalNode>();

        final int requestedDepth = params.getDepth();

        if (address == null) {
            throw new NodeNotExistingException("address was null");
        }

        final OrientGraphNoTx graph = graphFactory.getNoTx();

        try {
            Iterable<Vertex> vertices = null;

            switch (requestedDepth) {
            case -1:
                vertices = graph.command(new OCommandSQL(
                        "SELECT FROM (TRAVERSE in() FROM (SELECT FROM vslNode WHERE address = ?) STRATEGY DEPTH_FIRST)"))
                        .execute(address);
                break;
            case 0:
                vertices = graph.command(new OCommandSQL(
                        "SELECT FROM (TRAVERSE in() FROM (SELECT FROM vslNode WHERE address = ?) MAXDEPTH 0 STRATEGY DEPTH_FIRST)"))
                        .execute(address);
                break;
            default:
                vertices = graph.command(new OCommandSQL(
                        "SELECT FROM (TRAVERSE in() FROM (SELECT FROM vslNode WHERE address = ?) MAXDEPTH 1 STRATEGY DEPTH_FIRST)"))
                        .execute(address);
            }

            if (vertices != null) {
                final Iterator<Vertex> vslNodes = vertices.iterator();
                while (vslNodes.hasNext()) {
                    final Vertex currentNode = vslNodes.next();
                    results.put((String) currentNode.getProperty("address"),
                            constructInternalNode(currentNode, params));
                }
            }

        } catch (final Exception e) {
            System.out.println(e.getMessage());
        }

        if (results.isEmpty()) {
            throw new NodeNotExistingException("Node not found: " + address);
        }

        return results;
    }

    // Creates an InternalNode depending on parameters
    private InternalNode constructInternalNode(final Vertex vertex, final VslAddressParameters params) {
        InternalNode node;
        final int requestedVersion = params.getRequestedVersion();

        String value = vertex.getProperty("value");

        if (requestedVersion != -1) {
            final Map<Long, String> archive = vertex.getProperty("archive");
            value = archive.get(new Long(requestedVersion));

            // if (archive.size() - 1 < requestedVersion) {
            // value = archive.lastEntry().getValue();
            // }
        }

        final List<String> types = vertex.getProperty("types");

        final List<String> readerIDs = vertex.getProperty("readerIDs");
        final List<String> writerIDs = vertex.getProperty("writerIDs");

        final Map<Long, String> archive = vertex.getProperty("archive");

        final Long version = vertex.getProperty("version");

        final Date timestamp = vertex.getProperty("timestamp");
        final String restriction = vertex.getProperty("restriction");
        final String cacheParameters = vertex.getProperty("cacheParameters");

        switch (params.getNodeInformationScope()) {
        case VALUE:
            node = new InternalNode(new ArrayList<String>(), value, new ArrayList<String>(),
                    new ArrayList<String>(), -1l, null, "", "");
            break;
        case METADATA:
            node = new InternalNode(types, "", readerIDs, writerIDs, version, timestamp,
                    restriction, cacheParameters);
            break;
        default: // == case COMPLETE
            node = new InternalNode(types, value, readerIDs, writerIDs, version, timestamp,
                    restriction, cacheParameters);
        }
        return node;
    }
}
