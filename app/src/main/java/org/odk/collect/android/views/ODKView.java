/*
 * Copyright (C) 2011 University of Washington
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.exception.ExternalParamsException;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.external.ExternalAppsUtils;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.utilities.TextUtils;
import org.odk.collect.android.utilities.ToastUtils;
import org.odk.collect.android.widgets.IBinaryWidget;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.WidgetFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

/**
 * This class is
 *
 * @author carlhartung
 */
public class ODKView extends FrameLayout implements OnLongClickListener {

    // starter random number for view IDs
    private static final int VIEW_ID = 12345;

    private LinearLayout mView;
    private ArrayList<QuestionWidget> widgets;
    private Handler h = null;
    private boolean isTableView = false;
    private FormEntryCaption[] formEntryCptions;
    public static final String FIELD_LIST = "field-list";

    public ODKView(Context context, final FormEntryPrompt[] questionPrompts,
            FormEntryCaption[] groups, boolean advancingPage, boolean isTableView) {
        super(context);

        this.isTableView = isTableView;
        widgets = new ArrayList<QuestionWidget>();

        mView = new LinearLayout(getContext());
        mView.setOrientation(LinearLayout.VERTICAL);
        mView.setGravity(Gravity.TOP);
        mView.setPadding(0, 0, 0, 0);

        LinearLayout.LayoutParams mLayout =  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        // display which group you are in as well as the question
        formEntryCptions = groups;
        addGroupText(groups, false);

        // when the grouped fields are populated by an external app, this will get true.
        boolean readOnlyOverride = false;

        // get the group we are showing -- it will be the last of the groups in the groups list
        if (groups != null && groups.length > 0) {
            final FormEntryCaption c = groups[groups.length - 1];
            final String intentString = c.getFormElement().getAdditionalAttribute(null, "intent");
            if (intentString != null && intentString.length() != 0) {

                readOnlyOverride = true;

                final String buttonText;
                final String errorString;
                String v = c.getSpecialFormQuestionText("buttonText");
                buttonText = (v != null) ? v : context.getString(R.string.launch_app);
                v = c.getSpecialFormQuestionText("noAppErrorString");
                errorString = (v != null) ? v : context.getString(R.string.no_app);

                TableLayout.LayoutParams params = new TableLayout.LayoutParams();
                params.setMargins(7, 5, 7, 5);

                // set button formatting
                Button launchIntentButton = new Button(getContext());
                launchIntentButton.setId(QuestionWidget.newUniqueId());
                launchIntentButton.setText(buttonText);
                launchIntentButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                        Collect.getQuestionFontsize() + 2);
                launchIntentButton.setPadding(20, 20, 20, 20);
                launchIntentButton.setLayoutParams(params);

                launchIntentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String intentName = ExternalAppsUtils.extractIntentName(intentString);
                        Map<String, String> parameters = ExternalAppsUtils.extractParameters(
                                intentString);

                        Intent i = new Intent(intentName);
                        try {
                            ExternalAppsUtils.populateParameters(i, parameters,
                                    c.getIndex().getReference());

                            for (FormEntryPrompt p : questionPrompts) {
                                IFormElement formElement = p.getFormElement();
                                if (formElement instanceof QuestionDef) {
                                    TreeReference reference =
                                            (TreeReference) formElement.getBind().getReference();
                                    IAnswerData answerValue = p.getAnswerValue();
                                    Object value =
                                            answerValue == null ? null : answerValue.getValue();
                                    switch (p.getDataType()) {
                                        case Constants.DATATYPE_TEXT:
                                        case Constants.DATATYPE_INTEGER:
                                        case Constants.DATATYPE_DECIMAL:
                                            i.putExtra(reference.getNameLast(),
                                                    (Serializable) value);
                                            break;
                                    }
                                }
                            }

                            ((Activity) getContext()).startActivityForResult(i,
                                    FormEntryActivity.EX_GROUP_CAPTURE);
                        } catch (ExternalParamsException e) {
                            Timber.e(e, "ExternalParamsException");

                            ToastUtils.showShortToast(e.getMessage());
                        } catch (ActivityNotFoundException e) {
                            Timber.e(e, "ActivityNotFoundExcept");

                            ToastUtils.showShortToast(errorString);
                        }
                    }
                });

                View divider = new View(getContext());
                divider.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
                divider.setMinimumHeight(3);
                mView.addView(divider);
                mView.addView(launchIntentButton, mLayout);
            }
        }

        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout insideScrollView = new LinearLayout(getContext());
        insideScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        insideScrollView.setOrientation(LinearLayout.VERTICAL);

        int id = 0;
        for (FormEntryPrompt p : questionPrompts) {

            // if question or answer type is not supported, use text widget
            QuestionWidget qw =
                    WidgetFactory.createWidgetFromPrompt(p, getContext(), readOnlyOverride);
            qw.setLongClickable(true);
            qw.setOnLongClickListener(this);
            qw.setId(VIEW_ID + id++);

            widgets.add(qw);

            //before add mView, add cardView first
            LinearLayout holderCard = new LinearLayout(getContext());
            holderCard.setPadding(dp(getContext(),10),dp(getContext(),id==1?10:5),dp(getContext(),10),dp(getContext(),2));

            CardView cardView = new CardView(getContext());
            cardView.setUseCompatPadding(true);

            setPadding(context,qw,10);
            cardView.addView(qw);
            holderCard.addView(cardView);

            insideScrollView.addView(holderCard);
        }

        scrollView.addView(insideScrollView);
        mView.addView(scrollView);
        addView(mView);

        // see if there is an autoplay option. 
        // Only execute it during forward swipes through the form 
        if (advancingPage && widgets.size() == 1) {
            final String playOption = widgets.get(
                    0).getPrompt().getFormElement().getAdditionalAttribute(null, "autoplay");
            if (playOption != null) {
                h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (playOption.equalsIgnoreCase("audio")) {
                            widgets.get(0).playAudio();
                        } else if (playOption.equalsIgnoreCase("video")) {
                            widgets.get(0).playVideo();
                        }
                    }
                }, 150);
            }
        }
    }

    //here is table view
    public ODKView(final Activity context, final ArrayList<FormEntryPrompt[]> questionPromptArray,
                   FormEntryCaption[] groups, boolean advancingPage, boolean isTableView) {
        super(context);

        this.isTableView = isTableView;
        widgets = new ArrayList<QuestionWidget>();

        mView = new LinearLayout(getContext());
        mView.setOrientation(LinearLayout.VERTICAL);
        mView.setGravity(Gravity.TOP);
        mView.setPadding(0, 0, 0, 0);

        HorizontalScrollView hscrollView = new HorizontalScrollView(getContext());
        hscrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,0,1f));

        LinearLayout insideHScroll = new LinearLayout(getContext());
        insideHScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        insideHScroll.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout insideVScroll = new LinearLayout(getContext());
        insideVScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        insideVScroll.setOrientation(LinearLayout.VERTICAL);

        // display which group you are in as well as the question
        formEntryCptions = groups;

        addGroupText(groups, true);

        // when the grouped fields are populated by an external app, this will get true.
        boolean readOnlyOverride = false;

        // get the group we are showing -- it will be the last of the groups in the groups list
        if (groups != null && groups.length > 0) {
            final FormEntryCaption c = groups[groups.length - 1];
            final String intentString = c.getFormElement().getAdditionalAttribute(null, "intent");
            if (intentString != null && intentString.length() != 0) {

                readOnlyOverride = true;

                final String buttonText;
                final String errorString;
                String v = c.getSpecialFormQuestionText("buttonText");
                buttonText = (v != null) ? v : context.getString(R.string.launch_app);
                v = c.getSpecialFormQuestionText("noAppErrorString");
                errorString = (v != null) ? v : context.getString(R.string.no_app);

                TableLayout.LayoutParams params = new TableLayout.LayoutParams();
                params.setMargins(7, 5, 7, 5);

                // set button formatting
                Button launchIntentButton = new Button(getContext());
                launchIntentButton.setId(QuestionWidget.newUniqueId());
                launchIntentButton.setText(buttonText);
                launchIntentButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                        Collect.getQuestionFontsize() + 2);
                launchIntentButton.setPadding(20, 20, 20, 20);
                launchIntentButton.setLayoutParams(params);

                launchIntentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String intentName = ExternalAppsUtils.extractIntentName(intentString);
                        Map<String, String> parameters = ExternalAppsUtils.extractParameters(
                                intentString);

                        Intent i = new Intent(intentName);
                        try {
                            ExternalAppsUtils.populateParameters(i, parameters,
                                    c.getIndex().getReference());

                            for (FormEntryPrompt p : questionPromptArray.get(0)) {
                                IFormElement formElement = p.getFormElement();
                                if (formElement instanceof QuestionDef) {
                                    TreeReference reference =
                                            (TreeReference) formElement.getBind().getReference();
                                    IAnswerData answerValue = p.getAnswerValue();
                                    Object value =
                                            answerValue == null ? null : answerValue.getValue();
                                    switch (p.getDataType()) {
                                        case Constants.DATATYPE_TEXT:
                                        case Constants.DATATYPE_INTEGER:
                                        case Constants.DATATYPE_DECIMAL:
                                            i.putExtra(reference.getNameLast(),
                                                    (Serializable) value);
                                            break;
                                    }
                                }
                            }

                            ((Activity) getContext()).startActivityForResult(i,
                                    FormEntryActivity.EX_GROUP_CAPTURE);
                        } catch (ExternalParamsException e) {
                            Timber.e(e, "ExternalParamsException");

                            ToastUtils.showShortToast(e.getMessage());
                        } catch (ActivityNotFoundException e) {
                            Timber.e(e, "ActivityNotFoundExcept");

                            ToastUtils.showShortToast(errorString);
                        }
                    }
                });

                View divider = new View(getContext());
                divider.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
                divider.setMinimumHeight(3);
                insideHScroll.addView(divider);
                insideHScroll.addView(launchIntentButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }

        ArrayList<String> questionTexts;

        int id = 0;
        //show table view here
        int rowNumber = 0;
        for (final FormEntryPrompt[] questionPrompts:questionPromptArray) {

            LinearLayout mRow = new LinearLayout(context);
            mRow.setOrientation(LinearLayout.HORIZONTAL);
            mRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            //for first time, add action colomn
            ImageView img = new ImageView(getContext());
            img.setImageResource(R.drawable.ic_delete);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(context,38), ViewGroup.LayoutParams.MATCH_PARENT);
            img.setLayoutParams(params);
            img.setColorFilter(ContextCompat.getColor(getContext(), R.color.darker_gray));
            img.setClickable(true);
            img.setPadding(dp(context,8),dp(context,7),dp(context,8),dp(context,7));
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            img.setBackgroundResource(outValue.resourceId);
            img.setBackgroundColor(ContextCompat.getColor(context,R.color.background));

            img.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(questionPrompts.length>0) {
                        Collect.getInstance().getFormController().jumpToIndex(questionPrompts[0].getIndex());
                        ((FormEntryActivity) getContext()).createDeleteRepeatConfirmDialog();
                    }
                }
            });

            LinearLayout holderActionRow = new LinearLayout(getContext());
            holderActionRow.setOrientation(LinearLayout.VERTICAL);
            holderActionRow.setLayoutParams(params);

            TextView numberOfRow = new TextView(getContext());
            numberOfRow.setLayoutParams(new LinearLayout.LayoutParams(dp(getContext(),38),dp(getContext(),38)));
            numberOfRow.setPadding(dp(getContext(),5),dp(getContext(),5),dp(getContext(),5),dp(getContext(),5));
            numberOfRow.setText(""+(rowNumber+1));
            numberOfRow.setTextColor(ContextCompat.getColor(getContext(),R.color.white));
            numberOfRow.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.darker_gray));
            numberOfRow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            numberOfRow.setGravity(Gravity.CENTER);;

            holderActionRow.addView(numberOfRow);
            holderActionRow.addView(img);
            mRow.addView(holderActionRow);

            questionTexts = new ArrayList<>();

            int colNum = 1;
            for (FormEntryPrompt p : questionPrompts) {
                // if question or answer type is not supported, use text widget
                QuestionWidget qw = WidgetFactory.createWidgetFromPrompt(p, getContext(), readOnlyOverride);
                qw.setId(VIEW_ID + id++);
                qw.hideQuestion();
                widgets.add(qw);
                qw.setLayoutParams(new LinearLayout.LayoutParams(dp(context,200), ViewGroup.LayoutParams.WRAP_CONTENT));
                setPadding(context,qw,10);
                mRow.addView(qw);

                if(colNum!=questionPrompts.length){
                    View divider = new View(getContext());
                    divider.setBackgroundColor(ContextCompat.getColor(context,R.color.smooth_gray));
                    divider.setLayoutParams(new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
                    mRow.addView(divider);
                }
                colNum++;
                String promptText = p.getLongText();
                questionTexts.add(promptText == null ? "" : TextUtils.textToHtml(promptText).toString());
            }

            if(rowNumber==0){
                //add question card
                LinearLayout rowQuestion = new LinearLayout(getContext());
                rowQuestion.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                rowQuestion.setPadding(dp(getContext(),0),dp(getContext(),10),dp(getContext(),0),dp(getContext(),10));
                //make a row inside cardview
                LinearLayout holderCardQ = new LinearLayout(getContext());
                holderCardQ.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                holderCardQ.setPadding(dp(getContext(),10),dp(getContext(), 10),dp(getContext(),10),dp(getContext(),1));

                CardView cardViewQ = new CardView(getContext());
                cardViewQ.setUseCompatPadding(true);
                cardViewQ.setCardElevation(2);
                cardViewQ.setCardBackgroundColor(ContextCompat.getColor(context,R.color.colorPrimary));

                TextView margin = new TextView(getContext());
                margin.setLayoutParams(new LinearLayout.LayoutParams(dp(getContext(),32), ViewGroup.LayoutParams.WRAP_CONTENT));
                rowQuestion.addView(margin);
                rowQuestion.setGravity(Gravity.CENTER_VERTICAL);

                for (String q:questionTexts) {
                    TextView qText = new TextView(getContext());
                    qText.setLayoutParams(new LinearLayout.LayoutParams(dp(context,200), ViewGroup.LayoutParams.WRAP_CONTENT));
                    qText.setText(q);
                    qText.setPadding(dp(getContext(),6),dp(getContext(),0),dp(getContext(),6),dp(getContext(),0));
                    qText.setGravity(Gravity.CENTER);

                    qText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    qText.setTextColor(ContextCompat.getColor(getContext(),R.color.white));
//                    qText.setTypeface(null, Typeface.BOLD);
                    rowQuestion.addView(qText);

                    View divider = new View(getContext());
                    divider.setBackgroundColor(ContextCompat.getColor(context,R.color.white));
                    divider.setLayoutParams(new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
                    rowQuestion.addView(divider);
                }

                cardViewQ.addView(rowQuestion);
                holderCardQ.addView(cardViewQ);
                insideHScroll.addView(holderCardQ);
            }

            //make a row inside cardview
            LinearLayout holderCard = new LinearLayout(getContext());
            holderCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            holderCard.setPadding(dp(getContext(),10),dp(getContext(),0),dp(getContext(),10),dp(getContext(),0));

            CardView cardView = new CardView(getContext());
            cardView.setUseCompatPadding(true);
            cardView.setCardElevation(2);
            cardView.addView(mRow);
//            if(rowNumber%2==0) cardView.setCardBackgroundColor(ContextCompat.getColor(context,R.color.white));
//            else cardView.setCardBackgroundColor(ContextCompat.getColor(context,R.color.table_strip));

            holderCard.addView(cardView);
            insideVScroll.addView(holderCard);
            rowNumber++;
        }

        //add an plus button
        LinearLayout buttonPlusLayout = new LinearLayout(getContext());
        buttonPlusLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonPlusLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonPlusLayout.setPadding(0,16,0,16);
        ((LinearLayout.LayoutParams)buttonPlusLayout.getLayoutParams()).setMargins(dp(context,16),dp(context,6),dp(context,16),dp(context,1));
        buttonPlusLayout.setGravity(Gravity.CENTER);

        ImageView imgPlus = new ImageView(getContext());
        imgPlus.setImageResource(R.drawable.ic_add);
        imgPlus.setColorFilter(ContextCompat.getColor(getContext(),R.color.white));
        imgPlus.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView textView = new TextView(getContext());
        textView.setText("Add repeat");
        textView.setTextColor(ContextCompat.getColor(getContext(),R.color.white));
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        buttonPlusLayout.setBackgroundResource(R.drawable.cl_button_round_primary);
        buttonPlusLayout.addView(imgPlus);
        buttonPlusLayout.addView(textView);
        buttonPlusLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FormEntryActivity)context).createRepeatDialog();
            }
        });

