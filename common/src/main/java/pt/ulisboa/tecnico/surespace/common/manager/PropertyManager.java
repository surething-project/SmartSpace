/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import pt.ulisboa.tecnico.surespace.common.manager.exception.PropertyManagerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class PropertyManager implements PropertyManagerInterface {
  private final LogManagerInterface logManager;
  private final Properties properties;

  public PropertyManager(LogManagerInterface logManager) {
    this.logManager = logManager;
    this.properties = new Properties();
  }

  @Override
  public void beforeLoading() throws PropertyManagerException {
    extend(getClass().getClassLoader().getResourceAsStream("common.properties"));
  }

  protected final void extend(InputStream inputStream) throws PropertyManagerException {
    if (inputStream != null) {
      try {
        Properties extendedProperties = new Properties();
        extendedProperties.load(inputStream);

        // Get the the new keys to resolve possible key collisions.
        Set<Map.Entry<Object, Object>> entries = extendedProperties.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
          Object key = entry.getKey();
          String newValue = entry.getValue().toString();

          if (properties.containsKey(key)) {
            String oldValue = properties.get(key).toString();
            if (oldValue.contains(",")) {
              logManager.warning("Appending '%s' to property '%s'", newValue, key);
              properties.put(key, String.join(",", oldValue, newValue));

            } else {
              logManager.warning("Overriding property '%s'", key);
              properties.put(key, newValue);
            }

          } else properties.put(key, newValue);
        }

      } catch (IOException e) {
        e.printStackTrace();
        throw new PropertyManagerException(e.getMessage());
      }

    } else logManager.error("[-] Could not extend properties from null input stream.");
  }

  public final Property get(String... keyPath) {
    String path = composePath(keyPath);
    return new Property(path, properties.getProperty(path));
  }

  public final boolean has(String... keyPath) {
    return properties.containsKey(composePath(keyPath));
  }

  protected final LogManagerInterface log() {
    return logManager;
  }

  public final Property set(String key, String value) {
    return new Property(key, properties.setProperty(key, value));
  }

  @Override
  public String toString() {
    return "PropertyManager{" + "logManager=" + logManager + ", properties=" + properties + '}';
  }

  @Override
  public final void unset(String... keyPath) {
    properties.remove(composePath(keyPath));
  }
}
