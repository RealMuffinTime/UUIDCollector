package de.northernside.uuidcollector.misc;

import java.util.HashMap;
import java.util.UUID;

public class UUIDPostRequestModel {

  private final HashMap<UUID, String> users;

  public UUIDPostRequestModel(HashMap<UUID, String> users) {
    this.users = users;
  }
}
