package com.olimex.tangram;

import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.olimex.tangram.concept.SceneView;
import com.olimex.tangram.engine.Animal;

import java.io.IOException;

@SuppressWarnings("ALL")
public class GameActivity extends AppCompatActivity {
    public static final String MENU = "Menu";


    public static Animal animal = null;
    public static boolean isMenuScene = true;

    private SceneView sceneView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set global constants
        GlobalUtils.setLastAction();
        GlobalUtils.setActivity(this);

        super.onCreate(savedInstanceState);

        // full screen
        GlobalUtils.makeFullScreen(this);

        setContentView(R.layout.activity_one);

        sceneView = (SceneView) findViewById(R.id.scene);
        sceneView.setOnTouchListener(sceneView);

        TextView textView = (TextView) findViewById(R.id.title);
        Typeface tf = Typeface.createFromAsset(getAssets(),"font/prismfont.ttf");
        textView.setTypeface(tf, Typeface.NORMAL);

        findViewById(R.id.game_home).setOnTouchListener(GlobalUtils.onTouchListener);
        findViewById(R.id.game_back).setOnTouchListener(GlobalUtils.onTouchListener);

        gotoGame(MENU);
    }

    public void goHome(View view) {
        if (animal != null)
            animal.freeMemory();

        finish();
    }

    public void goBack(View view) {
        if (isMenuScene) {
            goHome(null);
            return;
        }

        gotoGame(MENU);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goBack(null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void gotoGame(String animalName) {
        isMenuScene = animalName.equals(MENU);

        findViewById(R.id.game_back).setVisibility(isMenuScene ? View.GONE : View.VISIBLE);
        findViewById(R.id.title).setVisibility(isMenuScene ? View.VISIBLE : View.GONE);

        if (animal != null) {
            animal.freeMemory();
            animal = null;
        }
        if (!isMenuScene)
            animal = Animal.generateAnimal(animalName);

        sceneView.postInvalidate();

        GlobalUtils.setLastAction();
    }

    MediaPlayer mp = null;

    public void playSound() {
        if (mp != null) {
            mp.reset();
            mp.release();
        }

        mp = GlobalUtils.getMediaPlayer(this);
        try {
            int soundID = getResources().getIdentifier(animal.getAnimalName(), "raw", getPackageName());
            AssetFileDescriptor afd = getResources().openRawResourceFd(soundID);
            if (afd == null) return;
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setLooping(false);
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
