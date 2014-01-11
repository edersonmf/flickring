package com.emf.flickring;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;

import com.emf.flickring.deploy.DeployModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

@Slf4j
public class Main implements Daemon {

  private String configFolder;

  private Engine engine;

  public static void main(final String[] args) throws DaemonInitException, Exception {
    final Main main = new Main();
    main.init(new DaemonContext() {
      @Override
      public DaemonController getController() {
        return null;
      }
      @Override
      public String[] getArguments() {
        return args;
      }
    });

    main.start();
//    main.stop();
//    main.destroy();
  }

  @Override
  public void destroy() {
    log.info("Flickring is about to be destroyed.");
    try {
      this.engine.waitBeforeLeaving();
    } catch (InterruptedException e) {
      log.error("Flickr engine was interrupted", e);
    }
    log.info("Done. Bye!");
  }

  @Override
  public void init(DaemonContext context) throws DaemonInitException, Exception {
    log.info("Initializing flickring...");
    this.configFolder = context.getArguments()[0];
  }

  @Override
  public void start() throws Exception {
    log.info("Starting flickring...");
    final Injector injector = Guice.createInjector(new DeployModule(configFolder));
    this.engine = injector.getInstance(Engine.class);
    this.engine.start();
  }

  @Override
  public void stop() throws Exception {
    log.info("Stopping flickring...");
    this.engine.stop();
  }
  

}