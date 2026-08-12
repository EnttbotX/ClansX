package x.Entt.ClansX.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeMng {
    private static final Pattern PATTERN = Pattern.compile("(\\d+)([hdwy])");

    public static long parseToMillis(String input) {
        Matcher m = PATTERN.matcher(input.toLowerCase().trim());
        long total = 0;
        boolean found = false;

        while (m.find()) {
            found = true;
            long value = Long.parseLong(m.group(1));
            char unit = m.group(2).charAt(0);
            total += switch (unit) {
                case 'h' -> value * 3600_000L;
                case 'd' -> value * 86400_000L;
                case 'w' -> value * 604800_000L;
                case 'y' -> value * 31536000_000L;
                default -> 0L;
            };
        }

        if (!found) {
            throw new IllegalArgumentException("Invalid format: " + input);
        }

        return total;
    }

    public static long fromNow(String input) {
        return System.currentTimeMillis() + parseToMillis(input);
    }
}