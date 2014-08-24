package com.emf.flickring.manager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

import com.emf.flickring.Command;
import com.google.inject.Inject;

@Slf4j
public class SyncCommand implements Command {

  private final PhotoDiscoveryHandler handler;
  private final ExecutorService executor;

  @Inject
  public SyncCommand(final PhotoDiscoveryHandler handler) {
    this.handler = handler;
    this.executor = Executors.newSingleThreadExecutor();
  }

  @Override
  public Response process(final Chain chain) {
    log.debug("Processing Sync Command.");
    this.executor.execute(handler);
    return chain.execute();
  }

  @Override
  public void stop() {
    log.info("Sync Command is shutdown.");
    handler.shutdownListener().shutdown();
    executor.shutdown();
  }

}
