package com.emf.flickring.model;

import java.util.Scanner;

import lombok.Getter;
import lombok.experimental.Builder;

@Builder
public class ConfigInput {

  @Getter
  private final String label;
  @Getter
  private String inputedValue;
  private final Scanner scanner;

  public void read() {
    System.out.println(label);
    this.inputedValue = scanner.nextLine();
  }
}
