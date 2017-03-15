package com.olimex.tangram.concept;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.olimex.tangram.GameActivity;
import com.olimex.tangram.GlobalUtils;
import com.olimex.tangram.engine.Animal;

public class SceneView extends View implements View.OnTouchListener, RotationGestureDetector.OnRotationGestureListener {

    private Canvas drawCanvas = new Canvas();
    private Bitmap drawBitmap;
    private Paint paint;

    public SceneView(Context context) {
        this(context, null);
    }

    public SceneView(Context context, AttributeSet attrs) {
        super(context, attrs);

        rotationGestureDetector = new RotationGestureDetector(this);

        drawBitmap = Bitmap.createBitmap(GlobalUtils.width, GlobalUtils.height, Bitmap.Config.ARGB_8888);
        drawCanvas.setBitmap(drawBitmap);
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBitmap.eraseColor(Color.WHITE);
        super.onDraw(drawCanvas);

        if (GameActivity.isMenuScene)
            drawMenuScene(drawCanvas);
        else
            drawGameScene(drawCanvas);

        canvas.drawBitmap(drawBitmap, 0, 0, paint);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        GlobalUtils.setLastAction();

        if (GameActivity.isMenuScene)
            onMenuTouchEvent(event);
        else
            onGameTouchEvent(event);

        invalidate();

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Menu Scene
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String animalNames[] = {Animal.ANIMAL_PARROT, Animal.ANIMAL_HIPO, Animal.ANIMAL_PELICAN, Animal.ANIMAL_DOG,
            Animal.ANIMAL_OWL, Animal.ANIMAL_CAT, Animal.ANIMAL_POSSUM, Animal.ANIMAL_GRASSHOPPER};
    private static Picture animalPictures[] = null;
    private boolean isDown = false;
    private int downAnimal;
    private int curDownAnimal;
    private void drawMenuScene(Canvas canvas) {
        if (animalPictures == null) {
            animalPictures = new Picture[animalNames.length];
            for (int i = 0; i < animalNames.length; i++) {
                String animalPath = "animals/" + animalNames[i] + "/complete.svg";
                animalPictures[i] = GlobalUtils.loadSVGFromAssets(GlobalUtils.getActivity(), animalPath);
            }
        }

        int height = canvas.getHeight();
        int offsetTop = getOffsetTop(height);

        // draw animals

        int canvasWidth = canvas.getWidth();
        int canvasHeight = getMenuAnimalCanvasHeight(height, offsetTop);
        int gapHorizontal = canvasWidth / 20;
        int gapVertical = canvasHeight / 5;
        int animalWidth = (canvasWidth - gapHorizontal * 3) / 4;
        int animalHeight = (canvasHeight - gapVertical) / 2;

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                int k = i * 4 + j;
                float scale = Math.min((float) animalWidth / (float) animalPictures[k].getWidth(),
                        (float) animalHeight / (float) animalPictures[k].getHeight());
                int centerX = (animalWidth + gapHorizontal) * j + animalWidth / 2;
                int centerY = (animalHeight + gapVertical) * i + animalHeight / 2;
                GlobalUtils.drawSVG(canvas, animalPictures[k], centerX, centerY + offsetTop, scale, 0);

                if (isDown && curDownAnimal == downAnimal && curDownAnimal == k) {
                    Rect rect = new Rect(centerX - animalWidth / 2, centerY - animalHeight / 2, centerX + animalWidth / 2, centerY + animalHeight / 2);
                    rect.offset(0, offsetTop);
                    Paint paint = new Paint();
//                    paint.setAlpha(30);
                    paint.setColor(0x4FFFFFFF);
                    canvas.drawRect(rect, paint);
                }
            }
        }
    }

    private int getOffsetTop(int height) {
        return height * 3 / 9;
    }

    private int getMenuAnimalCanvasHeight(int height, int offsetTop) {
        return (height - offsetTop) * 2 / 3;
    }

    private void onMenuTouchEvent(MotionEvent event) {
        int height = getMeasuredHeight();
        int offsetTop = getOffsetTop(height);
        int canvasWidth = getMeasuredWidth();
        int canvasHeight = getMenuAnimalCanvasHeight(height, offsetTop);
        int animalWidth = canvasWidth / 4;
        int animalHeight = canvasHeight / 2;
        int downX = (int) event.getX();
        int downY = (int) event.getY() - offsetTop;

        if (!(downX < 0 || downX >= canvasWidth || downY < 0 || downY >= canvasHeight))
            curDownAnimal = (downY / animalHeight) * 4 + (downX / animalWidth);
        else
            curDownAnimal = -1;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDown = true;
                downAnimal = curDownAnimal;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isDown = false;

                if (curDownAnimal == downAnimal && curDownAnimal != -1)
                    GlobalUtils.getActivity().gotoGame(animalNames[downAnimal]);
                break;
        }
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Animal Scene
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void drawGameScene(Canvas canvas) {
        GameActivity.animal.draw(canvas);
    }

    private RotationGestureDetector rotationGestureDetector;
    private int prevAngle = 0;
    private int prevX, prevY;
    private boolean isRotated;
    private void onGameTouchEvent(MotionEvent event) {
        Animal animal = GameActivity.animal;
        int x = (int) event.getX();
        int y = (int) event.getY();

        rotationGestureDetector.onTouchEvent(event);
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                int selPiece = animal.pickPiece(x, y);

                if (selPiece != -1) {
                    animal.selectPiece(selPiece);
                }
                prevX = x;
                prevY = y;

                prevAngle = 0;
                isRotated = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isRotated && rotationGestureDetector.isRotating())
                    isRotated = true;
                if (animal.getSelectedPieceIndex() != -1 && !isRotated) {
                    animal.move(x - prevX, y - prevY);
                    prevX = x;
                    prevY = y;

                    prevAngle = 0;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (animal.getSelectedPieceIndex() != -1)
                    checkRightPosition();
                break;
        }
    }

    private void checkRightPosition() {
        Animal animal = GameActivity.animal;
        boolean isRightPosition = animal.checkRightPosition();
        if (isRightPosition && animal.isCompleted())
            playWinningFeedback();

        if (isRightPosition)
            handler.postDelayed(runnable, 50);
    }

    private void playWinningFeedback() {
        Animal animal = GameActivity.animal;
        GlobalUtils.getActivity().playSound();
        animal.playWinningFeedback();
    }

    @Override
    public void OnRotation(RotationGestureDetector rotationDetector) {
        int angle = prevAngle - (int) rotationDetector.getAngle();
        prevAngle = (int) rotationDetector.getAngle();

        if (angle == 0)
            return;

        GameActivity.animal.rotate(angle);
    }


    ////////////////////////

    long startTime = 0;
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            invalidate();

            if (startTime == 0)
                startTime = System.currentTimeMillis();

            if (System.currentTimeMillis() - startTime < 10 * 1000)
                handler.postDelayed(runnable, 50);
            else
                startTime = 0;
        }
    };
}
