/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.async;

import pt.ulisboa.tecnico.surespace.common.async.exception.AsyncException;
import pt.ulisboa.tecnico.surespace.common.exception.BroadException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("unused")
public abstract class AsyncCollector<Result> {
  private final CompletableFuture<Result> completableFuture = new CompletableFuture<>();

  protected final void cancel(BroadException e) {
    if (!completableFuture.isDone()) completableFuture.completeExceptionally(e);
  }

  protected final void complete(Result result) {
    if (!completableFuture.isDone()) completableFuture.complete(result);
  }

  protected abstract void compute();

  public final Result get() throws AsyncException {
    try {
      return start().completableFuture.get();

    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      throw new AsyncException(e.getMessage());
    }
  }

  public final AsyncCollector<Result> get(AsyncListener<Result, BroadException> listener) {
    completableFuture.whenCompleteAsync(
        (result, throwable) -> {
          if (result != null) listener.onComplete(result);
          else listener.onError(new AsyncException(throwable.getLocalizedMessage()));
        });

    return this;
  }

  public final AsyncCollector<Result> start() {
    if (!completableFuture.isDone()) compute();
    return this;
  }
}
