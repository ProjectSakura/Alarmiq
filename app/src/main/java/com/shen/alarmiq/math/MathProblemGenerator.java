package com.shen.alarmiq.math;

import java.util.Random;

public class MathProblemGenerator {

    private final Random random = new Random();

    public MathProblem next(Difficulty difficulty) {
        switch (difficulty) {
            case EASY: return easy();
            case HARD: return hard();
            case NIGHTMARE:
            case PUNISHMENT: return destroyer();
            case NORMAL:
            default: return normal();
        }
    }

    private MathProblem easy() {
        int a = randRange(2, 9);
        int b = randRange(2, 9);
        if (random.nextBoolean()) {
            return new MathProblem(a + " + " + b, a + b);
        } else {
            if (a < b) { int t = a; a = b; b = t; }
            return new MathProblem(a + " − " + b, a - b);
        }
    }

    private MathProblem normal() {
        int op = random.nextInt(3);
        switch (op) {
            case 0: {
                int a = randRange(11, 49);
                int b = randRange(11, 49);
                return new MathProblem(a + " + " + b, a + b);
            }
            case 1: {
                int a = randRange(20, 90);
                int b = randRange(10, a - 1);
                return new MathProblem(a + " − " + b, a - b);
            }
            default: {
                int a = randRange(3, 12);
                int b = randRange(3, 12);
                return new MathProblem(a + " × " + b, a * b);
            }
        }
    }

    private MathProblem hard() {
        int op = random.nextInt(3);
        switch (op) {
            case 0: {
                int a = randRange(12, 29);
                int b = randRange(6, 13);
                return new MathProblem(a + " × " + b, a * b);
            }
            case 1: {
                int b = randRange(4, 13);
                int q = randRange(4, 13);
                int a = b * q;
                return new MathProblem(a + " ÷ " + b, q);
            }
            default: {
                int a = randRange(120, 480);
                int b = randRange(50, a - 1);
                return new MathProblem(a + " − " + b, a - b);
            }
        }
    }

    private MathProblem destroyer() {
        int form = random.nextInt(3);
        switch (form) {
            case 0: {
                int a = randRange(15, 39);
                int b = randRange(6, 14);
                int c = randRange(10, 80);
                return new MathProblem(a + " × " + b + " + " + c, a * b + c);
            }
            case 1: {
                int b = randRange(7, 17);
                int q = randRange(7, 17);
                int a = b * q;
                int c = randRange(20, 200);
                return new MathProblem("(" + a + " ÷ " + b + ") + " + c, q + c);
            }
            default: {
                int a = randRange(20, 99);
                int b = randRange(20, 99);
                int c = randRange(2, 11);
                return new MathProblem("(" + a + " + " + b + ") × " + c, (a + b) * c);
            }
        }
    }

    private int randRange(int min, int maxInclusive) {
        return random.nextInt(maxInclusive - min + 1) + min;
    }
}
