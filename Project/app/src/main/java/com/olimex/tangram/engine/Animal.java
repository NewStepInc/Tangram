package com.olimex.tangram.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PointF;
import android.util.Log;
import android.util.SparseArray;

import com.olimex.tangram.GlobalUtils;
import com.olimex.tangram.concept.ColorFilterGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Animal {
    private static final String TAG = "Tangram-Animal";
    private static final String JSON_FILENAME = "animals.json";
    private static String jsonFileContent = null;

    public static final String ANIMAL_PARROT        = "parrot";
    public static final String ANIMAL_OWL           = "owl";
    public static final String ANIMAL_CAT           = "cat";
    public static final String ANIMAL_DOG           = "dog";
    public static final String ANIMAL_GRASSHOPPER   = "grasshopper";
    public static final String ANIMAL_PELICAN       = "pelican";
    public static final String ANIMAL_POSSUM        = "possum";
    public static final String ANIMAL_HIPO          = "hipo";

    private static String readJsonFile(String fileName) {
        String json = null;
        try {
            InputStream is = GlobalUtils.getActivity().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e(TAG, "readJsonFile - load json file from assets");
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static Animal generateAnimal(String animalName) {
        if (jsonFileContent == null)
            jsonFileContent = Animal.readJsonFile(JSON_FILENAME);

        if (jsonFileContent == null || animalName == null || animalName.isEmpty())
            return null;

        Animal animal = new Animal(animalName);


        try {
            JSONObject jsonObject = new JSONObject(jsonFileContent);
            JSONObject jsonAnimal = jsonObject.getJSONObject(animalName);
            String strCompletedImageName = "animals/" + animalName + "/complete.svg";
            animal.setSVGCompleted(GlobalUtils.loadSVGFromAssets(GlobalUtils.getActivity(), strCompletedImageName));
            animal.initColorFilter();

            ///////////////////////
            // load piece data
            ///////////////////////
            JSONArray jsonPieces = jsonAnimal.getJSONArray("pieces");
            for (int i = 0; i < jsonPieces.length(); i++) {
                Piece piece = new Piece(animal);
                JSONObject jsonPiece = jsonPieces.getJSONObject(i);
                    piece.setID(jsonPiece.getInt("id"));

                    // piece image
                    String strImageName = "animals/" + animalName + "/" + piece.getID() + ".svg";
                    piece.setSVGPiece(GlobalUtils.loadSVGFromAssets(GlobalUtils.getActivity(), strImageName));//, orgWidth, orgHeight);

                    piece.setAngleCurrent(jsonPiece.getInt("angle"));
                    piece.setPtCurrent(new PointF(jsonPiece.getInt("curX"), jsonPiece.getInt("curY")));
                animal.addPiece(piece);
            }

            ///////////////////////
            // load right positions data
            ///////////////////////
            JSONArray jsonPositions = jsonAnimal.getJSONArray("positions");
            for (int i = 0; i < jsonPositions.length(); i++) {
                JSONObject jsonPosition = jsonPositions.getJSONObject(i);
                RightPosition position = new RightPosition();

                try {
                    String posString = jsonPosition.getString("position");
                    posString = posString.substring(1, posString.length() - 1).replace(",", "");
                    String poss[] = posString.split(" ");
                    position.setRightX(Float.parseFloat(poss[0]));
                    position.setRightY(Float.parseFloat(poss[1]));
                } catch (Exception e) {
                    position.setRightX((float) jsonPosition.getDouble("rightX"));
                    position.setRightY((float) jsonPosition.getDouble("rightY"));
                }
                    JSONArray jsonIDs = jsonPosition.getJSONArray("ids");
                    for (int j = 0; j < jsonIDs.length(); j++) {
                        JSONObject jsonID = jsonIDs.getJSONObject(j);
                        position.addID(jsonID.getInt("id"));
                    }

                animal.addPosition(position);
            }

        } catch (JSONException e) {
            Log.e(TAG, "createAnimal - read json format");
            e.printStackTrace();
            animal = null;
        }

        return animal;
    }



    private String animalName;
    private Picture picCompleted;
    private Bitmap bmpCompleted = null;
    private ArrayList<Piece> aryPiece = new ArrayList<>();
    private SparseArray<Piece> mapPiece = new SparseArray<>();
    private ArrayList<RightPosition> aryPosition = new ArrayList<>();
    ColorFilter cf = null;

    private boolean isSelected = false;
    private Paint paint = new Paint();
    private float fScale;

    public void freeMemory() {
        System.gc();

        bmpCompleted.recycle();
        bmpCompleted = null;

        while (mapPiece.size() > 0) {
            mapPiece.get(mapPiece.keyAt(0)).freeBitmap();
            mapPiece.removeAt(0);
        }
    }

    public String getAnimalName() {
        return this.animalName;
    }

    public Animal(String animalName) {
        this.animalName = animalName;
    }

    public void initColorFilter() {
        int hue, saturation;
        switch (animalName) {
            case ANIMAL_GRASSHOPPER:
                hue = -100;
                saturation = 50;
                cf = new ColorFilterGenerator.Builder()
                        .setHue(hue)
                        .setSaturation(saturation)
                        .build();
                break;
            case ANIMAL_PELICAN:
                hue = 150;
                saturation = 50;
                cf = new ColorFilterGenerator.Builder()
                        .setHue(hue)
                        .setSaturation(saturation)
                        .build();
                break;
            case ANIMAL_HIPO:
                hue = -135;
                saturation = 65;
                cf = new ColorFilterGenerator.Builder()
                        .setHue(hue)
                        .setSaturation(saturation)
                        .build();
                break;
            default:
                ColorMatrix colorMatrix_Inverted =
                        new ColorMatrix(new float[] {
                                -1,  0,  0,  0, 255,
                                0, -1,  0,  0, 255,
                                0,  0, -1,  0, 255,
                                0,  0,  0,  1,   0});
                cf = new ColorMatrixColorFilter(colorMatrix_Inverted);
        }
    }

    public void setSVGCompleted(Picture picture) {
        this.picCompleted = picture;
        float ratio = (float) picture.getWidth() / (float) picture.getHeight();
        int toWidth = (int) GlobalUtils.getPercentalSize(80, false);
        int toHeight = (int) ((float) toWidth / ratio);
        fScale = (float) toWidth / picture.getWidth();

        animalPosition.x = GlobalUtils.getPercentalSize(65, true);
        animalPosition.y = GlobalUtils.getPercentalSize(50, false);

        bmpCompleted = Bitmap.createBitmap(toWidth, toHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpCompleted);
        GlobalUtils.drawSVG(canvas, picCompleted, toWidth / 2, toHeight / 2, fScale, 0);
    }

    public float getScale() {
        return fScale;
    }

    PointF animalPosition = new PointF();
    public PointF getPosition() {
        return animalPosition;
    }

    public Picture getSVGCompleted() {
        return this.picCompleted;
    }

    public Animal addPiece(Piece piece)
    {
        this.aryPiece.add(piece);
        this.mapPiece.put(piece.getID(), piece);
        return this;
    }

    public Animal addPosition(RightPosition position) {
        this.aryPosition.add(position);
        return this;
    }

    public Animal removePosition(RightPosition position) {
        this.aryPosition.remove(position);
        return this;
    }

    public ArrayList<RightPosition> getRightPosition(int id) {
        ArrayList<RightPosition> aryPositions = new ArrayList<RightPosition>();
        for (RightPosition rightPosition : aryPosition) {
            if (rightPosition.hasID(id))
                aryPositions.add(rightPosition);
        }
        return aryPositions;
    }

    public ColorFilter getColorFilter() {
        return cf;
    }

    private long matchedTime = -1;
    private int flashCount;

    public boolean draw(Canvas canvas) {
        PointF point = getPosition();

        if (!isCompleted()) {
            try {
                // background
                for (RightPosition rightPosition : aryPosition) {
                    int id = rightPosition.getFirstID();
                    Piece piece = mapPiece.get(id);

                    if (piece != null) {
                        piece.drawShadow(canvas,
                                rightPosition.getRightX() * fScale,
                                rightPosition.getRightY() * fScale);
                    }
                }

                // pieces
                for (int i = 0; i < aryPiece.size(); i++)
                    aryPiece.get(i).draw(canvas);
            } catch (Exception e) {
                Log.d(TAG, "Drawing pieces failed!");
                return false;
            }

            matchedTime = -1;
        } else if (matchedTime != -1){
            int waitSecond = 3;
            if ((System.currentTimeMillis() - matchedTime)/1000 >= flashCount + waitSecond) {
                GlobalUtils.getActivity().goBack(null);
                return false;
            }
            if ((System.currentTimeMillis()-matchedTime)/1000 < flashCount &&
                (System.currentTimeMillis()-matchedTime)%1000 > 500) {
                paint.setColorFilter(cf);
            } else
                paint.setColorFilter(null);

//            if (paint.getColorFilter() == null)
//                GlobalUtils.drawSVG(canvas, picCompleted, point.x, point.y, fScale, 0);
//            else
                GlobalUtils.drawBitmap(canvas, bmpCompleted, point.x, point.y, 0, paint);
        }

        drawSmallAnimal(canvas);
        return true;
    }

    private void drawSmallAnimal(Canvas canvas) {
        Picture picture = getSVGCompleted();
        int toWidth = (int) GlobalUtils.getPercentalSize(15, false);
        float scale = (float) toWidth / picture.getWidth();
        int toHeight = (int) (picture.getHeight() * scale);

        GlobalUtils.drawSVG(canvas, picture, canvas.getWidth() - toWidth * 3 / 4, toHeight / 2 + toWidth / 4, scale, 0);
    }

    public int pickPiece(int x, int y) {
        int i;
        for (i = aryPiece.size()-1; i >= 0; i--) {
            if (aryPiece.get(i).ptInPiece(x, y))
                break;
        }
        if (i >= 0)
            return i;
        return -1;
    }

    public void selectPiece(int index) {
        if (isSelected) {
            if (index == getSelectedPieceIndex())
                return;
            aryPiece.get(getSelectedPieceIndex()).removeState(Piece.PIECE_STATE_SELECTED);
            isSelected = false;
        }
        if (index == -1)
            return;

        Piece piece = aryPiece.get(index).addState(Piece.PIECE_STATE_SELECTED);
        aryPiece.remove(index);
        aryPiece.add(aryPiece.size(), piece);
        isSelected = true;
    }

    public int getSelectedPieceIndex() {
        if (isSelected)
            return aryPiece.size() - 1;

        return -1;
    }

    public void rotate(int angle) {
        if (isSelected)
            aryPiece.get(getSelectedPieceIndex()).rotate(angle);
    }

    public void move(float x, float y) {
        if (isSelected)
            aryPiece.get(getSelectedPieceIndex()).move(new PointF(x, y));
    }

    public boolean isCompleted() {
        return aryPosition.size() == 0;
    }

    public boolean checkRightPosition() {
        if (isSelected && aryPiece.get(getSelectedPieceIndex()).checkRightPosition()) {
            Piece piece = aryPiece.get(getSelectedPieceIndex());
            aryPiece.remove(getSelectedPieceIndex());
            aryPiece.add(0, piece);
            isSelected = false;

            return true;
        }
        return false;
    }

    public void playWinningFeedback() {
        matchedTime = System.currentTimeMillis();
        flashCount = 6;
    }
}
