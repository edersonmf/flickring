package com.emf.flickring.manager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import com.emf.flickring.Command.Response;
import com.emf.flickring.Engine;
import com.google.inject.Inject;

public class FlickrEngine implements Engine {

  private final EngineHandler handler;
  private final ExecutorService executor;

  @Inject
  public FlickrEngine(final Chain chain) {
    this.handler = new EngineHandler(chain);
    this.executor = Executors.newSingleThreadExecutor();
  }

  @Override
  public void start() {
    executor.execute(handler);
  }

  @Override
  public void stop() {
    executor.shutdown();
  }

  @Override
  public void waitBeforeLeaving() throws InterruptedException {
    executor.awaitTermination(10, TimeUnit.SECONDS);
  }

  @Slf4j
  private static class EngineHandler implements Runnable {
    private final Chain chain;
    
    public EngineHandler(final Chain chain) {
      this.chain = chain;
    }

    public void release() {
      chain.releaseResources();
    }

    @Override
    public void run() {
      log.debug("Handler is running...");
      final Response response = chain.execute();
      log.debug("Execution chain has completed: {}", response);
    }
  }

}
