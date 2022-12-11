package org.ds2os.vsl.ka;

import java.util.HashMap;
import java.util.List;

import org.ds2os.vsl.core.AbstractRequestRouter;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.bridge.RequestHandlerToConnectorBridge;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters.NodeInformationScope;
import org.ds2os.vsl.exception.VslException;

import org.ds2os.vsl.sphinx.main.Sphinx;
import org.ds2os.vsl.sphinx.model.Access;


/**
 * This class is used to send the access descriptions to the sphinx or to save them.
 *
 * There are various methods to log the different type of accesses.
 * These methods do the preprocessing, then they call the logging method called: logConnection.
 *
 * @author francois
 */
public class AccessObserver {

    /** The connector instance used by this service.   */
   // private ServiceConnector connector;

    private AbstractRequestRouter connector;

    /** The name of the service. */
    private String serviceName = "system";

    /** The sphinx to which the access are sent. */
    private Sphinx sphinx;
    /** The agent ID. */
    private String agentID;

    /** Is the KA running a simulation. */
    boolean simulationMode = false;
    /** Is it running a demo. */
    boolean demoMode = false; //this is for a demo not needing service type, therefore we block it


    /**
     * A connector is needed to request some information about the services to the KA.
     */
    //String agentURL = "https://127.0.0.1:8081";
    //String connectorKeystore = ""+serviceName +".jks";

    /** A hash table to save the type of the service. */
    private HashMap<String, String> serviceTypeHash;
    /** A hash table to save the subscriptions. */
    private HashMap<String, String> subscribeHash;

    /**
     * The constructor.
     *
     * @param agentID the agentID
     * @param connector the connector for the requests
     */
    public AccessObserver(final String agentID, final AbstractRequestRouter connector) {
        this.connector = connector;

		serviceTypeHash = new HashMap<String, String>();
		subscribeHash = new HashMap<String, String>();

		this.agentID = agentID;
		if (!simulationMode) {

			final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
            final VslConnector sphinxConnector = new RequestHandlerToConnectorBridge(
                   connector, new ServiceIdentity("sphinx", "sphinx"), nodeFactory);

			sphinx = new Sphinx(agentID, sphinxConnector);
		}
    }

	/**
	 * Log a normal access: get or set.
	 * @param identity the id of the service
	 * @param operation the operation
	 * @param accessedAddress the accessed node
	 * @param node the node
	 * @return if the access is allowed
	 */
    public boolean logAccess(final VslIdentity identity, final String operation,
    		final String accessedAddress, final VslNode node) {

        // if this method is responsible for the request, then we don't want to log it
        if (identity.getClientId().equals(serviceName)) {
        	return true;
        }

		String value = node.getValue();
		String valueTimestamp = "none";
		try {
			valueTimestamp = Double.toString(node.getTimestamp().getTime());
		} catch (Exception e) {
		}

		String addressAccessing = getAddressFromID(identity.getClientId());
		String accessedServiceAddress = splitAddresses(accessedAddress)[0];

        //Finding out the type of the accessed node and the accessing node:
		String accessingType = getTypeFromAddress(addressAccessing);
		String accessedServiceType = getTypeFromAddress(accessedServiceAddress);
		String accessedNodeType = getTypeFromAddress(accessedAddress);

		if (operation.equals("read")) {
			if (isHiddenVirtualNode(accessedAddress)) {
		    	String[] parts = accessedAddress.split("/");
				value = parts[parts.length - 1];
			}
		}

		return logConnection(identity.getClientId(), addressAccessing, accessingType,
				accessedServiceAddress, accessedServiceType,
				accessedAddress, accessedNodeType, operation, value, valueTimestamp);
    	}

    /**
     * Log subscriptions.
     * @param identity the id of the service
     * @param operation the operation
     * @param address the subscribed node
     * @param value the values of the subscription
     * @return if the access is allowed
     */
    public boolean logSubscriptions(final VslIdentity identity, final String operation,
    		final String address, final String value) {
    	if (operation.equals("subscribe")) {
    		//subscribeList.add(new TuppleSub(identity.getClientId(),address));
    		subscribeHash.put(address, identity.getClientId());
    	} else if (operation.equals("unsubscribe")) {
    		subscribeHash.remove(address);	//,identity.getClientId()
    	}
		String addressAccessing = getAddressFromID(identity.getClientId());
		String accessedServiceAddress = splitAddresses(address)[0];

        //Finding out the type of the accessed node and the accessing node:
		String accessingType = getTypeFromAddress(addressAccessing);
		String accessedServiceType = getTypeFromAddress(accessedServiceAddress);
		String accessedNodeType = getTypeFromAddress(address);

		return logConnection(identity.getClientId(), addressAccessing, accessingType,
				accessedServiceAddress, accessedServiceType,
    			address, accessedNodeType, operation, value, "none");

    }

