package com.example.hr_broadcast;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class HeartbeatAnimationView extends View {

    private Paint paint;
    private float[] waveformData = new float[500]; // Ukladáme hodnoty na vykreslenie
    private int currentIndex = 0;
    private int heartRate = 70; // Štandardná hodnota BPM
    private boolean newHeartRateReceived = false;

    public HeartbeatAnimationView(Context context) {
        super(context);
        init();
    }

    public HeartbeatAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeartbeatAnimationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
    }

    // Metóda na aktualizáciu srdcovej frekvencie a pridanie PQRST pulzu
    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
        newHeartRateReceived = true;
        invalidate(); // Obnoví vykreslenie
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2;

        // Posunutie dát v poli pre simuláciu pohybu vpred
        for (int i = 0; i < waveformData.length - 1; i++) {
            waveformData[i] = waveformData[i + 1];
        }

        // Pridanie nového srdcového pulzu
        if (newHeartRateReceived) {
            int pulseWidth = 60; // Trvanie pulzu v jednotkách

            for (int i = 0; i < pulseWidth; i++) {
                if (i < pulseWidth * 0.2) {
                    // P vlna - mierne stúpanie
                    waveformData[waveformData.length - pulseWidth + i] = heartRate / 10.0f;
                } else if (i < pulseWidth * 0.4) {
                    // Q - malý pokles
                    waveformData[waveformData.length - pulseWidth + i] = -heartRate / 5.0f;
                } else if (i < pulseWidth * 0.5) {
                    // R - ostrý hrot hore
                    waveformData[waveformData.length - pulseWidth + i] = heartRate / 2.0f;
                } else if (i < pulseWidth * 0.6) {
                    // S - prudký pád dole
                    waveformData[waveformData.length - pulseWidth + i] = -heartRate / 3.0f;
                } else {
                    // T vlna - mierne vyrovnanie
                    waveformData[waveformData.length - pulseWidth + i] = heartRate / 15.0f;
                }
            }

            newHeartRateReceived = false;
        }

        // Vykreslenie vlnového priebehu
        float prevX = 0;
        float prevY = centerY;

        for (int i = 0; i < width; i++) {
            int dataIndex = (i * waveformData.length) / (int) width;
            float y = centerY - waveformData[dataIndex];

            canvas.drawLine(prevX, prevY, i, y, paint);
            prevX = i;
            prevY = y;
        }

        // Plynulá aktualizácia grafu
        postInvalidateDelayed(5);
    }

}
