package de.northernside.uuidcollector.listener;

import de.northernside.uuidcollector.UUIDCollector;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;

public class ServerDisconnectListener {

  private final UUIDCollector addon;

  public ServerDisconnectListener(UUIDCollector addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onServerDisconnect(ServerDisconnectEvent event) {
    addon.configuration().uploadToServer();
  }
}

