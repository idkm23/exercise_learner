package com.example.chris.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by chris on 1/13/16.
 */
public class LoadingView extends View {
    private final Paint paint;
    private final String MESSAGE = "Loading exercise";
    private int msg_x = -1, msg_y = -1;
    private double dot_count = 0.5;
    private double dot_increment = .025f;

    public LoadingView(Context context) {
        super(context);

        setBackgroundColor(Color.BLACK);
        paint = new Paint();
        paint.setTextSize(100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(msg_x == -1 && msg_y == -1) {
            msg_x = (int) (getWidth() / 2.0 - paint.measureText(MESSAGE) / 2.0);
            msg_y = (int) (getHeight() / 2.0 - paint.ascent() / 2);
        }

        String dots = "";
        for(int i = 0; i < (int)dot_count; i++) {
            dots += " . ";
        }

        paint.setColor(Color.GRAY);
        canvas.drawText(MESSAGE + dots, msg_x + 1, msg_y - 1, paint);

        paint.setColor(Color.WHITE);
        canvas.drawText(MESSAGE + dots, msg_x, msg_y, paint);

        if((dot_count += dot_increment) > 3.5 || dot_count < 0.5) {
            dot_increment *= -1;
        };

        invalidate();
    }
}
