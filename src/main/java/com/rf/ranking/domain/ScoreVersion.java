package com.rf.ranking.domain;

public enum ScoreVersion {
  V1("v1");

  private final String value;

  ScoreVersion(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ScoreVersion fromString(String value) {
    if (value == null) {
      return V1;
    }
    for (ScoreVersion version : values()) {
      if (version.value.equalsIgnoreCase(value)) {
        return version;
      }
    }
    throw new IllegalArgumentException("Unsupported score version: " + value);
  }
}
