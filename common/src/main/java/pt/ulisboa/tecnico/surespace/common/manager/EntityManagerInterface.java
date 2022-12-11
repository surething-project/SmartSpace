/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.manager.exception.EntityManagerException;

import java.util.List;

public interface EntityManagerInterface extends ManagerInterface<EntityManagerException> {
  Entity current() throws EntityManagerException;

  void current(Entity entity);

  void forget(Entity entity) throws EntityManagerException;

  Entity getByPath(String path) throws EntityManagerException;

  boolean knows(Entity entity);

  List<Entity> list();

  void register(Entity entity);
}
