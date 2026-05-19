package com.shen.alarmiq.challenge;

/**
 * One step the user must complete to dismiss an alarm.
 *
 * <p>For MATH/REVERSE/PARAGRAPH, {@link #expected} holds the canonical answer
 * that user input is compared against. For PHOTO, the user just has to capture
 * any image — there is no answer to type.</p>
 */
public class Challenge {

    public enum Type {
        MATH,           // numeric input, exact match
        REVERSE_TYPE,   // type a phrase backwards
        PARAGRAPH,      // type a paragraph (fuzzy match)
        PHOTO           // capture a picture of a named object
    }

    public final Type type;
    /** Short label shown above the prompt, e.g. "TYPE BACKWARDS". */
    public final int labelRes;
    /** The prompt the user reads. For PHOTO, the object name (e.g. "sink"). */
    public final String prompt;
    /** For PARAGRAPH/REVERSE the canonical answer. For MATH, the numeric answer. Null for PHOTO. */
    public final String expected;

    public Challenge(Type type, int labelRes, String prompt, String expected) {
        this.type = type;
        this.labelRes = labelRes;
        this.prompt = prompt;
        this.expected = expected;
    }

    public static Challenge math(String expression, int answer) {
        return new Challenge(Type.MATH,
                com.shen.alarmiq.R.string.challenge_math_label,
                expression, String.valueOf(answer));
    }

    public static Challenge reverse(String phrase) {
        return new Challenge(Type.REVERSE_TYPE,
                com.shen.alarmiq.R.string.challenge_reverse_label,
                phrase, reverseString(phrase));
    }

    public static Challenge paragraph(String text) {
        return new Challenge(Type.PARAGRAPH,
                com.shen.alarmiq.R.string.challenge_paragraph_label,
                text, text);
    }

    public static Challenge photo(String objectName) {
        return new Challenge(Type.PHOTO,
                com.shen.alarmiq.R.string.challenge_photo_label,
                objectName, null);
    }

    private static String reverseString(String s) {
        return new StringBuilder(s).reverse().toString();
    }
}
