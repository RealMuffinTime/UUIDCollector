package de.northernside.uuidcollector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.notification.Notification;
import net.labymod.api.util.MethodOrder;

@SuppressWarnings("FieldMayBeFinal")
@ConfigName("settings")
public class UUIDCollectorConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);
  @MethodOrder(after = "getCollection")
  @TextFieldSetting
  private final ConfigProperty<String> collectionServer = new ConfigProperty<>(
      "https://db.lilo-lookup.de/api/");
  @MethodOrder(after = "collectionServer")
  @TextFieldSetting
  private final ConfigProperty<String> authenticationKey = new ConfigProperty<>("Your AuthKey");

  @MethodOrder(after = "enabled")
  @ButtonSetting(translation = "uuidcollector.settings.uploadUUIDs.text")
  public void uploadUUIDs() {
    int uuidAmount = UUIDCollector.users.size();
    if (uuidAmount == 0) {
      Notification noUUIDsNotification = Notification.builder()
          .icon(Component.icon(Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
              .getIcon())
          .title(Component.text("Nothing to upload!"))
          .text(Component.text("You've collected " + uuidAmount + " UUIDs."))
          .duration(4000)
          .build();

      Laby.labyAPI().minecraft().executeOnRenderThread(
          () -> Laby.labyAPI().notificationController().push(noUUIDsNotification));
      return;
    }

    Notification uploadingNotification = Notification.builder()
        .icon(Component.icon(Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
            .getIcon())
        .title(Component.text("Uploading UUIDs..."))
        .text(Component.text("You've collected " + uuidAmount + " UUIDs."))
        .duration(5000)
        .build();

    Laby.labyAPI().minecraft().executeOnRenderThread(
        () -> Laby.labyAPI().notificationController().push(uploadingNotification));

    Thread uploadThread = new Thread(() -> {
      // Allows collection size to be put back accurately, by using a temporary system while it looks up the UUIDs.
      UUIDCollector.tempCollection.putAll(UUIDCollector.users);

      UUIDCollector.users.clear();
      UUIDCollector.tempCollection.forEach((playerUUID, playerUsername) -> {
        try {
          URL url = new URL(
              this.collectionServer.get() + "user/index/" + playerUUID + "/" + playerUsername
                  + "?key=" + authenticationKey.get());
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          connection.getInputStream();

          // Removes the user from the collection.
          UUIDCollector.tempCollection.remove(playerUUID);

          if (connection.getResponseCode() != 200) {
            Notification errorNotification = Notification.builder()
                .icon(Component.icon(
                        Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
                    .getIcon())
                .title(Component.text("Error " + connection.getResponseCode()))
                .text(Component.text("The collection server responded with an error."))
                .duration(4500)
                .build();

            Laby.labyAPI().minecraft().executeOnRenderThread(
                () -> Laby.labyAPI().notificationController().push(errorNotification));
          }
        } catch (IOException exception) {
          exception.printStackTrace();
        }
      });

      Notification uploadedNotification = Notification.builder()
          .icon(Component.icon(Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
              .getIcon())
          .title(Component.text("Uploaded UUIDs!"))
          .text(Component.text("Uploaded " + uuidAmount + " UUIDs to the collection server."))
          .duration(8000)
          .build();

      Laby.labyAPI().minecraft().executeOnRenderThread(
          () -> Laby.labyAPI().notificationController().push(uploadedNotification));
    });

    uploadThread.start();
  }

  @MethodOrder(after = "uploadUUIDs")
  @ButtonSetting(translation = "uuidcollector.settings.getCollection.text")
  public void getCollection() {
    int uuidAmount = UUIDCollector.users.size();
    Notification noUUIDsNotification = Notification.builder()
        .icon(Component.icon(Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
            .getIcon())
        .title(Component.text("UUIDCollector"))
        .text(Component.text("You've collected " + uuidAmount + " UUIDs."))
        .duration(3500)
        .build();

    Laby.labyAPI().minecraft().executeOnRenderThread(
        () -> Laby.labyAPI().notificationController().push(noUUIDsNotification));
  }

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<String> collectionServer() {
    return this.collectionServer;
  }

  public ConfigProperty<String> authenticationKey() {
    return this.authenticationKey;
  }
}
