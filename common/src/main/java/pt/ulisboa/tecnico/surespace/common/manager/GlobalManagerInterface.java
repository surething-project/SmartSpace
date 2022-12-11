/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

public interface GlobalManagerInterface {
  EntityManagerInterface entity();

  KeyStoreManagerInterface keyStore();

  LogManagerInterface log();

  NonceManagerInterface nonce();

  PropertyManagerInterface property();
}
