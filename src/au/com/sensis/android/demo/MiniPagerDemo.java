package au.com.sensis.android.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MiniPagerDemo extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mini_pager_demo);
        
        final ViewPager pager = (ViewPager)findViewById(R.id.content);
        
        List<String> someData = new ArrayList<String>();
        for (int i = 1; i <= 10000; i++){
            someData.add("Result #" + i);
        }
        
        
        pager.setAdapter(new MyPagerAdapter(this.getSupportFragmentManager(), someData));
        pager.setVisibility(View.INVISIBLE);
        
        
        ((Button)findViewById(R.id.show)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setVisibility(View.VISIBLE);
            }
        });
        
        ((Button)findViewById(R.id.hide)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setVisibility(View.INVISIBLE);
            }
        });
        
        ((Button)findViewById(R.id.new_data)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> someData = new ArrayList<String>();
                for (int i = 1; i <= 100; i++){
                    someData.add("Result #" + i);
                }
                
                pager.setAdapter(new MyPagerAdapter(MiniPagerDemo.this.getSupportFragmentManager(), someData));
            }
        });
        
    }
    
    
    public class MyPagerAdapter extends FragmentStatePagerAdapter {
        
        private List<String> data;
        
        private MyPagerAdapter(FragmentManager fm, String[] data) {
            super(fm);
            this.data = Arrays.asList(data);
        }
        
        private MyPagerAdapter(FragmentManager fm, List<String> data) {
            super(fm);
            this.data = data;
        }

        @Override
        public Fragment getItem(int index) {
            Fragment f = new MyFragment();
            Bundle args = new Bundle();
            args.putString(MyFragment.ARG, data.get(index));
            f.setArguments(args);
            return f;
        }

        @Override
        public int getCount() {
            return data.size();
        }
        
    }
    
    

}
