package org.ds2os.vsl.service.registry;

import org.apache.felix.framework.Felix;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;

public class ServiceRegistry implements BundleActivator, IServiceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);
    HashMap<String, String> configMap = new HashMap<>();
    BundleContext context;
    Felix osgiInstance;

    public ServiceRegistry() throws Exception {
        this.osgiInstance = new Felix(this.configMap);
        try {
            this.osgiInstance.init();
            this.osgiInstance.start();
            this.context = this.osgiInstance.getBundleContext();
        } catch (BundleException e) {
            e.printStackTrace();
        }
        removeBundles();
        printServices();
        start(this.context);
        printServices();
    }

    public BundleContext getContext() {
        return this.context;
    }

    public String[] getServices() {
        Bundle[] bundles = this.context.getBundles();
        String[] s = new String[1];
        return s;
    }

    public Bundle installServiceFromJar(String jarLocation) {
        try {
            Bundle bundle = this.context.installBundle(jarLocation);
            LOGGER.info("Installed Bundle with ID " + bundle.getBundleId());
            return bundle;
        } catch (BundleException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void printServices() {
        Bundle[] bundles = this.context.getBundles();
        for (Bundle b : bundles) {
            long id = b.getBundleId();
            ServiceReference[] srs = b.getRegisteredServices();
            LOGGER.info("Found Bundle with ID " + id + " | " + toStatusName(b.getState()) + " | " + b.getLocation());
            for (ServiceReference sr : srs) {
                String[] props = sr.getPropertyKeys();
                String out = "Found Service: ";
                for (String prop : props) {
                    if (prop == "objectClass") {
                        out = out + prop + ": " + Arrays.toString((Object[]) sr.getProperty(prop)) + " | ";
                    } else {
                        out = out + " | " + prop + ": " + sr.getProperty(prop);
                    }
                }
                LOGGER.info(out);
            }
        }
    }

    public void registerService(String className, Object serviceObject, Dictionary<String, ?> properties) {
        this.context.registerService(className, serviceObject, properties);
    }

    public void removeBundle(String bundleId) {
    }

    public void removeBundles() {
        Bundle[] bundles = this.context.getBundles();
        for (Bundle b : bundles) {
            try {
                b.uninstall();
                LOGGER.info("Uninstalled Bundle with ID " + b.getBundleId());
            } catch (BundleException e) {
                LOGGER.info("Bundle with ID " + b.getBundleId() + " could not be uninstalled.");
            }
        }
    }

    public void removeService() {
    }

    public void sendServiceToNode() {
    }

    public void start(BundleContext context) throws Exception {
        System.out.println("HelloWorldService registered");
    }

    public void stop(BundleContext context) throws Exception {
    }

    public String toStatusName(int statusCode) {
        String statusName = "";
        switch (statusCode) {
            case 1:
                statusName = "UNINSTALLED";
                break;
            case 2:
                statusName = "INSTALLED";
                break;
            case 4:
                statusName = "RESOLVED";
                break;
            case 8:
                statusName = "STARTING";
                break;
            case 16:
                statusName = "STOPPING";
                break;
            case 32:
                statusName = "ACTIVE";
                break;
        }
        return statusName;
    }
}
