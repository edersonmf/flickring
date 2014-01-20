package com.emf.flickring.manager;

import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import com.emf.flickring.Command;
import com.emf.flickring.Command.Response;
import com.google.inject.Inject;

@Slf4j
public class ExecutionChain implements Chain {

  private final Stack<Command> commandsStack;
  private final Stack<Command> finalizeStack;

  @Inject
  public ExecutionChain(final ApiKeysCommand apiKeysCommand, final SyncCommand syncCommand) {
    this.commandsStack = new Stack<Command>();
    this.finalizeStack = new Stack<Command>();
    this.commandsStack.add(syncCommand);
    this.commandsStack.add(apiKeysCommand);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T execute() {
    if (!commandsStack.isEmpty()) {
      final Command command = commandsStack.pop();
      final Response response = command.process(this);
      finalizeStack.add(command);
      log.info("Command {} result is {}", command.getClass().getSimpleName(), response);
      if (!response.canProceed()) {
        return (T) response;
      }
    }
    return (T) Response.SUCCESS;
  }

  @Override
  public void breakIt() {
    while (!finalizeStack.isEmpty()) {
      final Command command = finalizeStack.pop();
      command.stop();
      log.info("Command {} was stopped", command.getClass().getSimpleName());
    }
  }

}
