/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.domain;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import pt.ulisboa.tecnico.surespace.common.domain.exception.ObjectException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Entity extends Object<Entity> {
  public static final String PATH_PREFIX = "surespace://";
  private static final long serialVersionUID = 4996326800927368411L;
  private final String descriptiveUId;
  private final String name;
  private final String path;
  private final String type;

  public Entity(String name, String path) throws ObjectException {
    if (name == null || path == null)
      throw new IllegalArgumentException("Name and path must be non-null");

    // Check name.
    if (StringUtils.isBlank(name))
      throw new IllegalArgumentException("Blank names are not allowed");
    this.name = name;

    // Check path.
    if (!path.startsWith(PATH_PREFIX))
      throw new IllegalArgumentException("Path must start with '" + PATH_PREFIX + "'");
    this.path = path;

    // Remove prefix.
    String[] splitPath = path.substring(PATH_PREFIX.length()).split("/");
    int pathLength = splitPath.length;
    if (pathLength < 2) {
      throw new IllegalArgumentException("Invalid path '" + path + "'");

    } else if (pathLength == 2) {
      this.type = splitPath[pathLength - 1];

    } else {
      this.type = splitPath[pathLength - 2];
    }

    try {
      byte[] digestBytes = MessageDigest.getInstance("SHA-512").digest(path.getBytes(UTF_8));
      this.descriptiveUId = type + "_" + new String(Hex.encodeHex(digestBytes));

    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      throw new ObjectException(this, e.getMessage());
    }
  }

  @Override
  public Entity clone() {
    return this;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (!(o instanceof Entity)) return false;
    Entity stEntity = (Entity) o;
    return path.equals(stEntity.path);
  }

  public String getDescriptiveUId() {
    return descriptiveUId;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public String getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public String toString() {
    return "Entity{"
        + "descriptiveUId='"
        + descriptiveUId
        + '\''
        + ", name='"
        + name
        + '\''
        + ", path='"
        + path
        + '\''
        + ", type='"
        + type
        + '\''
        + '}';
  }
}
