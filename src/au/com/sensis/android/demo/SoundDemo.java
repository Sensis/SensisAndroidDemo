package au.com.sensis.android.demo;

import java.io.IOException;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SoundDemo extends ListActivity {
    SoundPool soundPool;
    int yes = 0;
    int no = 1;
    int applause = 2;
    int cheering = 3;
    int burp = 4;
    int currentStream = 0;
    String[] files  = {"yes.ogg", "no.ogg", "applause.ogg", "cheering.ogg", "burp.ogg"};
    int[] sounds = new int[5];
    String tasks[] = { "Short Sound", "Long Sound", "Repeat 3", "Interrupt & Resume", "Interrupt & Kill" };
    Handler handler = new Handler();
    

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        
        try {
            AssetManager assetManager = getAssets();
            for (int i = 0; i < files.length; i++){
                AssetFileDescriptor descriptor = assetManager.openFd(files[i]);
                sounds[i] = soundPool.load(descriptor, 1);
            }
            
        } catch (IOException e) {
            
        }
        
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tasks));
        
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);
        String taskName = tasks[position];
        if (taskName.equals("Short Sound")){
            currentStream = soundPool.play(sounds[yes], 1, 1, 0, 0, 1);
        }
        else if (taskName.equals("Long Sound")){
            currentStream = soundPool.play(sounds[cheering], 1, 1, 0, 0, 1);
        }
        else if (taskName.equals("Repeat 3")){
            currentStream = soundPool.play(sounds[no], 1, 1, 0, 2, 1);
        }
        else if (taskName.equals("Interrupt & Resume")){
            soundPool.pause(currentStream);
            soundPool.play(sounds[burp], 1, 1, 0, 0, 1);
            
            handler.postDelayed(new Runnable() {
                
                @Override
                public void run() {
                    soundPool.resume(currentStream);
                }
            }, 1000);
            
        }
        else if (taskName.equals("Interrupt & Kill")){
            soundPool.stop(currentStream);
            currentStream = soundPool.play(sounds[burp], 1, 1, 0, 0, 1);
        }
    }
}
