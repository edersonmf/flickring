package com.emf.flickring;

public interface Engine {

  void start();

  void stop();

  void waitBeforeLeaving() throws InterruptedException;

}
