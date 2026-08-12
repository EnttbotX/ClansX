package x.Entt.ClansX.Utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import static x.Entt.ClansX.CX.prefix;

public class Updater {
    private final JavaPlugin plugin;
    private final int resourceId;

    public Updater(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void searchUpdates() {
        String latest = getLatestVersion();
        if (latest == null) {
            return;
        }

        if (compareVersions(plugin.getDescription().getVersion(), latest) >= 0) {
            return;
        }

        Text.log("");
        Text.log("&2&l======== " + prefix + "&2&l========");
        Text.log("&e&lUPDATE AVAILABLE");
        Text.log("&e&lVersion: &f" + plugin.getDescription().getVersion());
        Text.log("&e&lNew Version: &f" + latest);
        Text.log("&e&lDownload Link:");
        Text.log(getResourceURL());
        Text.log("&2&l======== " + prefix + "&2&l========");
        Text.log("");
    }

    public boolean newUpdateAvailable() {
        return compareVersions(plugin.getDescription().getVersion(), getLatestVersion()) < 0;
    }

    public String getLatestVersion() {
        try {
            URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            try (InputStream in = con.getInputStream(); Scanner s = new Scanner(in)) {
                if (s.hasNext()) {
                    return s.next();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Update check failed: " + e.getMessage());
        }

        return null;
    }

    private int compareVersions(String current, String latest) {
        String[] c = current.split("\\.");
        String[] l = latest.split("\\.");

        int max = Math.max(c.length, l.length);

        for (int i = 0; i < max; i++) {
            int cv = i < c.length ? Integer.parseInt(c[i]) : 0;
            int lv = i < l.length ? Integer.parseInt(l[i]) : 0;

            if (cv < lv) return -1;
            if (cv > lv) return 1;
        }

        return 0;
    }

    public String getResourceURL() {
        return "https://www.spigotmc.org/resources/" + resourceId + "/";
    }
}