package love.atorixa.oplatinamefix;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-pass, case-aware text replacement. No personal strings live here: every
 * pair comes from Config, which is edited on the device.
 */
final class NameTransformer {

    private final List<String[]> pairs = new ArrayList<>();
    private final List<String[]> initials = new ArrayList<>();
    private Pattern pattern;
    private boolean identity = true;

    NameTransformer(Config c) {
        if (c == null) {
            c = new Config();
        }
        addPair(c.oldFirst, c.newFirst);
        addPair(c.oldLast, c.newLast);

        String[] lines = c.extra == null ? new String[0] : c.extra.split("\\r?\\n");
        for (String line : lines) {
            int eq = line.indexOf('=');
            if (eq > 0) {
                addPair(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }

        addInitials(c.oldInitials, c.newInitials);
        // Derived from the names themselves, e.g. first letters of the old
        // first name + surname -> first letters of the new ones.
        addInitials(initialsOf(c.oldFirst, c.oldLast), initialsOf(c.newFirst, c.newLast));

        if (!pairs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            // Longest first so a two-word value wins over its parts.
            pairs.sort((a, b) -> b[0].length() - a[0].length());
            for (String[] p : pairs) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append("(?<![\\p{L}\\p{N}])")
                        .append(Pattern.quote(p[0]))
                        .append("(?![\\p{L}\\p{N}])");
            }
            pattern = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            identity = false;
        }
    }

    boolean isIdentity() {
        return identity;
    }

    private void addPair(String from, String to) {
        if (from == null || to == null) {
            return;
        }
        String f = from.trim();
        String t = to.trim();
        if (f.isEmpty() || t.isEmpty() || f.equalsIgnoreCase(t)) {
            return;
        }
        pairs.add(new String[]{f, t});
    }

    private void addInitials(String from, String to) {
        if (from == null || to == null) {
            return;
        }
        String f = from.trim();
        String t = to.trim();
        if (f.isEmpty() || t.isEmpty() || f.equalsIgnoreCase(t)) {
            return;
        }
        initials.add(new String[]{f, t});
    }

    private static String initialsOf(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.trim().isEmpty()) {
            sb.append(first.trim().charAt(0));
        }
        if (last != null && !last.trim().isEmpty()) {
            sb.append(last.trim().charAt(0));
        }
        return sb.length() >= 2 ? sb.toString() : "";
    }

    /** Returns the text unchanged when nothing matches; never re-applies itself. */
    String apply(String input) {
        if (input == null || input.isEmpty() || identity) {
            return input;
        }
        String trimmed = input.trim();
        // Short standalone strings are almost always the avatar initials.
        if (trimmed.length() <= 6) {
            for (String[] p : initials) {
                if (p[0].equalsIgnoreCase(trimmed)) {
                    return matchCase(trimmed, p[1]);
                }
            }
        }
        Matcher m = pattern.matcher(input);
        StringBuilder out = new StringBuilder(input.length() + 8);
        boolean changed = false;
        while (m.find()) {
            String found = m.group();
            String repl = null;
            for (String[] p : pairs) {
                if (p[0].equalsIgnoreCase(found)) {
                    repl = matchCase(found, p[1]);
                    break;
                }
            }
            m.appendReplacement(out, Matcher.quoteReplacement(repl == null ? found : repl));
            changed = changed || repl != null;
        }
        if (!changed) {
            return input;
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Keeps the shape of the original text: ALL CAPS, Capitalised or lowercase. */
    private static String matchCase(String source, String replacement) {
        boolean hasLetters = false;
        boolean allUpper = true;
        boolean firstUpper = false;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (Character.isLetter(ch)) {
                if (!hasLetters) {
                    firstUpper = Character.isUpperCase(ch);
                }
                hasLetters = true;
                if (!Character.isUpperCase(ch)) {
                    allUpper = false;
                }
            }
        }
        if (!hasLetters) {
            return replacement;
        }
        if (allUpper) {
            return replacement.toUpperCase();
        }
        String lower = replacement.toLowerCase();
        if (firstUpper) {
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
        return lower;
    }
}
