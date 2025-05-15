package org.androidtown.ppppp.pt;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public class TimeGridSelector extends View {

    private static final int CELL_WIDTH = 100;
    private static final int CELL_HEIGHT = 100;
    private static final int COLUMNS = 7;  // 요일 수
    private static final int ROWS = 13;    // 시간 수 (10~22시)
    private static final int TOP_MARGIN = 80;
    private static final int LEFT_MARGIN = 120;

    private final Set<String> selectedTimes = new HashSet<>();
    private final Paint gridPaint = new Paint();
    private final Paint selectedPaint = new Paint();
    private final Paint labelPaint = new Paint();
    private final Paint backgroundPaint = new Paint();

    private boolean isDragging = false;
    private Boolean draggingToSelect = null;
    private String lastTouchedKey = null;  // 추가

    private final String[] daysKor = {"월", "화", "수", "목", "금", "토", "일"};
    private final int[] hours = {10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};

    public TimeGridSelector(Context context, AttributeSet attrs) {
        super(context, attrs);

        gridPaint.setColor(Color.DKGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);

        selectedPaint.setColor(Color.parseColor("#4CAF50")); // 더 진한 초록
        selectedPaint.setStyle(Paint.Style.FILL);

        backgroundPaint.setColor(Color.WHITE);

        labelPaint.setColor(Color.DKGRAY);
        labelPaint.setTextSize(28f);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        labelPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 배경
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        // 상단 요일 라벨
        for (int col = 0; col < COLUMNS; col++) {
            int x = LEFT_MARGIN + col * CELL_WIDTH + CELL_WIDTH / 4;
            canvas.drawText(daysKor[col], x, TOP_MARGIN - 20, labelPaint);
        }

        // 왼쪽 시간 라벨 + 그리드 셀
        for (int row = 0; row < ROWS; row++) {
            int y = TOP_MARGIN + row * CELL_HEIGHT + CELL_HEIGHT / 2;
            canvas.drawText(hours[row] + "시", 20, y, labelPaint);

            for (int col = 0; col < COLUMNS; col++) {
                int left = LEFT_MARGIN + col * CELL_WIDTH;
                int top = TOP_MARGIN + row * CELL_HEIGHT;
                int right = left + CELL_WIDTH;
                int bottom = top + CELL_HEIGHT;

                String key = daysKor[col] + "-" + hours[row];

                // 선택된 셀은 진한 초록색으로 채움
                if (selectedTimes.contains(key)) {
                    canvas.drawRect(left, top, right, bottom, selectedPaint);
                }

                // 그리드 테두리
                canvas.drawRect(left, top, right, bottom, gridPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        int col = (int) ((x - LEFT_MARGIN) / CELL_WIDTH);
        int row = (int) ((y - TOP_MARGIN) / CELL_HEIGHT);

        if (col < 0 || col >= COLUMNS || row < 0 || row >= ROWS) return false;

        String key = daysKor[col] + "-" + hours[row];

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                draggingToSelect = !selectedTimes.contains(key);
                lastTouchedKey = key;
                applyDrag(key);
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && draggingToSelect != null && !key.equals(lastTouchedKey)) {
                    lastTouchedKey = key;
                    applyDrag(key);
                }
                break;

            case MotionEvent.ACTION_UP:
                isDragging = false;
                draggingToSelect = null;
                lastTouchedKey = null;
                break;
        }

        return true;
    }

    private void applyDrag(String key) {
        if (draggingToSelect && !selectedTimes.contains(key)) {
            selectedTimes.add(key);
            invalidate();
        } else if (!draggingToSelect && selectedTimes.contains(key)) {
            selectedTimes.remove(key);
            invalidate();
        }
    }

    private void toggleCell(String key, boolean select) {
        boolean changed = false;
        if (select && !selectedTimes.contains(key)) {
            selectedTimes.add(key);
            changed = true;
        } else if (!select && selectedTimes.contains(key)) {
            selectedTimes.remove(key);
            changed = true;
        }

        if (changed) invalidate();
    }

    public Set<String> getSelectedTimes() {
        return selectedTimes;
    }

    public void clearSelection() {
        selectedTimes.clear();
        invalidate();
    }
}

