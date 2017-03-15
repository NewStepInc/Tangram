package com.olimex.tangram.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import com.olimex.tangram.GlobalUtils;

import java.util.ArrayList;

public class Piece {
    public static final int PIECE_STATE_NORMAL      = 0x00;
    public static final int PIECE_STATE_SELECTED    = 0x01;
    public static final int PIECE_STATE_LOCATED     = 0xFF;

    private int id;
    private PointF ptCurrent = new PointF();
    private float fScale;
    private int angleCurrent;
    private int symmetryCycle;
    private int state = PIECE_STATE_NORMAL;
    private Picture picPiece;
    private Bitmap bmpPiece = null;
    private boolean showLocated = false;
    private long locatedTime;

    private Paint paint = new Paint();
    {
        paint.setAntiAlias(true);
    }

    private Animal animal = null;

    public Piece(Animal animal) {
        this.animal = animal;
    }

    public Piece setID(int id) {
        this.id = id;
        return this;
    }

    public int getID() {
        return this.id;
    }

    public Piece addState(int state) {
        this.state |= state;
        if (state == PIECE_STATE_LOCATED)
            locatedTime = System.currentTimeMillis();

        return this;
    }

    public Piece removeState(int state) {
        this.state &= state ^ 0xFF;
        return this;
    }

    public boolean checkState(int state) {
        if (state == PIECE_STATE_LOCATED)
            return this.state == state;

        return (state & this.state) != 0;
    }

    public Piece setPtCurrent(PointF ptCurrent) {
        this.ptCurrent.x = ptCurrent.x * fScale;
        this.ptCurrent.y = ptCurrent.y * fScale;
        return this;
    }

    public Piece setAngleCurrent(int angleCurrent) {
        this.angleCurrent = angleCurrent;

        if (angleCurrent < 360)
            symmetryCycle = 360;
        else if (angleCurrent < 720)
            symmetryCycle = 1;
        else if (angleCurrent < 900)
            symmetryCycle = 180;
        else
            symmetryCycle = 90;

        this.angleCurrent = (this.angleCurrent + symmetryCycle) % symmetryCycle;
        return this;
    }

    public Piece setSVGPiece(Picture picture) {
        this.picPiece = picture;

        this.fScale = animal.getScale();
        float toWidth = picture.getWidth() * fScale + 2.5f;
        float toHeight = picture.getHeight() * fScale + 2.5f;

        bmpPiece = Bitmap.createBitmap((int) toWidth, (int) toHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpPiece);
        float centerX = toWidth / 2;
        float centerY = toHeight / 2;
        GlobalUtils.drawSVG(canvas, picture, centerX+1, centerY, fScale, 0);
        GlobalUtils.drawSVG(canvas, picture, centerX-1, centerY, fScale, 0);
        GlobalUtils.drawSVG(canvas, picture, centerX, centerY+1, fScale, 0);
        GlobalUtils.drawSVG(canvas, picture, centerX, centerY-1, fScale, 0);

        return this;
    }

    public Piece move(PointF offset) {
        if (ptCurrent.x + offset.x < 0 || ptCurrent.x + offset.x > GlobalUtils.width ||
            ptCurrent.y + offset.y < 0 || ptCurrent.y + offset.y > GlobalUtils.height)
            return this;
        this.ptCurrent.offset(offset.x, offset.y);
        return this;
    }

    public Piece rotate(int offset) {
        angleCurrent = (angleCurrent + offset + symmetryCycle) % symmetryCycle;
        return this;
    }

    public boolean ptInPiece(int x, int y) {
        if (checkState(PIECE_STATE_LOCATED))
            return false;

        PointF point = new PointF(ptCurrent.x, ptCurrent.y);
        float xx = x - point.x;
        float yy = y - point.y;
        float dist = (float) Math.sqrt(xx*xx + yy*yy);

        if (!checkState(PIECE_STATE_SELECTED))
            dist /= 0.8f;

        if (dist == 0)
            return (Color.alpha(bmpPiece.getPixel(bmpPiece.getWidth()/2, bmpPiece.getHeight()/2)) != 0);

        double angle = Math.atan2(yy, xx) - Math.toRadians(angleCurrent);
        x = (int) (bmpPiece.getWidth() / 2 + dist * Math.cos(angle));
        y = (int) (bmpPiece.getHeight() / 2 + dist * Math.sin(angle));

        if (x < 0 || x >= bmpPiece.getWidth() ||
            y < 0 || y >= bmpPiece.getHeight())
            return false;

        int color = bmpPiece.getPixel(x, y);
//        Log.d("pick color", String.valueOf(id) + " : (" +  Color.alpha(color) + ", " + Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color) + ")");

        return (Color.alpha(color) != 0);
    }

    public PointF getPosition() {
        return ptCurrent;
    }

    public boolean checkRightPosition() {
        int angle = angleCurrent;
        if (Math.abs(angle % symmetryCycle) > 5 && Math.abs(angle % symmetryCycle) < symmetryCycle - 5) // angle delta is out of 5 degree
            return false;

        PointF ptAnimal = animal.getPosition();
        ArrayList<RightPosition> aryPositions = animal.getRightPosition(id);
        for (RightPosition rightPosition : aryPositions) {
            PointF ptRight = new PointF(rightPosition.getRightX() * fScale, rightPosition.getRightY() * fScale);
            ptRight.x += ptAnimal.x;
            ptRight.y += ptAnimal.y;

            float xx = ptCurrent.x - ptRight.x;
            float yy = ptCurrent.y - ptRight.y;
            double dist = Math.sqrt(xx * xx + yy * yy);
            double positionDelta = GlobalUtils.getPercentalSize(2, false);
            if (dist < positionDelta) {     // position delta is 2% of small edge of screen
                ptCurrent.x = ptRight.x;
                ptCurrent.y = ptRight.y;
                angleCurrent = 0;
                addState(PIECE_STATE_LOCATED);

                animal.removePosition(rightPosition);
                return true;
            }
        }

        return false;
    }

    public void draw(Canvas canvas) {
        float fScale = this.fScale;
        if (!checkState(PIECE_STATE_SELECTED))
            fScale *= 0.8f;

        if (checkState(PIECE_STATE_LOCATED)) {
            if (showLocated || (System.currentTimeMillis() - locatedTime) > 600 || (System.currentTimeMillis() - locatedTime) % 300 < 150) {
                paint.setColorFilter(null);
            } else {
                paint.setColorFilter(animal.getColorFilter());
            }

            if (!showLocated && (System.currentTimeMillis() - locatedTime) > 600)
                showLocated = true;
        } else
            paint.setColorFilter(null);

        if (paint.getColorFilter() == null && !checkState(PIECE_STATE_LOCATED))
            GlobalUtils.drawSVG(canvas, picPiece, ptCurrent.x, ptCurrent.y, fScale, angleCurrent);
        else
            GlobalUtils.drawBitmap(canvas, bmpPiece, ptCurrent.x, ptCurrent.y, angleCurrent, paint);
    }

    private static ColorFilter shadowColorFilter = new PorterDuffColorFilter(0xFFA2A2A2, PorterDuff.Mode.SRC_ATOP);
    public void drawShadow(Canvas canvas, float x, float y) {
        paint.setColorFilter(shadowColorFilter);
        x += animal.getPosition().x;
        y += animal.getPosition().y;
        GlobalUtils.drawBitmap(canvas, bmpPiece, x, y, 0, paint);
    }

    public void freeBitmap() {
        bmpPiece.recycle();
        bmpPiece = null;
    }
}
