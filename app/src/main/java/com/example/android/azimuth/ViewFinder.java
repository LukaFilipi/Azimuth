package com.example.android.azimuth;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

/**
 * A custom View class that defines a viewfinder to be placed on top of a camera preview.
 * Its purpose is to make it easier to maintain the same spatial resolution of the photographed object
 * at different angles.
 * It includes a square of the same width as the screen drawn in the centre of the screen
 * as well as a square of the same size rotated by 45 degrees.
 */

public class ViewFinder extends View {
    private final String TAG = "ViewFinder";

    // Thickness of the paint strokes in dp
    private static final int STROKE_WIDTH = 4;
    private static final int GRID_STROKE = 2;
    private static final int GRID_DIVISIONS = 4;

    // Colors of the straight and rotated squares composing the viewfinder
    private final int STRAIGHT_COLOR = Color.WHITE;
    private final int ROTATED_COLOR = Color.RED;

    // Rectangle to be drawn on the screen
    private Rect rect = new Rect(0, 0, 0, 0);
    // Paints used to draw the 2 rectangles composing the viewfinder and the grids
    private Paint straightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint rotatedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint straightGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint rotatedGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Length of side of the square
    private int sideLength;
    // Coordinates of top left corner of the square
    private int x, y;


    public ViewFinder(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initPaint();

    }

    // Method to set the parameters of the paint used to draw viewfinder
    private void initPaint() {
        straightPaint.setColor(STRAIGHT_COLOR);
        straightPaint.setStyle(Paint.Style.STROKE);
        straightPaint.setStrokeWidth(dpToPixels(STROKE_WIDTH));

        rotatedPaint.setColor(ROTATED_COLOR);
        rotatedPaint.setStyle(Paint.Style.STROKE);
        rotatedPaint.setStrokeWidth(dpToPixels(STROKE_WIDTH));

        straightGridPaint.setColor(STRAIGHT_COLOR);
        straightGridPaint.setStyle(Paint.Style.STROKE);
        straightGridPaint.setStrokeWidth(dpToPixels(GRID_STROKE));

        rotatedGridPaint.setColor(ROTATED_COLOR);
        rotatedGridPaint.setStyle(Paint.Style.STROKE);
        rotatedGridPaint.setStrokeWidth(dpToPixels(GRID_STROKE));
    }

    /**
     * Method to calculate the side of the viewfinder square based on the
     * dimensions of the screen.
     */
    private int calculateSideLength() {
        int length = 0;
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        // Set the side length to the smaller screen dimension
        if (width > height) {
            length = height;
        } else {
            length = width;
        }

        return length;
    }

    private int dpToPixels(int dp) {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    // Function to draw grid with divs subdivisions within rectangle rect
    // x, y are the coordinates of top left corner of rect
    private void drawGrid(Canvas canvas, Rect rectangle, Paint paint, int x, int y, int divs) {
        int w = rectangle.width();
        int h = rectangle.height();
        int dw = w/divs;
        int dh = h/divs;

        for(int i = 1; i < divs; i++) {
            canvas.drawLine(x + i*dw, y, x + i*dw, y + h, paint);
            canvas.drawLine(x, y + i*dh, x + w, y + i*dh, paint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        sideLength = calculateSideLength();
        x = (getMeasuredWidth() - sideLength)/2;
        y = 0;

        // Draw the straight viewfinder
        rect.set(x, y, x+sideLength, sideLength);
        canvas.drawRect(rect, straightPaint);
        drawGrid(canvas, rect, straightGridPaint, x, y, GRID_DIVISIONS);

        // Draw the rotated viewfinder
        canvas.rotate(45, rect.centerX(), rect.centerY());
        canvas.drawRect(rect, rotatedPaint);
        drawGrid(canvas, rect, rotatedGridPaint, x, y, GRID_DIVISIONS);
        canvas.restore();
    }
}
