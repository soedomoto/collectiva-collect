package org.odk.collect.android.collectiva;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.odk.collect.android.R;

import java.util.ArrayList;

/**
 * Created by lenovo on 5/5/2017.
 */

public class CollectivaAdapterProject extends RecyclerView.Adapter<CollectivaAdapterProject.ViewHolder> {
    private Activity activity;
    private ArrayList<ModelProjectSurvey> mDataSet;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.collectiva_item_form, parent, false);
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    public CollectivaAdapterProject(Activity activity){
        this.activity = activity;
    }

    public void setData(ArrayList<ModelProjectSurvey> listData){
        this.mDataSet = listData;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
