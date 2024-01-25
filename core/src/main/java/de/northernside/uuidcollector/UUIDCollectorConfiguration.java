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
import java.util.*;

@SuppressWarnings("FieldMayBeFinal")
@ConfigName("settings")
public class UUIDCollectorConfiguration extends AddonConfig {

  private void sendNotification(String text, int duration) {
    Notification notification = Notification.builder()
        .icon(Component.icon(Icon.texture(ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
            .getIcon())
        .title(Component.text("UUIDCollector"))
        .text(Component.text(text))
        .duration(duration)
        .build();

    Laby.labyAPI().minecraft().executeOnRenderThread(
        () -> Laby.labyAPI().notificationController().push(notification));
  }

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
    sendNotification("You've collected locally " + uuidAmount + " UUIDs.", 4000);
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

          if (connection.getResponseCode() == 200) {
            InputStream inputStream = connection.getInputStream();
            json = new Gson().fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), UUIDJsonModel.class);
          } else {
            error = "The collection server responded with an error " + connection.getResponseCode() + ".";
          }
        } catch (IOException e) {
          error = "Could not connect to collection server!";
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

    sendNotification(text, 4000);
  }

  @MethodOrder(after = "getOnServer")
  @ButtonSetting(translation = "uuidcollector.settings.uploadToServer.text")
  public void uploadToServer() {

    if (UUIDCollector.users.isEmpty()) {
      sendNotification("Nothing to upload!", 4000);
      return;
    }

    class veryClassy implements Runnable {
      private String error;
      private UUIDJsonModel json;
      private HashMap<UUID, String> splitCollection;

      public veryClassy(HashMap<UUID, String> splitCollection) {
          this.splitCollection = splitCollection;
      }

      @Override
      public void run() {
        try {
          UUIDPostRequestModel usersRequestModel = new UUIDPostRequestModel(this.splitCollection);

          String usersJson = new Gson().toJson(usersRequestModel);
          Response<WebInputStream> response = Request.ofInputStream()
              .url(collectionServer.get() + "api/donate/" + authenticationKey.get())
              .json(usersJson)
              .readTimeout(20000)
              .executeSync();

          if (response.getStatusCode() == 200) {
            json = new Gson().fromJson(new InputStreamReader(response.get(), StandardCharsets.UTF_8), UUIDJsonModel.class);
          } else {
            throw new IOException();
          }
        } catch (Exception e) {
          error = "Could not connect to collection server!";
          UUIDCollector.users.putAll(this.splitCollection);
          InCollectionHUD.updateInCollection(UUIDCollector.users.size());
        }
      }

      public UUIDJsonModel getJson() {
        return json;
      }

      public String getError() {
        return error;
      }
    }

    // Allows collection size to be put back accurately, by using a temporary system while it uploads up the UUIDs.
    // Splitting up HashMaps to max 250 entries, because experienced issues with timeouts because of to large requests (around 500 entries).
    UUIDCollector.tempCollection.putAll(UUIDCollector.users);
    UUIDCollector.users.clear();
    InCollectionHUD.updateInCollection(0);

    List<HashMap<UUID, String>> collections = new ArrayList<>();
    HashMap<UUID, String> tempMap = new HashMap<>();
    int counter = 0;
    for (Map.Entry<UUID, String> entry : UUIDCollector.tempCollection.entrySet()) {
      tempMap.put(entry.getKey(), entry.getValue());
      counter++;
      if (tempMap.size() == 250 || counter == UUIDCollector.tempCollection.size()) {
        collections.add(new HashMap<>(tempMap));
        tempMap.clear();
      }
    }
    UUIDCollector.tempCollection.clear();

    List<Integer> validList = new ArrayList<>();
    List<Integer> totalList = new ArrayList<>();
    List<String> errorsList = new ArrayList<>();
    for (HashMap<UUID, String> splitCollection : collections) {
      veryClassy classy = new veryClassy(splitCollection);
      Thread thread = new Thread(classy);
      thread.start();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      if (classy.getError() == null) {
        validList.add(classy.getJson().getValid());
        totalList.add(classy.getJson().getLength());
      } else {
        errorsList.add(classy.getError());
      }
    }

    int valid = 0;
    for (int number : validList) {
      valid += number;
    }

    if (errorsList.isEmpty()) {
      sendNotification("Uploaded " + valid + " valid UUIDs. Total UUIDs: " + Collections.max(totalList), 8000);
      OnServerHUD.updateOnServer(Collections.max(totalList));
    } else if (!validList.isEmpty()) {
      sendNotification("Uploaded " + valid + " valid UUIDs. Total UUIDs: " + Collections.max(totalList) + " (There have been errors)", 8000);
      OnServerHUD.updateOnServer(Collections.max(totalList));
      for (String error : errorsList) {
        sendNotification(error, 4000);
      }
    } else {
      for (String error : errorsList) {
        sendNotification(error, 4000);
      }
      OnServerHUD.updateOnServer(-1);
    }
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
