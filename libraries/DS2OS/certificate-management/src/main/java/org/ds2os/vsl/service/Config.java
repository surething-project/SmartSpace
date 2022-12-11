package org.ds2os.vsl.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public int agentNumber;
    public String agentUrl = null;
    public String deploymentSideChannelPort = null;
    public String networkInterface;
    public int nlsmNumber;
    public Map<String, String> parameters = new HashMap<>();

    public String s2storeUrl = null;
    public int serverPort;
    public String serviceKeystore = null;
    public int sheNumber;
    public String workingDirectory = null;

    public Config() {
    }

    public Config(String agentUrl, String serviceKeystore, String workingDirectory) {
        this.agentUrl = agentUrl;
        this.serviceKeystore = serviceKeystore;
        this.workingDirectory = workingDirectory;
    }

    public static Config parseConfig(String fileName) throws Exception {
        Path path = Paths.get(fileName);
        List<String> lines = Files.readAllLines(path);
        Config config = new Config();
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            String[] parts = line.split("=");
            if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty())
                throw new Exception("Syntax error in configuration file at line " + lineNo + ": " + line);
            if ("agentUrl".equals(parts[0])) {
                config.agentUrl = parts[1];
                continue;
            }
            if ("serviceKeystore".equals(parts[0])) {
                config.serviceKeystore = parts[1];
                continue;
            }
            if ("workingDirectory".equals(parts[0])) {
                config.workingDirectory = parts[1];
                continue;
            }
            if ("agentNumber".equals(parts[0])) {
                config.agentNumber = Integer.parseInt(parts[1]);
                continue;
            }
            if ("serverPort".equals(parts[0])) {
                config.serverPort = Integer.parseInt(parts[1]);
                continue;
            }
            if ("s2storeUrl".equals(parts[0])) {
                config.s2storeUrl = parts[1];
                continue;
            }
            if ("she".equals(parts[0])) {
                config.sheNumber = Integer.parseInt(parts[1]);
                continue;
            }
            if ("nlsm".equals(parts[0])) {
                config.nlsmNumber = Integer.parseInt(parts[1]);
                continue;
            }
            if ("deploymentSideChannelPort".equals(parts[0])) {
                config.deploymentSideChannelPort = parts[1];
                continue;
            }
            if ("networkInterface".equals(parts[0])) {
                config.networkInterface = parts[1];
                continue;
            }
            config.parameters.put(parts[0], parts[1]);
        }
        if (config.agentUrl == null)
            throw new Exception("agentUrl is a required configuration field");
        if (config.serviceKeystore == null)
            throw new Exception("serviceKeystore is a required configuration field");
        if (config.workingDirectory == null)
            throw new Exception("workingDirectory is a required configuration field");
        return config;
    }

    public boolean contains(String key) {
        if ("agentUrl".equals(key))
            return (this.agentUrl != null);
        if ("serviceKeystore".equals(key))
            return (this.serviceKeystore != null);
        if ("workingDirectory".equals(key))
            return (this.workingDirectory != null);
        return this.parameters.containsKey(key);
    }

    public String getParameter(String key) {
        if ("agentUrl".equals(key))
            return this.agentUrl;
        if ("serviceKeystore".equals(key))
            return this.serviceKeystore;
        if ("workingDirectory".equals(key))
            return this.workingDirectory;
        return this.parameters.get(key);
    }
}
