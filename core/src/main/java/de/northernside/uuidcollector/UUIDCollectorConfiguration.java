package de.northernside.uuidcollector;

import com.google.gson.Gson;
import de.northernside.uuidcollector.hud.InCollectionHUD;
import de.northernside.uuidcollector.hud.OnServerHUD;
import de.northernside.uuidcollector.misc.UUIDJsonModel;
import de.northernside.uuidcollector.misc.UUIDPostRequestModel;
import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.notification.Notification;
import net.labymod.api.util.MethodOrder;
import net.labymod.api.util.io.web.WebInputStream;
import net.labymod.api.util.io.web.request.Request;
import net.labymod.api.util.io.web.request.Response;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("FieldMayBeFinal")
@ConfigName("settings")
public class UUIDCollectorConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  @MethodOrder(after = "enabled")
  @ButtonSetting(translation = "uuidcollector.settings.getInCollection.text")
  public void getInCollection() {
    int uuidAmount = UUIDCollector.users.size();
    Notification notification = Notification.builder()
        .icon(Component.icon(Icon.texture(ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
            .getIcon())
        .title(Component.text("UUIDCollector"))
        .text(Component.text("You've collected locally " + uuidAmount + " UUIDs."))
        .duration(4000)
        .build();

    Laby.labyAPI().minecraft().executeOnRenderThread(
        () -> Laby.labyAPI().notificationController().push(notification));
  }

  @MethodOrder(after = "getInCollection")
  @ButtonSetting(translation = "uuidcollector.settings.getOnServer.text")
  public void getOnServer() {
    class veryClassy implements Runnable {
      private String error;
      private UUIDJsonModel json;

      public veryClassy() {}

      @Override
      public void run() {
        try {
          URL url = new URL(collectionServer.get() + "api/key/" + authenticationKey.get());
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          InputStream inputStream = connection.getInputStream();
          UUIDJsonModel json = new Gson().fromJson(
              new InputStreamReader(inputStream, StandardCharsets.UTF_8), UUIDJsonModel.class);

          if (connection.getResponseCode() != 200) {
            error = "The collection server responded with an error " + connection.getResponseCode() + ".";
          } else {
            this.json = json;
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      public UUIDJsonModel getJson() {
        return json;
      }

      public String getError() {
        return error;
      }
    }

    veryClassy classy = new veryClassy();
    Thread thread = new Thread(classy);
    thread.start();
    try {
      thread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    String text;
    if (classy.getError() == null) {
      text = "There are " + classy.getJson().getLength() + " UUIDs on the server.";
      OnServerHUD.updateOnServer(classy.getJson().getLength());
    } else {
      text = classy.getError();
      OnServerHUD.updateOnServer(-1);
    }

    Notification notification = Notification.builder()
        .icon(Component.icon(Icon.texture(ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
            .getIcon())
        .title(Component.text("UUIDCollector"))
        .text(Component.text(text))
        .duration(4000)
        .build();

    Laby.labyAPI().minecraft().executeOnRenderThread(
        () -> Laby.labyAPI().notificationController().push(notification));
  }

  @MethodOrder(after = "getOnServer")
  @ButtonSetting(translation = "uuidcollector.settings.uploadToServer.text")
  public void uploadToServer() {
    class veryClassy implements Runnable {
      private String error;
      private UUIDJsonModel json;

      public veryClassy() {}

      @Override
      public void run() {
        // Allows collection size to be put back accurately, by using a temporary system while it looks up the UUIDs.
        UUIDCollector.tempCollection.putAll(UUIDCollector.users);
        UUIDCollector.users.clear();

        UUIDPostRequestModel usersRequestModel = new UUIDPostRequestModel(
            UUIDCollector.tempCollection);
        String usersJson = new Gson().toJson(usersRequestModel);
        Response<WebInputStream> response = Request.ofInputStream()
            .url(collectionServer.get() + "api/donate/" + authenticationKey.get())
            .json(usersJson).executeSync();

        if (response.getStatusCode() != 200) {
          error = "The collection server responded with an error " + response.getStatusCode() + ".";
          UUIDCollector.users.putAll(UUIDCollector.tempCollection);
          InCollectionHUD.updateInCollection(UUIDCollector.users.size());
        } else {
          json = new Gson().fromJson(new InputStreamReader(response.get(), StandardCharsets.UTF_8), UUIDJsonModel.class);
          UUIDCollector.tempCollection.clear();
          InCollectionHUD.updateInCollection(0);
        }
      }

      public UUIDJsonModel getJson() {
        return json;
      }

      public String getError() {
        return error;
      }
    }

    veryClassy classy = new veryClassy();
    Thread thread = new Thread(classy);
    thread.start();
    try {
      thread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    String text;
    int duration = 4000;
    if (classy.getError() == null) {
      text = "Uploaded " + classy.getJson().getValid() + " valid UUIDs. Total UUIDs: " + classy.getJson().getLength();
      OnServerHUD.updateOnServer(classy.getJson().getLength());
      duration = 8000;
    } else {
      text = classy.getError();
      OnServerHUD.updateOnServer(-1);
    }

    Notification notification = Notification.builder()
        .icon(Component.icon(
                Icon.texture(ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
            .getIcon())
        .title(Component.text("UUIDCollector"))
        .text(Component.text(text))
        .duration(duration)
        .build();

    Laby.labyAPI().minecraft().executeOnRenderThread(
        () -> Laby.labyAPI().notificationController().push(notification));
  }
  @MethodOrder(after = "uploadToServer")
  @TextFieldSetting
  private final ConfigProperty<String> collectionServer = new ConfigProperty<>(
      "https://users.northernsi.de/");

  public ConfigProperty<String> collectionServer() {
    return this.collectionServer;
  }
  @MethodOrder(after = "collectionServer")
  @TextFieldSetting
  private final ConfigProperty<String> authenticationKey = new ConfigProperty<>("Your AuthKey");

  public ConfigProperty<String> authenticationKey() {
    return this.authenticationKey;
  }
}