    /**
     * Log the rest.
     * @param identity the id of the service
     * @param operation the operation
     * @param address the accessed address
     * @return if the access is allowed
     */
    public boolean logVariousStuff(final VslIdentity identity,
    		final String operation, final String address) {

    	if (operation.equals("registerVirtualNode")) {
    		serviceTypeHash.put(address, "VirtualNode");
    	} else if (operation.equals("unregisterVirtualNode")) {
    		serviceTypeHash.remove(address);
    	}
    	String addressAccessing = getAddressFromID(identity.getClientId());
		String accessedServiceAddress = splitAddresses(address)[0];

        //Finding out the type of the accessed node and the accessing node:
		String accessingType = getTypeFromAddress(addressAccessing);
		String accessedServiceType = getTypeFromAddress(accessedServiceAddress);
		String accessedNodeType = getTypeFromAddress(address);

		return logConnection(identity.getClientId(), addressAccessing, accessingType,
				accessedServiceAddress, accessedServiceType,
    			address, accessedNodeType, operation, "none", "none");
    }


    /**
     * Logs call backs.
     *
     * @param changedAddress the addressed changed
     * @return if the access is allowed
     */
    public boolean logCallBack(final String changedAddress) {
    	//the best solution for the callbacks that was found until now
    	//should be better?: -> log the real connections not the ones that should be
    	//	-> have it in the correct way: origin address the changed node (?)

    	if (subscribeHash.containsKey(changedAddress)) {
    		String notifyID = subscribeHash.get(changedAddress);
    		String accessingAddress = getAddressFromID(notifyID);
			String[] adrParts = splitAddresses(changedAddress);

			return logConnection(notifyID, accessingAddress,
			getTypeFromAddress(accessingAddress), adrParts[0],
			getTypeFromAddress(adrParts[0]), changedAddress, getTypeFromAddress(changedAddress),
			"notifyCallback", "none", "none");

    	} else {
    		return true;
    	}

    }


    /**
     * This is the function that logs a connection. Be it saving in the file
	 *	or giving it to the online running sphinx.
     *
     * It recieves one string for every feature that is to be saved.
     * 	"none" in case this feature is not available for this connection.
     * 	"nk" in case it is not known.
     *
     * One connection log is of the form:		(R) stands for required
     * @param accessingID			(R) The ID of the service accessing
     * @param accessingAddress		The extracted address of the accessing node
     * @param accessingType			The type of this node
     * @param accessedServiceAddress the address of the service containing the accessed node
     * @param accessedServiceType	the type of this service
     * @param accessedNodeAddress	(R) the address that is accessed
     * @param accessedNodeType		the type of this address
     * @param operation				(R) the operation performed
     * @param value					the value exchanged
     * 		@param valueTimestamp //seems always to be "none" so not logged
     */
    private boolean logConnection(final String accessingID, final String accessingAddress, String accessingType,
    		final String accessedServiceAddress, final String accessedServiceType,
    		final String accessedNodeAddress, final String accessedNodeType,
    		final String operation, final String value, final String valueTimestamp) {

		String fileName = "./loggedAccesses.csv";		//the file to save access descriptions

		String replaceComma = ";";
		String replaceLineBreak = "";
		String connectionLog = "";

		String sourceLocation = getLocationFromAddress(accessingAddress);
		String destinationLocation = getLocationFromAddress(accessedServiceAddress);

		String[] featureArray = {accessingID, accessingAddress, accessingType, sourceLocation,
		accessedServiceAddress, accessedServiceType, destinationLocation, accessedNodeAddress,
		accessedNodeType, operation, value, Long.toString(System.currentTimeMillis()), "none"};

		//remove last comma:
		for (String feature : featureArray) {
			if (feature == null) {
				feature = "none";
			}
			connectionLog += feature.replace("\n", replaceLineBreak).replace("\r", replaceLineBreak)
					.replace(",", replaceComma) + ",";
		}
		connectionLog = connectionLog.substring(0, connectionLog.length() - 1);
		connectionLog += "\n";


		// If we want to save the accesses instead of sending them to the Sphinx:
		/*
		try {
			// right now we dont really want to log everything,
			 * just the service behavior, not the system one
			boolean dontLog = connectionLog.contains("sphinx") ||
			connectionLog.contains("system") || connectionLog.contains("search")
					|| connectionLog.contains("/agent1/typeSearch")
					|| connectionLog.contains("/agent1/slmr");

			if(!dontLog){
				Files.write(Paths.get(fileName), connectionLog.getBytes(),
				 StandardOpenOption.APPEND);
			}
        }catch (IOException e) {
        	System.out.println("error");
        } */

		//long measureStartTime = System.nanoTime(); //System.currentTimeMillis();
		boolean toreturn = sphinx.accessAnalyser(new Access(connectionLog));
		//long duration = System.nanoTime() - measureStartTime;

		return toreturn;

    }

