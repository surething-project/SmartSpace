/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import pt.ulisboa.tecnico.surespace.common.domain.Entity;
import pt.ulisboa.tecnico.surespace.common.domain.Nonce;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.ThreadLocalRandom;

public final class NonceManager implements NonceManagerInterface {
  private static final int NONCES_SET_SIZE = 128;
  private final HashSet<Nonce> nonces = new LinkedHashSet<>();

  private synchronized long generateRandomLong() {
    return ThreadLocalRandom.current().nextLong();
  }

  @Override
  public Nonce next(Entity entity) {
    Nonce nonce = new Nonce(generateRandomLong());
    while (!nonces.add(nonce)) nonce = new Nonce(generateRandomLong());

    return nonce;
  }

  @Override
  public boolean valid(Nonce nonce, Entity entity) {
    boolean alreadySeen = !nonces.contains(nonce);
    if (nonces.size() == NONCES_SET_SIZE) nonces.clear();

    return alreadySeen;
  }
}
