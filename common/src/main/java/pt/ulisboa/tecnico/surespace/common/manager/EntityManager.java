/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.domain.exception.ObjectException;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityManager implements EntityManagerInterface {
  private final ConcurrentHashMap<String, Entity> entities;
  private final PropertyManagerInterface propertyManager;
  private Entity currentEntity;

  public EntityManager(PropertyManagerInterface propertyManager) {
    this.propertyManager = propertyManager;

    entities = new ConcurrentHashMap<>();
    afterLoading();
  }

  @Override
  public void afterLoading() {
    if (propertyManager != null && propertyManager.has("entity", "keys")) {
      String entityKeys = propertyManager.get("entity", "keys").asString();
      String[] splitKeys = entityKeys.split(",");

      for (String key : splitKeys) {
        if (propertyManager.has("entity", "known", key)) {
          String rawData = propertyManager.get("entity", "known", key).asString();
          String[] splitData = rawData.split(",");

          if (splitData.length >= 2) {
            String name = splitData[0];
            String path = splitData[1];

            try {
              register(new Entity(name, path));

            } catch (ObjectException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }

  @Override
  public void beforeLoading() {}

  @Override
  public Entity current() throws EntityManagerException {
    if (currentEntity == null) throw new EntityManagerException("Undefined current entity");
    return currentEntity.clone();
  }

  @Override
  public void current(Entity entity) {
    register(entity);
    this.currentEntity = entity.clone();
  }

  @Override
  public void forget(Entity entity) throws EntityManagerException {
    if (entities.remove(entity.getPath()) == null)
      throw new EntityManagerException("Unknown entity '%s'", entity);
  }

  @Override
  public Entity getByPath(String path) throws EntityManagerException {
    Entity entity = entities.get(path);
    if (entity == null) throw new EntityManagerException("Unknown entity with path '%s'", path);

    return entity;
  }

  @Override
  public boolean knows(Entity entity) {
    return entities.containsKey(entity.getPath());
  }

  @Override
  public List<Entity> list() {
    return new ArrayList<>(entities.values());
  }

  @Override
  public void register(Entity entity) {
    entities.putIfAbsent(entity.getPath(), entity.clone());
  }
}
