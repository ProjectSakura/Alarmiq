package com.shen.alarmiq.math;

import java.util.Random;

public class MathProblemGenerator {

    private final Random random = new Random();

    public MathProblem next(Difficulty difficulty) {
        switch (difficulty) {
            case NONE: return null;
            case HARD: return hard();
            case NIGHTMARE:
            case PUNISHMENT: return destroyer();
            case NORMAL:
            default: return normal();
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
        int form = random.nextInt(4);
        switch (form) {
            case 0: {
                int a = randRange(15, 45);
                int b = randRange(7, 15);
                int c = randRange(15, 45);
                int d = randRange(7, 15);
                return new MathProblem(a + " × " + b + " + " + c + " × " + d, a * b + c * d);
            }
            case 1: {
                int a = randRange(100, 999);
                int b = randRange(100, 999);
                int c = randRange(100, 999);
                return new MathProblem(a + " + " + b + " − " + c, a + b - c);
            }
            case 2: {
                int a = randRange(11, 19);
                int b = randRange(11, 19);
                int c = randRange(11, 19);
                return new MathProblem(a + " × " + b + " × " + c, a * b * c);
            }
            default: {
                int a = randRange(25, 99);
                int b = randRange(25, 99);
                int c = randRange(3, 12);
                int d = randRange(10, 100);
                return new MathProblem("(" + a + " + " + b + ") × " + c + " − " + d, (a + b) * c - d);
            }
        }
    }

    private int randRange(int min, int maxInclusive) {
        return random.nextInt(maxInclusive - min + 1) + min;
    }
}
