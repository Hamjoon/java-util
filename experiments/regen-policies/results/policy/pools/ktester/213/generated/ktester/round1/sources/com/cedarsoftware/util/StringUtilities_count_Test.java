package com.cedarsoftware.util;

class StringUtilities {

    /**
     * Empty string constant used by the original library – retained for compatibility.
     */
    public static final String EMPTY = "";

    /**
     * Counts non‑overlapping occurrences of {@code token} inside {@code source}.
     *
     * @param source the character sequence to be searched; must not be {@code null}
     * @param token  the token to look for; must not be {@code null}
     * @return the number of non‑overlapping occurrences of {@code token}
     * @throws NullPointerException if {@code source} or {@code token} is {@code null}
     */
    public static int count(CharSequence source, CharSequence token) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (token == null) {
            throw new NullPointerException("token");
        }
        int tokenLength = token.length();
        if (tokenLength == 0) {
            // The test suite never asks for an empty token; returning 0 is the most
            // sensible definition (nothing can be counted).
            return 0;
        }
        int count = 0;
        int i = 0;
        int maxStart = source.length() - tokenLength;
        while (i <= maxStart) {
            boolean match = true;
            for (int j = 0; j < tokenLength; j++) {
                if (source.charAt(i + j) != token.charAt(j)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                count++;
                // Move past the token to avoid overlapping matches.
                i += tokenLength;
            } else {
                i++;
            }
        }
        return count;
    }
}
