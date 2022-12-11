package org.ds2os.vsl.kor;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.nin;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Projections.computed;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.ds2os.vsl.core.config.VslKORDatabaseConfig;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.kor.dataStructures.MetaNode;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;

public class MongodbDatabase implements VslNodeDatabase {

    /**
     * The configuration service.
     */
    private final VslKORDatabaseConfig configService;

    /**
     * Connection to MongoDB service.
     */
    private MongoClient mongoClient;

    /**
     * Default connection address of MongoDB.
     * <p>
     * TODO: Make mongoClientURI configurable
     * </p>
     */
    private final MongoClientURI mongoClientURI = new MongoClientURI("mongodb://localhost:27017");

    /**
     * The MongoDb instance.
     */
    private MongoDatabase vslDatabase;

    private MongoCollection<Document> currentCollection;
    private MongoCollection<Document> archiveCollection;

    public MongodbDatabase(final VslKORDatabaseConfig configService) {
        this.configService = configService;
    }

    @Override
    public void activate() {
        this.mongoClient = new MongoClient(mongoClientURI);
        this.vslDatabase = mongoClient.getDatabase("vsldb");

        if (!collectionExists("current")) {
            vslDatabase.createCollection("current", new CreateCollectionOptions().autoIndex(false));
        }

        if (!collectionExists("archive")) {
            vslDatabase.createCollection("archive", new CreateCollectionOptions().autoIndex(false));
        }

        this.currentCollection = vslDatabase.getCollection("current");
        currentCollection.createIndex(Indexes.ascending("address"),
                new IndexOptions().unique(true));
        this.archiveCollection = vslDatabase.getCollection("archive");
        archiveCollection.createIndex(Indexes.ascending("address", "version"),
                new IndexOptions().unique(true));
    }

