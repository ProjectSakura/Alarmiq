package com.shen.alarmiq.alarm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Round analog time picker. Touch nearer to a hand to grab it; tap the centre
 * dot to toggle explicitly. Hour-mode releases auto-advance to minute mode.
 */
public class AnalogClockPicker extends View {

    public interface OnTimeChangedListener {
        void onTimeChanged(int hour24, int minute);
    }

    public interface OnModeChangedListener {
        void onModeChanged(boolean hourMode);
    }

    private enum Mode { HOUR, MINUTE }

    private final Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMajor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMinor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hourHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minuteHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int hour24 = 7;
    private int minute = 0;
    private Mode mode = Mode.HOUR;
    /** True when the active hand for the current gesture was chosen explicitly
     * (by tapping closer to one hand than the other). Suppresses the
     * auto-switch-on-release that fires for the standard "pick hour" flow. */
    private boolean explicitGesture = false;

    private float cx, cy, radius;
    private OnTimeChangedListener listener;
    private OnModeChangedListener modeListener;

    public AnalogClockPicker(Context ctx) { this(ctx, null); }

    public AnalogClockPicker(Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    public AnalogClockPicker(Context ctx, @Nullable AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        init();
    }

    private void init() {
        int surfaceVariant = resolveAttrColor("colorSurfaceContainerHigh", 0xFF2A2A2A);
        int outline = resolveAttrColor("colorOutlineVariant", 0xFF444444);
        int primary = resolveAttrColor("colorPrimary", 0xFF7DD3FC);
        int onSurface = resolveAttrColor("colorOnSurface", 0xFFE0E0E0);
        int onSurfaceVariant = resolveAttrColor("colorOnSurfaceVariant", 0xFFAAAAAA);
        int onPrimary = resolveAttrColor("colorOnPrimary", 0xFF000000);

        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setColor(surfaceVariant);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(outline);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        tickMajor.setStyle(Paint.Style.STROKE);
        tickMajor.setStrokeCap(Paint.Cap.ROUND);
        tickMajor.setColor(onSurfaceVariant);

        tickMinor.setStyle(Paint.Style.STROKE);
        tickMinor.setStrokeCap(Paint.Cap.ROUND);
        tickMinor.setColor(outline);

        hourHandPaint.setStyle(Paint.Style.STROKE);
        hourHandPaint.setStrokeCap(Paint.Cap.ROUND);
        hourHandPaint.setColor(primary);

        minuteHandPaint.setStyle(Paint.Style.STROKE);
        minuteHandPaint.setStrokeCap(Paint.Cap.ROUND);
        minuteHandPaint.setColor(primary);
        minuteHandPaint.setAlpha(200);

        numberPaint.setColor(onSurface);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setFakeBoldText(true);

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(primary);

        knobPaint.setStyle(Paint.Style.FILL);
        knobPaint.setColor(primary);

        knobRingPaint.setStyle(Paint.Style.FILL);
        knobRingPaint.setColor(onPrimary);

        setClickable(true);
        setFocusable(true);
    }

    private int resolveAttrColor(String attrName, int fallback) {
        Context ctx = getContext();
        int attrId = ctx.getResources().getIdentifier(attrName, "attr", ctx.getPackageName());
        if (attrId == 0) {
            attrId = ctx.getResources().getIdentifier(attrName, "attr", "android");
        }
        if (attrId == 0) return fallback;
        
        TypedValue tv = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attrId, tv, true)) {
            if (tv.resourceId != 0) {
                return ContextCompat.getColor(ctx, tv.resourceId);
            }
            return tv.data;
        }
        return fallback;
    }

    public void setOnTimeChangedListener(OnTimeChangedListener l) {
        this.listener = l;
    }

    public void setOnModeChangedListener(OnModeChangedListener l) {
        this.modeListener = l;
    }

    public boolean isHourMode() {
        return mode == Mode.HOUR;
    }

    public void setTime(int hour24, int minute) {
        this.hour24 = ((hour24 % 24) + 24) % 24;
        this.minute = ((minute % 60) + 60) % 60;
        invalidate();
    }

    public int getHour24() { return hour24; }
    public int getMinute() { return minute; }

    public void setMode(boolean hourMode) {
        Mode newMode = hourMode ? Mode.HOUR : Mode.MINUTE;
        if (newMode == mode) return;
        this.mode = newMode;
        invalidate();
        if (modeListener != null) modeListener.onModeChanged(hourMode);
    }

    public void toggleMode() {
        setMode(mode != Mode.HOUR);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(w, h);
        if (size <= 0) size = (int) (260 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cx = w / 2f;
        cy = h / 2f;
        radius = Math.min(w, h) / 2f - dp(8);
        trackPaint.setStrokeWidth(dp(2f));
        hourHandPaint.setStrokeWidth(dp(9f));
        minuteHandPaint.setStrokeWidth(dp(5f));
        numberPaint.setTextSize(radius * 0.16f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(cx, cy, radius, facePaint);

        RectF rect = new RectF(cx - radius + dp(4), cy - radius + dp(4),
                cx + radius - dp(4), cy + radius - dp(4));
        canvas.drawArc(rect, 0, 360, false, trackPaint);

        float numberRadius = radius - dp(28);
        Paint.FontMetrics fm = numberPaint.getFontMetrics();
        for (int h = 1; h <= 12; h++) {
            double angle = Math.toRadians(h * 30 - 90);
            float x = cx + (float) Math.cos(angle) * numberRadius;
            float y = cy + (float) Math.sin(angle) * numberRadius;
            float ty = y - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(String.valueOf(h), x, ty, numberPaint);
        }

        float tickStart = radius - dp(6);
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6 - 90);
            float r1 = tickStart;
            float r2 = i % 5 == 0 ? radius - dp(14) : radius - dp(10);
            float x1 = cx + (float) Math.cos(angle) * r1;
            float y1 = cy + (float) Math.sin(angle) * r1;
            float x2 = cx + (float) Math.cos(angle) * r2;
            float y2 = cy + (float) Math.sin(angle) * r2;
            Paint p = i % 5 == 0 ? tickMajor : tickMinor;
            p.setStrokeWidth(dp(i % 5 == 0 ? 2.2f : 1.2f));
            canvas.drawLine(x1, y1, x2, y2, p);
        }

        double hourAngle = Math.toRadians(((hour24 % 12) + minute / 60f) * 30 - 90);
        float hourLen = radius * 0.50f;
        float hx = cx + (float) Math.cos(hourAngle) * hourLen;
        float hy = cy + (float) Math.sin(hourAngle) * hourLen;
        hourHandPaint.setAlpha(mode == Mode.HOUR ? 255 : 110);
        canvas.drawLine(cx, cy, hx, hy, hourHandPaint);

        double minAngle = Math.toRadians(minute * 6 - 90);
        float minLen = radius * 0.78f;
        float mx = cx + (float) Math.cos(minAngle) * minLen;
        float my = cy + (float) Math.sin(minAngle) * minLen;
        minuteHandPaint.setAlpha(mode == Mode.MINUTE ? 230 : 100);
        canvas.drawLine(cx, cy, mx, my, minuteHandPaint);

        // Knob on the active hand
        float knobX = mode == Mode.HOUR ? hx : mx;
        float knobY = mode == Mode.HOUR ? hy : my;
        canvas.drawCircle(knobX, knobY, dp(16f), knobPaint);
        canvas.drawCircle(knobX, knobY, dp(5f), knobRingPaint);

        canvas.drawCircle(cx, cy, dp(7f), centerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (Math.hypot(x - cx, y - cy) < dp(28f)) {
                    toggleMode();
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    explicitGesture = true;
                    return true;
                }
                explicitGesture = pickActiveHand(x, y);
                updateFromTouch(x, y);
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                updateFromTouch(x, y);
                return true;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (mode == Mode.HOUR && !explicitGesture) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    setMode(false);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Picks whichever hand the touch is closer to (by angular distance).
     * Returns true if the touch caused a deliberate mode switch — used to
     * suppress the post-release auto-advance.
     */
    private boolean pickActiveHand(float x, float y) {
        double dx = x - cx;
        double dy = y - cy;
        double touchAngle = normalize(Math.toDegrees(Math.atan2(dy, dx)) + 90);
        double hourAngle = normalize(((hour24 % 12) + minute / 60f) * 30);
        double minuteAngle = normalize(minute * 6);
        double distHour = angularDist(touchAngle, hourAngle);
        double distMinute = angularDist(touchAngle, minuteAngle);

        boolean preferHour = distHour + 8 < distMinute; // 8° bias toward minute hand
        Mode desired = preferHour ? Mode.HOUR : Mode.MINUTE;
        if (desired == mode) return false;
        setMode(desired == Mode.HOUR);
        return true;
    }

    private static double normalize(double deg) {
        deg = deg % 360;
        if (deg < 0) deg += 360;
        return deg;
    }

    private static double angularDist(double a, double b) {
        double d = Math.abs(a - b) % 360;
        return d > 180 ? 360 - d : d;
    }

    private void updateFromTouch(float x, float y) {
        double dx = x - cx;
        double dy = y - cy;
        double angle = normalize(Math.toDegrees(Math.atan2(dy, dx)) + 90);
        if (mode == Mode.HOUR) {
            int newHour12 = (int) Math.round(angle / 30f);
            if (newHour12 == 0) newHour12 = 12;
            boolean wasPm = hour24 >= 12;
            int base = newHour12 % 12;
            hour24 = base + (wasPm ? 12 : 0);
        } else {
            int newMin = (int) Math.round(angle / 6f);
            if (newMin >= 60) newMin -= 60;
            if (newMin != minute) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
            minute = newMin;
        }
        invalidate();
        if (listener != null) listener.onTimeChanged(hour24, minute);
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
