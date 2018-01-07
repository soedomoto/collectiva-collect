package org.odk.collect.android.collectiva;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.odk.collect.android.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class CollectivaFragmentListProject extends Fragment {

    private RecyclerView recyclerView;
    private RelativeLayout progressBar;
    private TextView messageError;
    private CollectivaAdapterProject mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.collectiva_fragment_list_project, container, false);
        recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view);
        progressBar = (RelativeLayout) v.findViewById(R.id.holder_progress_bar);
        messageError = (TextView) v.findViewById(R.id.message_error);

        mAdapter = new CollectivaAdapterProject(getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        upDateUI();

        return v;
    }

    private void upDateUI() {


    }

}