    /**
     * Helper method.
     */
    private boolean collectionExists(final String collectionName) {
        final MongoIterable<String> collectionNames = vslDatabase.listCollectionNames();
        for (final String name : collectionNames) {
            if (name.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addNode(final String address, final List<String> types, final List<String> readerIDs,
            final List<String> writerIDs, final String restriction, final String cacheParameters) {
        final Document newNode = new Document("address", address)
                .append("ancestors", AddressParser.getAllParentsOfAddress(address))
                .append("parent", AddressParser.getParentAddress(address)).append("types", types)
                .append("readerIDs", readerIDs).append("writerIDs", writerIDs)
                .append("restriction", restriction).append("cacheParameters", cacheParameters);
        if (nodeExists(address)) {
            currentCollection.updateOne(eq("address", address), new Document("$set", newNode));
        } else {
            newNode.append("version", -1L);
            currentCollection.insertOne(newNode);
        }
    }

    @Override
    public List<String> getAddressesOfType(String rootAddress, final String type) {

        final List<String> addressesOfType = new ArrayList<String>();

        if (rootAddress.length() > 1 && rootAddress.endsWith("/")) {
            rootAddress = rootAddress.substring(0, rootAddress.length() - 1);
        }

        final MongoCursor<Document> cursor = currentCollection
                .find(and(in("ancestors", rootAddress), in("types", type))).iterator();
        while (cursor.hasNext()) {
            final Document currentDocument = cursor.next();
            addressesOfType.add(currentDocument.getString("address"));
        }
        return addressesOfType;
    }

    @Override
    public String getHashOfSubtree(final String rootAddress, final List<String> exludeSubtrees) {

        long result = 17;

        final Bson sortParameter = Sorts.ascending("address");

        final List<String> exludeSubtreesFormated = makeSubtreeAddressAbsolute(rootAddress,
                exludeSubtrees);

        final Bson excludedSubtrees = Filters.and(nin("ancestors", exludeSubtreesFormated),
                nin("address", exludeSubtreesFormated));
        final Bson searchParameter = Filters.and(
                or(in("ancestors", rootAddress), eq("address", rootAddress)), excludedSubtrees);

        final MongoCursor<Document> cursor = currentCollection.find(searchParameter).sort(sortParameter)
                .iterator();

        while (cursor.hasNext()) {
            final Document currentDocument = cursor.next();
            final String currentAddress = currentDocument.getString("address");
            final List<String> currentTypes = (List<String>) currentDocument.get("types");
            final String currentTypesFormated = StringUtils.join(currentTypes, LIST_SEPARATOR);

            result = 37 * result + currentAddress.hashCode();
            result = 37 * result + currentTypesFormated.hashCode();
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
        final TreeMap<String, MetaNode> results = new TreeMap<String, MetaNode>();
        final Document rootNode = currentCollection.find(eq("address", address)).first();
        if (rootNode == null) {
            throw new NodeNotExistingException("node does not exist");
        }
        final MetaNode rootNodeMetadata = new MetaNode((List<String>) rootNode.get("types"),
                (List<String>) rootNode.get("readerIDs"), (List<String>) rootNode.get("writerIDs"),
                rootNode.getString("restriction"), rootNode.getString("cacheParameters"));
        results.put(address, rootNodeMetadata);

        if (includeSubtree) {
            final MongoCursor<Document> cursor = currentCollection.find(in("ancestors", address))
                    .iterator();
            while (cursor.hasNext()) {
                final Document currentDocument = cursor.next();
                results.put(currentDocument.getString("address"),
                        new MetaNode((List<String>) currentDocument.get("types"),
                                (List<String>) currentDocument.get("readerIDs"),
                                (List<String>) currentDocument.get("writerIDs"),
                                currentDocument.getString("restriction"),
                                currentDocument.getString("cacheParameters")));
            }
        }
        return results;
    }

    @Override
    public boolean nodeExists(final String address) {
        if (currentCollection.find(eq("address", address)).first() == null) {
            return false;
        }
        return true;
    }

    @Override
    public void removeNode(final String address) {
        currentCollection.deleteMany(in("ancestors", address));
        currentCollection.deleteOne(eq("adress", address));
        archiveCollection.deleteMany(in("ancestors", address));
        archiveCollection.deleteOne(eq("adress", address));

    }

    @Override
    public void setValueTree(final Map<String, String> values) throws NodeNotExistingException {
        if (values.size() == 0) {
            return;
        }

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

        affectedNodes.removeAll(values.keySet());

        // Iterating over valueTree, archiving the old nodes,
        // setting new values and incrementing versions of affected nodes.
        for (final Entry<String, String> entry : values.entrySet()) {
            final Document currentDocument = currentCollection.find(eq("address", entry.getKey()))
                    .first();
            archiveCollection.insertOne(currentDocument);

            currentCollection.updateOne(eq("address", entry.getKey()),
                    combine(set("value", entry.getValue()), set("timestamp", new Date()),
                            inc("version", 1)));
        }

        // incrementing version of ancestor nodes
        for (final String entry : affectedNodes) {
            final Document currentDocument = currentCollection.find(eq("address", entry)).first();
            archiveCollection.insertOne(currentDocument);
            currentCollection.updateOne(eq("address", entry), inc("version", 1));
        }
    }

    @Override
    public void shutdown() {
        vslDatabase.drop();

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
        MongoCollection<Document> chosenCollection = currentCollection;
        final List<Bson> appliedFilters = new ArrayList<Bson>();
        if (address == null) {
            throw new NodeNotExistingException("address was null");
        }

        final int requestedVersion = params.getRequestedVersion();
        final int requestedDepth = params.getDepth();

        if (requestedVersion != -1) {
            chosenCollection = archiveCollection;
            filterRequestedVersion(appliedFilters, requestedVersion);
        }

        final Document rootNode = chosenCollection.find(eq("address", address)).first();
        if (rootNode == null) {
            throw new NodeNotExistingException("node does not exist");
        }

        filterRequestedDepth(appliedFilters, requestedDepth, address);

        final MongoCursor<Document> cursor = chosenCollection.aggregate(appliedFilters).iterator();

        while (cursor.hasNext()) {
            final Document currentDocument = cursor.next();
            results.put(currentDocument.getString("address"),
                    constructInternalNode(currentDocument, params));
        }

        if (results.isEmpty()) {
            throw new NodeNotExistingException("Node not found: " + address);
        }
        return results;
    }

    /**
     * Adds filters to aggregation pipeline, which are needed to retrieve requested version of a
     * node from "archive" collection
     * 
     * @param appliedFilters
     * @param requestedVersion
     */

    private void filterRequestedVersion(final List<Bson> appliedFilters, final int requestedVersion) {

        appliedFilters.add(Aggregates.match(lte("version", requestedVersion)));
        appliedFilters.add(Aggregates.sort(Sorts.ascending("address", "version")));
        appliedFilters.add(Aggregates.group("$address", Accumulators.last("types", "$types"),
                Accumulators.last("readerIDs", "$readerIDs"),
                Accumulators.last("writerIDs", "$writerIDs"),
                Accumulators.last("restriction", "$restriction"),
                Accumulators.last("cacheParameters", "$cacheParameters"),
                Accumulators.last("version", "$version"), Accumulators.last("value", "$value"),
                Accumulators.last("timestamp", "$timestamp"),
                Accumulators.last("ancestors", "$ancestors"), Accumulators.last("parent", "$parent")

        ));
        appliedFilters
                .add(Aggregates.project(fields(computed("address", "$_id"),
                        include("types", "readerIDs", "writerIDs", "restriction", "cacheParameters",
                                "version", "value", "timestamp", "ancestors", "parent"),
                        exclude("_id"))));
    }

    private void filterRequestedDepth(final List<Bson> appliedFilters, final int requestedDepth,
            final String address) {
        switch (requestedDepth) {
        case -1:
            appliedFilters
                    .add(Aggregates.match(or(eq("address", address), in("ancestors", address))));
            break;
        case 0:
            appliedFilters.add(Aggregates.match(eq("address", address)));
            break;
        case 1:
            appliedFilters.add(Aggregates.match(or(eq("address", address), eq("parent", address))));
            break;
        }
    }

    // Creates an InternalNode depending on parameters
    private InternalNode constructInternalNode(final Document mongoDocument,
            final VslAddressParameters params) {
        InternalNode node;

        final List<String> types = (List<String>) mongoDocument.get("types");
        final String value = mongoDocument.getString("value");
        final List<String> readerIDs = (List<String>) mongoDocument.get("readerIDs");
        final List<String> writerIDs = (List<String>) mongoDocument.get("writerIDs");
        final Long version = mongoDocument.getLong("version");
        final Date timestamp = mongoDocument.getDate("timestamp");
        final String restriction = mongoDocument.getString("restriction");
        final String cacheParameters = mongoDocument.getString("cacheParameters");

        switch (params.getNodeInformationScope()) {
        case VALUE:
            node = new InternalNode(new ArrayList<String>(), value, new ArrayList<String>(),
                    new ArrayList<String>(), -1l, null, "", "");
            break;
        case METADATA:
            node = new InternalNode(types, "", readerIDs, writerIDs, version,
                    timestamp, restriction, cacheParameters);
            break;
        default: // == case COMPLETE
            node = new InternalNode(types, value, readerIDs, writerIDs, version,
                    timestamp, restriction, cacheParameters);
            break;
        }
        return node;
    }

}
