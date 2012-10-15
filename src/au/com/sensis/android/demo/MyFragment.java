package au.com.sensis.android.demo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MyFragment extends Fragment {

    public static final String ARG = "arg";

    public MyFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        String text = getArguments().getString(ARG);

        TextView tv = new TextView(getActivity());

        tv.setText(text);
        tv.setTextSize(TypedValue.DENSITY_DEFAULT, 40);
        tv.setGravity(Gravity.CENTER);

        return tv;
    }

}
