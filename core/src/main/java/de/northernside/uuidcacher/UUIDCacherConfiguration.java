package de.northernside.uuidcacher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.notification.Notification;
import net.labymod.api.util.MethodOrder;

@SuppressWarnings("FieldMayBeFinal")
@ConfigName("settings")
public class UUIDCacherConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @MethodOrder(after = "enabled")
  @ButtonSetting(translation = "uuidcacher.settings.uploadUUIDs.text")
  public void uploadUUIDs() {
    int uuidAmount = UUIDCacher.users.size();
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
      UUIDCacher.users.forEach((playerUUID, playerUsername) -> {
        try {
          URL url = new URL(
              "https://db.lilo-lookup.de/api/user/index/" + playerUUID + "/" + playerUsername);
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          connection.getInputStream();
          if (connection.getResponseCode() != 200) {
            Notification errorNotification = Notification.builder()
                .icon(Component.icon(Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
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

      UUIDCacher.users.clear();
    });

    uploadThread.start();
  }

  @MethodOrder(after = "uploadUUIDs")
  @ButtonSetting(translation = "uuidcacher.settings.getCache.text")
  public void getCache() {
    int uuidAmount = UUIDCacher.users.size();
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
}
