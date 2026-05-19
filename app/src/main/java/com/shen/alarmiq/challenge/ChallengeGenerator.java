package com.shen.alarmiq.challenge;

import com.shen.alarmiq.math.Difficulty;
import com.shen.alarmiq.math.MathProblem;
import com.shen.alarmiq.math.MathProblemGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds the ordered list of challenges for one alarm firing.
 *
 * <p>The easier difficulties (EASY/NORMAL/HARD) deliver only math problems
 * scaled to that level. NIGHTMARE and PUNISHMENT mix in typing, paragraph and
 * photo challenges so the user can't muscle-memory their way through.</p>
 */
public class ChallengeGenerator {

    private final Random random = new Random();
    private final MathProblemGenerator math = new MathProblemGenerator();

    public List<Challenge> generate(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:    return repeatedMath(Difficulty.EASY, 3);
            case NORMAL:  return repeatedMath(Difficulty.NORMAL, 3);
            case HARD:    return repeatedMath(Difficulty.HARD, 3);
            case NIGHTMARE: return nightmare();
            case PUNISHMENT: return punishment();
            default: return repeatedMath(Difficulty.NORMAL, 3);
        }
    }

    private List<Challenge> repeatedMath(Difficulty d, int count) {
        List<Challenge> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            MathProblem p = math.next(d);
            out.add(Challenge.math(p.question, p.answer));
        }
        return out;
    }

    /** Mix designed for NIGHTMARE — 5 challenges, varied. */
    private List<Challenge> nightmare() {
        List<Challenge> out = new ArrayList<>();
        out.add(mathChallenge(Difficulty.HARD));
        out.add(reverseChallenge());
        out.add(mathChallenge(Difficulty.HARD));
        out.add(photoChallenge());
        out.add(paragraphChallenge());
        return out;
    }

    /** Mix designed for PUNISHMENT — 6 challenges, all hard variants. */
    private List<Challenge> punishment() {
        List<Challenge> out = new ArrayList<>();
        out.add(mathChallenge(Difficulty.HARD));
        out.add(reverseChallenge());
        out.add(photoChallenge());
        out.add(paragraphChallenge());
        out.add(mathChallenge(Difficulty.HARD));
        out.add(reverseChallenge());
        // Shuffle the middle so the user can't predict the order.
        Collections.shuffle(out.subList(0, out.size()), random);
        return out;
    }

    private Challenge mathChallenge(Difficulty d) {
        MathProblem p = math.next(d);
        return Challenge.math(p.question, p.answer);
    }

    private Challenge reverseChallenge() {
        String phrase = ChallengeBank.REVERSE_PHRASES[
                random.nextInt(ChallengeBank.REVERSE_PHRASES.length)];
        return Challenge.reverse(phrase);
    }

    private Challenge paragraphChallenge() {
        String para = ChallengeBank.PARAGRAPHS[
                random.nextInt(ChallengeBank.PARAGRAPHS.length)];
        return Challenge.paragraph(para);
    }

    private Challenge photoChallenge() {
        String subject = ChallengeBank.PHOTO_SUBJECTS[
                random.nextInt(ChallengeBank.PHOTO_SUBJECTS.length)];
        return Challenge.photo(subject);
    }
}
