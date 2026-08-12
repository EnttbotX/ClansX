package x.Entt.ClansX.Utils;

import org.bukkit.configuration.file.FileConfiguration;

import x.Entt.ClansX.CX;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckName {
    private final CX plugin;
    private static final Map<String, String> leetspeakMap = new HashMap<>();

    static {
        leetspeakMap.put("4", "a");
        leetspeakMap.put("3", "e");
        leetspeakMap.put("1", "i");
        leetspeakMap.put("0", "o");
        leetspeakMap.put("7", "t");
        leetspeakMap.put("@", "a");
        leetspeakMap.put("$", "s");
        leetspeakMap.put("5", "s");
        leetspeakMap.put("8", "b");
    }

    public CheckName(CX plugin) {
        this.plugin = plugin;
    }

    public String validateClanName(String name) {
        FileConfiguration cfg = plugin.getFH().getConfig();

        int min = cfg.getInt("clans.names.min-length", 1);
        int max = cfg.getInt("clans.names.max-length", 0);
        boolean allowUpper = cfg.getBoolean("clans.names.allow-uppercase", true);
        boolean allowNumbers = cfg.getBoolean("clans.names.allow-numbers", true);

        String normalized = normalize(name);
        List<String> blocked = cfg.getStringList("clans.names.blocked");

        for (String b : blocked) {
            if (normalized.contains(normalize(b))) {
                return null;
            }
        }

        if (!allowNumbers && name.matches(".*\\d.*")) {
            return null;
        }

        if (name.length() < min || (max > 0 && name.length() > max)) {
            return null;
        }

        return allowUpper ? name : name.toLowerCase();
    }

    public static String normalize(String name) {
        if (name == null) return "";
        String text = name.toLowerCase();

        for (Map.Entry<String, String> entry : leetspeakMap.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        text = text.replaceAll("[^a-z]", "");
        text = text.replaceAll("(.)\\1+", "$1");

        return text;
    }
}