//        insideVScroll.addView(buttonPlusLayout);
        scrollView.addView(insideVScroll);
        insideHScroll.addView(scrollView);
        hscrollView.addView(insideHScroll);
        mView.addView(hscrollView);
        mView.addView(buttonPlusLayout);
        addView(mView);

        // see if there is an autoplay option.
        // Only execute it during forward swipes through the form
        if (advancingPage && widgets.size() == 1) {
            final String playOption = widgets.get(
                    0).getPrompt().getFormElement().getAdditionalAttribute(null, "autoplay");
            if (playOption != null) {
                h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (playOption.equalsIgnoreCase("audio")) {
                            widgets.get(0).playAudio();
                        } else if (playOption.equalsIgnoreCase("video")) {
                            widgets.get(0).playVideo();
                        }
                    }
                }, 150);
            }
        }
    }

    /**
     * http://code.google.com/p/android/issues/detail?id=8488
     */
    public void recycleDrawables() {
        this.destroyDrawingCache();
        mView.destroyDrawingCache();
        for (QuestionWidget q : widgets) {
            q.recycleDrawables();
        }
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        Collect.getInstance().getActivityLogger().logScrollAction(this, t - oldt);
    }

    /**
     * @return a HashMap of answers entered by the user for this set of widgets
     */
    public LinkedHashMap<FormIndex, IAnswerData> getAnswers() {
        LinkedHashMap<FormIndex, IAnswerData> answers = new LinkedHashMap<FormIndex, IAnswerData>();
        Iterator<QuestionWidget> i = widgets.iterator();
        while (i.hasNext()) {
            /*
             * The FormEntryPrompt has the FormIndex, which is where the answer gets stored. The
             * QuestionWidget has the answer the user has entered.
             */
            QuestionWidget q = i.next();
            FormEntryPrompt p = q.getPrompt();
            answers.put(p.getIndex(), q.getAnswer());
        }

        return answers;
    }


    /**
     * // * Add a TextView containing the hierarchy of groups to which the question belongs. //
     */
    private void addGroupText(FormEntryCaption[] groups, boolean isTableView) {
        StringBuilder s = new StringBuilder("");
        String t = "";
        int i;
        // list all groups in one string
        for (FormEntryCaption g : groups) {
            i = g.getMultiplicity() + 1;
            t = g.getLongText();
            if (t != null) {
                s.append(t);
                if (g.repeats() && i > 0) {
                    if(!isTableView) s.append(" (" + i + ")");
                }
                s.append(" > ");
            }
        }

        //we must modify group label text, if table view is showing

        // build view
        if (s.length() > 0) {
            TextView tv = new TextView(getContext());
            tv.setText(s.substring(0, s.length() - 3));
            tv.setTextColor(ContextCompat.getColor(getContext(),R.color.white));
            tv.setGravity(Gravity.CENTER);
            int questionFontsize = Collect.getQuestionFontsize();
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, questionFontsize - 4);
            tv.setPadding(dp(getContext(),16), dp(getContext(),8), dp(getContext(),16), dp(getContext(),8));
            tv.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.primary));
            mView.addView(tv);
        }
    }


    public void setFocus(Context context) {
        if (widgets.size() > 0) {
            widgets.get(0).setFocus(context);
        }
    }


    /**
     * Called when another activity returns information to answer this question.
     */
    public void setBinaryData(Object answer) {
        boolean set = false;
        for (QuestionWidget q : widgets) {
            if (q instanceof IBinaryWidget) {
                if (((IBinaryWidget) q).isWaitingForBinaryData()) {
                    try {
                        ((IBinaryWidget) q).setBinaryData(answer);
                    } catch (Exception e) {
                        Timber.e(e);
                        ToastUtils.showLongToast(getContext().getString(R.string.error_attaching_binary_file,
                                        e.getMessage()));
                    }
                    set = true;
                    break;
                }
            }
        }

        if (!set) {
            Timber.w("Attempting to return data to a widget or set of widgets not looking for data");
        }
    }

    public void setDataForFields(Bundle bundle) throws JavaRosaException {
        if (bundle == null) {
            return;
        }
        FormController formController = Collect.getInstance().getFormController();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            for (QuestionWidget questionWidget : widgets) {
                FormEntryPrompt prompt = questionWidget.getPrompt();
                TreeReference treeReference =
                        (TreeReference) prompt.getFormElement().getBind().getReference();
                if (treeReference.getNameLast().equals(key)) {

                    switch (prompt.getDataType()) {
                        case Constants.DATATYPE_TEXT:
                            formController.saveAnswer(prompt.getIndex(),
                                    ExternalAppsUtils.asStringData(bundle.get(key)));
                            break;
                        case Constants.DATATYPE_INTEGER:
                            formController.saveAnswer(prompt.getIndex(),
                                    ExternalAppsUtils.asIntegerData(bundle.get(key)));
                            break;
                        case Constants.DATATYPE_DECIMAL:
                            formController.saveAnswer(prompt.getIndex(),
                                    ExternalAppsUtils.asDecimalData(bundle.get(key)));
                            break;
                        default:
                            throw new RuntimeException(
                                    getContext().getString(R.string.ext_assign_value_error,
                                            treeReference.toString(false)));
                    }

                    break;
                }
            }
        }
    }

    public void cancelWaitingForBinaryData() {
        int count = 0;
        for (QuestionWidget q : widgets) {
            if (q instanceof IBinaryWidget) {
                if (((IBinaryWidget) q).isWaitingForBinaryData()) {
                    ((IBinaryWidget) q).cancelWaitingForBinaryData();
                    ++count;
                }
            }
        }

        if (count != 1) {
            Timber.w("Attempting to cancel waiting for binary data to a widget or set of widgets "
                            + "not looking for data");
        }
    }

    public boolean suppressFlingGesture(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        for (QuestionWidget q : widgets) {
            if (q.suppressFlingGesture(e1, e2, velocityX, velocityY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the answer was cleared, false otherwise.
     */
    public boolean clearAnswer() {
        // If there's only one widget, clear the answer.
        // If there are more, then force a long-press to clear the answer.
        if (widgets.size() == 1 && !widgets.get(0).getPrompt().isReadOnly()) {
            widgets.get(0).clearAnswer();
            return true;
        } else {
            return false;
        }
    }


    public ArrayList<QuestionWidget> getWidgets() {
        return widgets;
    }


    @Override
    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        for (int i = 0; i < widgets.size(); i++) {
            QuestionWidget qw = widgets.get(i);
            qw.setOnFocusChangeListener(l);
        }
    }


    @Override
    public boolean onLongClick(View v) {
        return false;
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        for (QuestionWidget qw : widgets) {
            qw.cancelLongPress();
        }
    }

    public void stopAudio() {
        widgets.get(0).stopAudio();
    }

    public boolean isTableView() {
        return isTableView;
    }

    public FormEntryCaption[] getFormEntryCptions() {
        return formEntryCptions;
    }

    private static void setPadding(Context c, View v, int dp){
        v.setPadding(dp(c,dp),dp(c,dp),dp(c,dp),dp(c,dp+5));
    }

    private static int dp(Context context, int dip) {
        return (int) (dip * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
