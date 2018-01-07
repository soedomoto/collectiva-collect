package org.odk.collect.android.collectiva;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.odk.collect.android.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class CollectivaFragmentNotifications extends Fragment {


    public CollectivaFragmentNotifications() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.collectiva_fragment_notifications, container, false);
    }

}
