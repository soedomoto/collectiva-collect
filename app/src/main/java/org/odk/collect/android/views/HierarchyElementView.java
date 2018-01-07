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

package org.odk.collect.android.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.javarosa.form.api.FormEntryController;
import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.logic.HierarchyElement;
import org.odk.collect.android.utilities.TextUtils;
import org.odk.collect.android.widgets.QuestionWidget;

public class HierarchyElementView extends LinearLayout {

    private TextView mPrimaryTextView, mPrimaryTextGroup;
    private TextView mSecondaryTextView;
    private CardView holderGroup;
    private View mark, status;
    private View divider;
    private LinearLayout childHolder;
    private ImageView mIcon;


    private static final int CHILD = 1;
    private static final int EXPANDED = 2;
    private static final int COLLAPSED = 3;
    private static final int QUESTION = 4;

    public HierarchyElementView(Context context, HierarchyElement ita) {
        super(context);

        View v = LayoutInflater.from(context).inflate(R.layout.collectiva_item_hierarchyelement,null);
        mPrimaryTextGroup = (TextView) v.findViewById(R.id.primary_text_group);
        mPrimaryTextView = (TextView) v.findViewById(R.id.primary_text);
        mSecondaryTextView = (TextView) v.findViewById(R.id.secondary_text);
        holderGroup = (CardView) v.findViewById(R.id.group_holder);
        childHolder = (LinearLayout) v.findViewById(R.id.child_holder);
        mark = v.findViewById(R.id.mark);
        status = v.findViewById(R.id.status);
        divider = v.findViewById(R.id.divider);
        mIcon = (ImageView) v.findViewById(R.id.icon);

        boolean isParent = false;
        boolean isRepeat = false;
        if(ita.getType()==COLLAPSED || ita.getType()==EXPANDED){
            isParent = true;
        }
        if(Collect.getInstance().getFormController().getEvent(ita.getFormIndex())== FormEntryController.EVENT_REPEAT){
            isRepeat = true;
        }

        if(isParent){
            childHolder.setVisibility(GONE);
            holderGroup.setVisibility(VISIBLE);

            mIcon.setImageDrawable(ita.getIcon());
            mPrimaryTextGroup.setText(ita.getPrimaryText());
            divider.setVisibility(GONE);
            if(isRepeat) mark.setVisibility(VISIBLE);
            else mark.setVisibility(GONE);
        }else {
            childHolder.setVisibility(VISIBLE);
            holderGroup.setVisibility(GONE);

            childHolder.setPadding(dipToPx(15*(ita.getIndentLevel())), 0, 0, 0);

            mPrimaryTextView.setText(ita.getPrimaryText());
            mSecondaryTextView.setText(ita.getSecondaryText());
            status.setBackgroundColor(ita.getColor());
        }

        addView(v,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

//    public HierarchyElementView(Context context, HierarchyElement it) {
//        super(context);
//
//        setColor(it.getColor());
//
//        mIcon = new ImageView(context);
//        mIcon.setImageDrawable(it.getIcon());
//        mIcon.setId(QuestionWidget.newUniqueId());
//        mIcon.setPadding(0, 0, dipToPx(4), 0);
//
//        addView(mIcon, new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//                LayoutParams.WRAP_CONTENT));
//
//        mPrimaryTextView = new TextView(context);
//        mPrimaryTextView.setTextAppearance(context, android.R.style.TextAppearance_Large);
//        setPrimaryText(it.getPrimaryText());
//        mPrimaryTextView.setId(QuestionWidget.newUniqueId());
//        mPrimaryTextView.setGravity(Gravity.CENTER_VERTICAL);
//        LayoutParams l =
//                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//                        LayoutParams.WRAP_CONTENT);
//        l.addRule(RelativeLayout.RIGHT_OF, mIcon.getId());
//        addView(mPrimaryTextView, l);
//
//        mSecondaryTextView = new TextView(context);
//        mSecondaryTextView.setText(it.getSecondaryText());
//        mSecondaryTextView.setTextAppearance(context, android.R.style.TextAppearance_Small);
//        mSecondaryTextView.setGravity(Gravity.CENTER_VERTICAL);
//
//        LayoutParams lp =
//                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//                        LayoutParams.WRAP_CONTENT);
//        lp.addRule(RelativeLayout.BELOW, mPrimaryTextView.getId());
//        lp.addRule(RelativeLayout.RIGHT_OF, mIcon.getId());
//        addView(mSecondaryTextView, lp);
//
//        setPadding(dipToPx(8), dipToPx(4), dipToPx(8), dipToPx(8));
//
//    }
//
//
//    public void setPrimaryText(String text) {
//        mPrimaryTextView.setText(TextUtils.textToHtml(text));
//    }
//
//
//    public void setSecondaryText(String text) {
//        mSecondaryTextView.setText(TextUtils.textToHtml(text));
//    }
//
//
//    public void setIcon(Drawable icon) {
//        mIcon.setImageDrawable(icon);
//    }
//
//
//    public void setColor(int color) {
//        setBackgroundColor(color);
//    }
//
//
//    public void showSecondary(boolean bool) {
//        if (bool) {
//            mSecondaryTextView.setVisibility(VISIBLE);
//            setMinimumHeight(dipToPx(64));
//
//        } else {
//            mSecondaryTextView.setVisibility(GONE);
//            setMinimumHeight(dipToPx(32));
//
//        }
//    }
//
    public int dipToPx(int dip) {
        return (int) (dip * getResources().getDisplayMetrics().density + 0.5f);
    }

}
