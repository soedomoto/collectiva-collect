/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.HierarchyElement;
import org.odk.collect.android.views.HierarchyElementView;

import java.util.ArrayList;
import java.util.List;

public class HierarchyListAdapter extends BaseAdapter {

    private Context mContext;
    private boolean isForErrorOnly = false;
    private List<HierarchyElement> mItems = new ArrayList<HierarchyElement>();

    public HierarchyListAdapter(Activity context, boolean isForErrorOnly) {
        mContext = context;
        this.isForErrorOnly = isForErrorOnly;
    }


    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if(isForErrorOnly){
            View v = LayoutInflater.from(mContext).inflate(R.layout.collectiva_item_error, null);
            TextView primary = (TextView) v.findViewById(R.id.primary_text);
            TextView secondary = (TextView) v.findViewById(R.id.secondary_text);
            LinearLayout errorH = (LinearLayout) v.findViewById(R.id.errorHolder);

            primary.setText(mItems.get(position).getPrimaryText());
            secondary.setText(mItems.get(position).getSecondaryText());
            errorH.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FormController fc = Collect.getInstance().getFormController();
                    FormIndex fi =mItems.get(position).getFormIndex();
                    if(fi!=null){
                        fc.jumpToIndex(getIndexAdaptionFromSetting(fi));
                        ((FormEntryActivity) mContext).refreshCurrentView();
                    }
                }
            });

            return v;

        }else {
//            HierarchyElementView hev;
//            if (convertView == null) {
//                hev = new HierarchyElementView(mContext, mItems.get(position));
//            } else {
//                hev = (HierarchyElementView) convertView;
//            }
//
//            return hev;

            return new HierarchyElementView(mContext, mItems.get(position));
        }
    }


    /**
     * Sets the list of items for this adapter to use.
     */
    public void setListItems(List<HierarchyElement> it) {
        mItems = it;
        notifyDataSetChanged();
    }

    public List<HierarchyElement> getItems() {
        return mItems;
    }

    public int dipToPx(int dip) {
        return (int) (dip * mContext.getResources().getDisplayMetrics().density + 0.5f);
    }

    public FormIndex getIndexAdaptionFromSetting(FormIndex index){
        FormController formController = Collect.getInstance().getFormController();
        if(formController.indexIsInFieldList(index)){
            //search group index
            FormEntryCaption[] captions = formController.getCaptionHierarchy(index);
            if (captions.length < 2) {
                // question has no group
                return index;
            }
            FormEntryCaption grp = captions[captions.length - 2];
            return grp.getIndex();
        }
        return index;
    }
}