    //....................................     Helper methods     .................................

    // find out how the location of services is encoded and if it is encoded
    /** This should be used to find out the location of a service.
     * @param address the address of the service
     * @return the location
     */
    private String getLocationFromAddress(final String address) {

		return "room1";
    }

    /** The number of errors. */
    private int numberOfErrors = 0;

    /**
     * This methode returns the type of the service at a given address.
     * If it cannot be found it returns "nk" (not known)
     *
     * @param address the asked address
     * @return the type
     */
    private String getTypeFromAddress(final String address) {
    	String type = "nk";


    	if (address.equals("none")) {
    		return "none";
    	}
    	//if this tupple is known just give it
    	if (serviceTypeHash.containsKey(address)) {
    		return serviceTypeHash.get(address);
    	}

    	//else ask for the type and save it in the list
		List<String> typeList;

		boolean askForTyp = !address.contains("sphinx") && !address.contains("echidna");
		if (askForTyp) {
			try {
				typeList = connector.get(address, (VslIdentity) new AddressParameters()
						.withNodeInformationScope(NodeInformationScope.METADATA)).getTypes();
				//System.out.println(typeList);
				type = typeList.get(0);
				serviceTypeHash.put(address, type);
			} catch (VslException e) {
				numberOfErrors++;
				//e.printStackTrace();
			} catch (NullPointerException  e) {
				serviceTypeHash.put(address, "none");
			} catch (Exception e) {
				//System.out.println("Well poop..");
			}
		}

    	return type;
    }


    /**
     *
     * @param identity the id
     * @return  the corresponding address
     */
    private String getAddressFromID(final String identity) {
    	// right now it only works for services finishing with the number of the agent they are on.
    	String address = "none";

		//only case /agent1/service1 because accessing (no case /agent1/service1/value)
		int numberOfService = -1;

    	if ((identity.charAt(identity.length() - 2) - 48) < 10
    			&& (identity.charAt(identity.length() - 2) - 48) > -1) {
        	numberOfService = 10 * (identity.charAt(identity.length() - 2) - 48)
        			+ identity.charAt(identity.length() - 1) - 48;
        } else if ((identity.charAt(identity.length() - 1) - 48) < 10
        		&& (identity.charAt(identity.length() - 1) - 48) > -1) {
        	numberOfService = identity.charAt(identity.length() - 1) - 48;
        }

		if (numberOfService != -1) {
			address = "/agent" + numberOfService + "/" + identity;
		} else {
			address = "/agent1/" + identity; //identity.getClientId();
		}
		// this is only true because serviceX on agentX runs

    	return address;
    }


    /**
     * This function splits an address and returns the service address on one side and
     * the the node in the service on the other.
     * @param address the address to split
     * @return the splited address
     */
    private static String[] splitAddresses(final String address) {
    	String serviceAddress = "none";
    	String nodeAddress = "none";

    	if (address.equals("none")) {
    		String[] a = {"none", "none"}; return a;
    	}

    	String[] parts = address.split("/");

    	if (parts.length < 3) {
    		serviceAddress = "/" + parts[1];
    	} else if (parts.length < 4) {
    		serviceAddress = "/" + parts[1] + "/" + parts[2];
    	} else {
    		serviceAddress = "/" + parts[1] + "/" + parts[2];
    		nodeAddress = "";
    		for (int i = 3; i < parts.length; i++) {
    			nodeAddress += "/" + parts[i];
    		}
    	}
    	String[] a = {serviceAddress, nodeAddress};
    	return (a);
    }


    /**
     * This methods gives if the given address is a read request on a virtual node
     * in the form /agent/virtualnode/parameter.
     *
     * still needs to be tested
     * @param address the address of the node
     * @return true if virtual node
     */
    private boolean isHiddenVirtualNode(final String address) {
    	String[] parts = address.split("/");
    	String realAddress = "";
    	for (int i = 1; i < parts.length - 1; i++) {
    		realAddress += "/" + parts[i];
    	}
    	if (serviceTypeHash.containsKey(realAddress)) {
    		return serviceTypeHash.get(realAddress).equals("VirtualNode");
    	}
    	return false;
    }

}


