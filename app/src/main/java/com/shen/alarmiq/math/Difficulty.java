package com.shen.alarmiq.math;

import com.shen.alarmiq.R;

public enum Difficulty {
    NONE(R.string.difficulty_none, R.string.difficulty_none_desc, 0),
    NORMAL(R.string.difficulty_normal, R.string.difficulty_normal_desc, 3),
    HARD(R.string.difficulty_hard, R.string.difficulty_hard_desc, 3),
    NIGHTMARE(R.string.difficulty_nightmare, R.string.difficulty_nightmare_desc, 5),
    PUNISHMENT(R.string.difficulty_punishment, R.string.difficulty_punishment_desc, 6);

    public final int labelRes;
    public final int descRes;
    public final int problemCount;

    Difficulty(int labelRes, int descRes, int problemCount) {
        this.labelRes = labelRes;
        this.descRes = descRes;
        this.problemCount = problemCount;
    }

    public static Difficulty fromName(String name) {
        if (name == null) return NORMAL;
        // Legacy migration
        if ("DESTROYER".equals(name)) return NIGHTMARE;
        try {
            return Difficulty.valueOf(name);
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
