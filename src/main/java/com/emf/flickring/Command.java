package com.emf.flickring;

import java.util.EventListener;

import com.emf.flickring.manager.Chain;

public interface Command {

  Response process(Chain chain);
  
  void stop();

  public enum Response {
    /** Command has succeed his task. */
    SUCCESS,
    /** Command has failed his task completion. */
    FAIL,
    /** Command step can be skipped to the next command in the chain. */
    SKIP,
    /** Command chain must be interrupted. */
    END;

    public boolean canProceed() {
      if (this.equals(SKIP) || this.equals(SUCCESS)) {
        return true;
      }
      return false;
    }

  }

  public interface ShutdownListener extends EventListener {
    /** Indicates shutdown action was successful. */
    boolean shutdown();
  }

}
