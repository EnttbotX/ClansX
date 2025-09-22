package x.Entt.ClansX.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import x.Entt.ClansX.CX;

public class Updater {
  private final CX plugin;
  
  private final int resourceId;
  
  public Updater(CX plugin, int resourceId) {
    this.plugin = plugin;
    this.resourceId = resourceId;
  }
  
  public String getLatestVersion() throws IOException {
    URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId);
    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    connection.setRequestMethod("GET");
    try {
      InputStream inputStream = connection.getInputStream();
      try {
        Scanner scanner = new Scanner(inputStream);
        try {
          if (scanner.hasNext()) {
            String str = scanner.next();
            scanner.close();
            if (inputStream != null)
              inputStream.close(); 
            return str;
          } 
          scanner.close();
        } catch (Throwable throwable) {
          try {
            scanner.close();
          } catch (Throwable throwable1) {
            throwable.addSuppressed(throwable1);
          } 
          throw throwable;
        } 
        if (inputStream != null)
          inputStream.close(); 
      } catch (Throwable throwable) {
        if (inputStream != null)
          try {
            inputStream.close();
          } catch (Throwable throwable1) {
            throwable.addSuppressed(throwable1);
          }  
        throw throwable;
      } 
    } catch (IOException e) {
      this.plugin.getLogger().severe("Failed to retrieve the latest version: " + e.getMessage());
      throw e;
    } 
    return null;
  }
  
  public boolean isUpdateAvailable() {
    try {
      String currentVersion = this.plugin.getDescription().getVersion();
      String latestVersion = getLatestVersion();
      return (latestVersion != null && !latestVersion.equals(currentVersion));
    } catch (IOException e) {
      this.plugin.getLogger().severe("Error checking for updates: " + e.getMessage());
      return false;
    } 
  }
}
