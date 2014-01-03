package com.emf.flickring.manager;

public interface Chain {

  <T>T execute();

  void releaseResources();

}
