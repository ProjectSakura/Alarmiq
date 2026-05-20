package com.shen.alarmiq.challenge;

import java.util.Random;

/**
 * Generates highly complex, dynamic, and non-repetitive linguistic challenges.
 * Uses an expanded vocabulary and nested grammatical structures to ensure maximum cognitive friction.
 */
public class DynamicChallengeGenerator {

    private final Random random = new Random();

    private static final String[] ADJECTIVES = {
            "Implicit", "Inextricable", "Ephemeral", "Pernicious", "Ubiquitous", "Quintessential",
            "Meticulous", "Subterranean", "Labyrinthine", "Ethereal", "Cognitive", "Stoic",
            "Relentless", "Arbitrary", "Paradoxical", "Synchronous", "Profound", "Transient",
            "Indefatigable", "Voracious", "Obscure", "Ambiguous", "Robust", "Diligent"
    };

    private static final String[] ADVERBS = {
            "inevitably", "meticulously", "profoundly", "relentlessly", "paradoxically",
            "systematically", "spontaneously", "clandestinely", "vehemently", "implicitly",
            "robustly", "diligently", "voraciously", "transiently", "arbitrarily"
    };

    private static final String[] SUBJECTS = {
            "The architecture of consciousness", "A disciplined neurological pathway", 
            "Your future existential manifestation", "Methodical consistency", 
            "Empirical success", "Incremental progress", "Inherent mental resilience", 
            "The arduous path to self-mastery", "Excellence in execution", 
            "The accumulation of small victories", "Boundless ambition", "A laser-focused cognitive state",
            "Philosophical stoicism", "The inertia of habitual comfort", "Proprioceptive awareness"
    };

    private static final String[] VERBS = {
            "necessitates", "mandates", "fundamentally supersedes", "precipitates", "orchestrates", 
            "asserts total dominance over", "catalyzes the evolution of", "metamorphoses", 
            "masterfully navigates", "bolsters", "substantiates", "circumvents"
    };

    private static final String[] OBJECTS = {
            "uncompromising discipline", "unwavering industrious effort", "the seduction of mediocrity", 
            "transcendental clarity", "a meticulously calibrated instrument", "measurable advancement", 
            "psychological sovereignty", "the threshold of monumental achievement", 
            "a durable neurological precedent", "your internal subjective reality", 
            "an inexhaustible reservoir of will", "sustained intellectual vitality"
    };

    private static final String[] CONNECTORS = {
            "notwithstanding the fact that", "while simultaneously ensuring that", 
            "precisely because", "whereas", "insofar as", "provided that", "lest"
    };

    /** Generates a single complex phrase for short challenges. */
    public String nextPhrase() {
        if (random.nextBoolean()) {
            return ADJECTIVES[random.nextInt(ADJECTIVES.length)].toLowerCase() + " " +
                   SUBJECTS[random.nextInt(SUBJECTS.length)].toLowerCase() + " " +
                   ADVERBS[random.nextInt(ADVERBS.length)] + " " +
                   VERBS[random.nextInt(VERBS.length)] + " " +
                   OBJECTS[random.nextInt(OBJECTS.length)];
        } else {
            return SUBJECTS[random.nextInt(SUBJECTS.length)].toLowerCase() + " " +
                   VERBS[random.nextInt(VERBS.length)] + " " +
                   ADJECTIVES[random.nextInt(ADJECTIVES.length)].toLowerCase() + " " +
                   OBJECTS[random.nextInt(OBJECTS.length)];
        }
    }

    /** Generates a dynamic paragraph with high linguistic complexity. */
    public String nextParagraph() {
        StringBuilder sb = new StringBuilder();
        int sentences = 3 + random.nextInt(2);
        for (int i = 0; i < sentences; i++) {
            sb.append(generateComplexSentence());
            if (i < sentences - 1) sb.append(" ");
        }
        return sb.toString();
    }

    private String generateComplexSentence() {
        String base = SUBJECTS[random.nextInt(SUBJECTS.length)];
        if (random.nextBoolean()) base = ADJECTIVES[random.nextInt(ADJECTIVES.length)] + " " + base.toLowerCase();
        
        String mid = ADVERBS[random.nextInt(ADVERBS.length)] + " " + VERBS[random.nextInt(VERBS.length)];
        String end = OBJECTS[random.nextInt(OBJECTS.length)];
        if (random.nextBoolean()) end = ADJECTIVES[random.nextInt(ADJECTIVES.length)].toLowerCase() + " " + end;

        String s = base + " " + mid + " " + end;
        
        if (random.nextInt(3) == 0) { // 33% chance to append a complex clause
            s += " " + CONNECTORS[random.nextInt(CONNECTORS.length)] + " " +
                 SUBJECTS[random.nextInt(SUBJECTS.length)].toLowerCase() + " " +
                 ADVERBS[random.nextInt(ADVERBS.length)] + " " +
                 VERBS[random.nextInt(VERBS.length)] + " " +
                 OBJECTS[random.nextInt(OBJECTS.length)];
        }
        
        return s + ".";
    }
}
