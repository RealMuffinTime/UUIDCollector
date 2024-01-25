package de.northernside.uuidcollector.listener;

import de.northernside.uuidcollector.UUIDCollector;
import de.northernside.uuidcollector.hud.InCollectionHUD;
import java.util.UUID;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.playerinfo.PlayerInfoAddEvent;

public class PlayerInfoAddListener {

  private final UUIDCollector addon;

  public PlayerInfoAddListener(UUIDCollector addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onPlayerInfoAdd(PlayerInfoAddEvent event) {
    UUID playerUUID = event.playerInfo().profile().getUniqueId();
    if (playerUUID.toString().endsWith("0000-000000000000")) return; // Specially crafted NPCs used for the LabyMod API.

    String playerUsername = event.playerInfo().profile().getUsername();
    if (!UUIDCollector.users.containsKey(playerUUID)
        && !UUIDCollector.totalUsers.containsKey(playerUUID)) {
      UUIDCollector.users.put(playerUUID, playerUsername);

      // To make sure you don't collect a user you've already collected before. Even when the user collection is cleared out!
      UUIDCollector.totalUsers.put(playerUUID, playerUsername);
      InCollectionHUD.updateInCollection(UUIDCollector.users.size());
    }
  }
}
