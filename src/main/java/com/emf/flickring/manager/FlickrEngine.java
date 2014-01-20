package com.emf.flickring.manager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import com.emf.flickring.Command.Response;
import com.emf.flickring.Engine;
import com.google.inject.Inject;

@Slf4j
public class FlickrEngine implements Engine {

  private final Handler handler;
  private final ExecutorService executor;
  private Future<Handler> futureHandler;

  @Inject
  public FlickrEngine(final Chain chain) {
    this.handler = new EngineHandler(chain);
    this.executor = Executors.newSingleThreadExecutor();
  }

  @Override
  public void start() {
    futureHandler = executor.submit(handler, handler);
  }

  @Override
  public void stop() {
    executor.shutdown();
    try {
      futureHandler.get().stop();
    } catch (InterruptedException e) {
      log.error("Handler got interrupted.", e);
    } catch (ExecutionException e) {
      log.error("Could not execute handler.", e);
    }
  }

  @Override
  public void waitBeforeLeaving() throws InterruptedException {
    executor.awaitTermination(10, TimeUnit.SECONDS);
  }

  @Slf4j
  private static class EngineHandler implements Handler {
    private final Chain chain;
    
    public EngineHandler(final Chain chain) {
      this.chain = chain;
    }

    @Override
    public void run() {
      log.debug("Handler is running...");
      final Response response = chain.execute();
      log.debug("Execution chain has completed: {}", response);
    }

    @Override
    public void stop() {
      chain.breakIt();
    }
  }

  private interface Handler extends Runnable {
    void stop();
  }

}
