package com.example.chris.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

import edu.uml.odgboxtherapy.R;
import glfont.TexturePack;

/**
 * Created by chris on 1/13/16.
 */
public class TextOverlay extends View {
    private String onScreenMessage = "";
    private int message_width, message_height;
    private final Paint paint;
    private Bitmap completionScreen;

    public TextOverlay(Context context) {
        super(context);

        paint = new Paint();
        paint.setTextSize(40);

        //ascent returns a negative number
        message_height = -1*(int)paint.ascent();
        completionScreen = BitmapFactory.decodeResource(getResources(), R.drawable.ribbon_banner);
    }

    public boolean setHeaderText(String msg) {
        onScreenMessage = msg;
        message_width = (int)paint.measureText(onScreenMessage);

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        ExerciseActivity activity = ExerciseActivity.getInstance();

        if(activity.getStateSub() != null) {
            paint.setColor(Color.GREEN);
            canvas.drawRect(0, getHeight() - 33 - message_height,
                    (int) (activity.getPlayerStats().getProgress() * getWidth()),
                    getHeight(), paint);
        }

        paint.setColor(Color.WHITE);
        canvas.drawText(onScreenMessage, getWidth() / 2 - message_width / 2, message_height + 20, paint);

        paint.setColor(Color.BLACK);
        canvas.drawText("Progress:", 10, getHeight() - 20, paint);

        if(activity.getProgramStatus() == ExerciseActivity.ProgramStatus.COMPLETE) {
            canvas.drawBitmap(completionScreen, getWidth()/2 - completionScreen.getScaledWidth(canvas)/2 + 5,
                    getHeight()/2 - completionScreen.getScaledHeight(canvas)/2, paint);

            canvas.drawText(activity.getPlayerStats().getTimeElapsed() + " seconds", getWidth() / 2, getHeight() / 2 - 132, paint);
            canvas.drawText(activity.getPlayerStats().getPromptCount()
                            + " demo" + (activity.getPlayerStats().getPromptCount()!=1?"s":""),
                    getWidth()/2, getHeight()/2 + 20, paint);
        }

    }
}
