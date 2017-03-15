package com.olimex.tangram;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import com.larvalabs.svgandroid.SVGParser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GlobalUtils {

    private static final long NOACTION_PERIOD_MAINMENU = 1000 * 60 * 2; // 2 min
    private static final long NOACTION_PERIOD_GAME = 1000 * 60 * 4; // 4 min

    private static GameActivity activity;
    public static final int width = 1920, height = 1080;

    private static Handler handler = new Handler();
    private static Runnable runnable = new Runnable() {
        @Override
        public void run() {
            activity.goBack(null);
        }
    };

    public static void setLastAction() {
        handler.removeCallbacks(runnable);

        if (GameActivity.isMenuScene)
            handler.postDelayed(runnable, NOACTION_PERIOD_MAINMENU);
        else
            handler.postDelayed(runnable, NOACTION_PERIOD_GAME);
    }

    public static GameActivity getActivity() {
        return activity;
    }
    public static void setActivity(GameActivity activity) {
        GlobalUtils.activity = activity;
    }

    public static Picture loadSVGFromAssets(Context context, String filename) {
        try {
            return SVGParser.getSVGFromAsset(context.getAssets(), filename).getPicture();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                v.setAlpha(0.3f);
            else if (event.getAction() == MotionEvent.ACTION_UP)
                v.setAlpha(1f);

            return false;
        }
    };

    public static Point getDimentionalSize()
    {
        Display display = activity.getWindowManager().getDefaultDisplay();
        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17){
            //new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            //reflection for this weird in-between time
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                //this may not be 100% accurate, but it's all we've got
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
            }

        } else {
            //This should be close, as lower API devices should not have window navigation bars
            realWidth = display.getWidth();
            realHeight = display.getHeight();
        }

        if (realHeight < realWidth) {
            realWidth += realHeight;
            realHeight = realWidth - realHeight;
            realWidth -= realHeight;
        }

        return new Point(realWidth, realHeight);
    }

    public static float getPercentalSize(float percent, boolean isOfLongEdge) {
        Point size = getDimentionalSize();
        int srcSize = isOfLongEdge ? size.y : size.x;
        return percent * srcSize / 100.0f;
    }

    public static void drawBitmap(Canvas canvas, Bitmap bmp, float x, float y, int angle, Paint paint) {
        Matrix matrix = new Matrix();
        matrix.postTranslate(-bmp.getWidth() / 2f, -bmp.getHeight() / 2f);
        matrix.postRotate(angle, 0, 0);
        matrix.postTranslate(x, y);

        canvas.drawBitmap(bmp, matrix, paint);
    }

    public static void drawSVG(Canvas canvas, Picture picture, float x, float y, float scale, int angle) {
        Matrix matrix = new Matrix();

        matrix.postTranslate(-picture.getWidth() / 2f, -picture.getHeight() / 2f);
        matrix.postRotate(angle, 0, 0);
        matrix.postScale(scale, scale);
        matrix.postTranslate(x, y);

        canvas.setMatrix(matrix);
        canvas.drawPicture(picture);
        canvas.setMatrix(null);
    }

    public static MediaPlayer getMediaPlayer(Context context){

        MediaPlayer mediaplayer = new MediaPlayer();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            return mediaplayer;
        }

        try {
            Class<?> cMediaTimeProvider = Class.forName( "android.media.MediaTimeProvider" );
            Class<?> cSubtitleController = Class.forName( "android.media.SubtitleController" );
            Class<?> iSubtitleControllerAnchor = Class.forName( "android.media.SubtitleController$Anchor" );
            Class<?> iSubtitleControllerListener = Class.forName( "android.media.SubtitleController$Listener" );

            Constructor constructor = cSubtitleController.getConstructor(Context.class, cMediaTimeProvider, iSubtitleControllerListener);

            Object subtitleInstance = constructor.newInstance(context, null, null);

            Field f = cSubtitleController.getDeclaredField("mHandler");

            f.setAccessible(true);
            try {
                f.set(subtitleInstance, new Handler());
            }
            catch (IllegalAccessException e) {return mediaplayer;}
            finally {
                f.setAccessible(false);
            }

            Method setsubtitleanchor = mediaplayer.getClass().getMethod("setSubtitleAnchor", cSubtitleController, iSubtitleControllerAnchor);

            setsubtitleanchor.invoke(mediaplayer, subtitleInstance, null);
            //Log.e("", "subtitle is setted :p");
        } catch (Exception ignored) {}

        return mediaplayer;
    }

    public static void makeFullScreen(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        }
        decorView.setSystemUiVisibility(uiOptions);
    }
}