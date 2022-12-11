package org.ds2os.vsl.service;

import org.ds2os.vsl.connector.ServiceConnector;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;

public abstract class Ds2osServiceRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ds2osServiceRunner.class);
    protected Config config;
    protected ServiceConnector connector;
    protected String knowledgeRoot;
    protected String workingDirectory;

    public Ds2osServiceRunner(VslServiceManifest manifest, Config config) throws Exception {
        this.connector = new ServiceConnector(config.agentUrl, config.serviceKeystore, "K3yst0r3");
        this.connector.activate();
        this.knowledgeRoot = this.connector.registerService(manifest);
        this.workingDirectory = config.workingDirectory;
        this.config = config;
        Path path = Paths.get(this.workingDirectory);
        if (Files.notExists(path))
            Files.createDirectories(path);
        registerSubscriptions();
        registerVirtualNodeHandlers();
    }

    public Ds2osServiceRunner(VslServiceManifest manifest, String keystore) throws Exception {
        this(manifest, new Config(keystore, "https://127.0.0.1:8081", "."));
    }

    public String getAgent() {
        return this.knowledgeRoot.split("/")[1];
    }

    public String getServiceName() {
        String serviceAddr = this.connector.getRegisteredAddress();
        return serviceAddr.substring(serviceAddr.lastIndexOf("/") + 1);
    }

    public boolean isRunning() {
        return (this.knowledgeRoot != null);
    }

    public abstract void registerSubscriptions() throws VslException;

    public abstract void registerVirtualNodeHandlers() throws VslException;

    public abstract void run() throws Exception;

    public void shutdown() {
        if (isRunning()) {
            LOGGER.info("Shutting down " + getServiceName() + "...");
            this.connector.shutdown();
            this.knowledgeRoot = null;
        } else {
            LOGGER.warn("Cannot shut down " + getServiceName() + ". The service is not running anymore already");
        }
    }
}
