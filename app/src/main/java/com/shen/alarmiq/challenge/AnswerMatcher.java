package com.shen.alarmiq.challenge;

/**
 * Matching helpers for free-text challenges.
 *
 * <p>Reverse-type uses {@link #exactNormalized} so the user has to nail every
 * letter. Paragraph uses {@link #similar} so a few typos in 50 words are
 * forgiven — but a wholesale skip isn't.</p>
 */
public final class AnswerMatcher {

    public static boolean exactNormalized(String expected, String input) {
        return normalize(expected).equals(normalize(input));
    }

    public static boolean similar(String expected, String input, double threshold) {
        String a = normalize(expected);
        String b = normalize(input);
        if (a.isEmpty() && b.isEmpty()) return true;
        int distance = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        double sim = maxLen == 0 ? 1.0 : 1.0 - ((double) distance / (double) maxLen);
        return sim >= threshold;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        boolean lastSpace = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
                lastSpace = false;
            } else if (!lastSpace) {
                sb.append(' ');
                lastSpace = true;
            }
        }
        // strip trailing space
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == ' ') sb.setLength(len - 1);
        return sb.toString();
    }

    private static int levenshtein(String a, String b) {
        int n = a.length(), m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[m];
    }

    private AnswerMatcher() {}
}
