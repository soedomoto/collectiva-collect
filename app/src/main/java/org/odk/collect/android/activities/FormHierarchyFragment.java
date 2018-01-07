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

package org.odk.collect.android.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.adapters.HierarchyListAdapter;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.collectiva.CollectivaPreferences;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.HierarchyElement;
import org.odk.collect.android.utilities.FormEntryPromptUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class FormHierarchyFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final int CHILD = 1;
    private static final int EXPANDED = 2;
    private static final int COLLAPSED = 3;
    private static final int QUESTION = 4;

    private ImageView goUpArrow;

    List<HierarchyElement> formList;
    TextView mPath, jumpToEnd;

    FormIndex mStartIndex;
    private FormIndex currentIndex;
    private EditText searchBox;
    private ListView listView;
    private HierarchyListAdapter mAdapter;
    private static String searched = "";
    private ImageView closeSearch;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.collectiva_fragment_hierarchy_layout, container, false);
        FormController formController = Collect.getInstance().getFormController();
        // We use a static FormEntryController to make jumping faster.
        mStartIndex = formController.getFormIndex();

        mPath = (TextView) v.findViewById(R.id.pathtext);
        goUpArrow = (ImageView) v.findViewById(R.id.back_arrow);
        listView = (ListView) v.findViewById(android.R.id.list);
        searchBox = (EditText) v.findViewById(R.id.search_box);
        closeSearch = (ImageView) v.findViewById(R.id.search_close_btn);
        jumpToEnd = (TextView) v.findViewById(R.id.jump_to_end);

        listView.setDivider(null);
        listView.setDividerHeight(0);

        mAdapter = new HierarchyListAdapter(getActivity(), false);
        goUpArrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger().logInstanceAction(this, "goUpLevelButton",
                        "click");
                goUpLevel();
            }
        });
        refreshView();

        final int delay = 1000;
        final long[] lastEditText = {0};
        final Handler handler = new Handler();

        final Runnable inputFinish = new Runnable() {
            @Override
            public void run() {
                if(System.currentTimeMillis()> lastEditText[0] + delay - 500){
                    filterFormList(searched);
                }
            }
        };

        searchBox.setHint("Search question..");
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(inputFinish);
            }

            @Override
            public void afterTextChanged(Editable s) {
                searched = s.toString();
                if(s.length()>0){
                    lastEditText[0] = System.currentTimeMillis();
                    handler.postDelayed(inputFinish, delay);
                }else filterFormList("");
            }
        });
        searchBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                filterFormList(searched);
            }
        });

        closeSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(searchBox.getText().toString().equals("")) closePanel();
                else {
                    searchBox.setText("");
                    filterFormList("");
                }
            }
        });
        jumpToEnd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getFormController().jumpToIndex(FormIndex.createEndOfFormIndex());

                ((FormEntryActivity) getActivity()).refreshCurrentView();
                refreshView();
            }
        });

        return v;
    }

    private void closePanel() {
        ((FormEntryActivity) getActivity()).getSlidingMenu().showContent();
    }

    private void filterFormList(String s) {
        if(!s.equals("")){
            ArrayList<HierarchyElement> newList = new ArrayList<>();
            List<HierarchyElement> allList = addAllHierarchyElement(new ArrayList<HierarchyElement>(), formList);
            for (HierarchyElement h: allList) {
                if(h.getPrimaryText()!=null) {
                    if(h.getSecondaryText()==null) h.setSecondaryText("");
                    if (h.getPrimaryText().toLowerCase().contains(s.toLowerCase()) || h.getSecondaryText().toLowerCase().contains(s.toLowerCase())) {
                        newList.add(h);
                    }
                }
            }
            log(newList);
            mAdapter.setListItems(newList);
        }else {
            mAdapter.setListItems(formList);
        }
    }

    private void log(ArrayList<HierarchyElement> newList) {
        String s = "";
        for (HierarchyElement h:newList) {
            s += h.getPrimaryText()+",";
        }
        Log.d("DEBUGCOLL","searched: "+s);
    }

    private List<HierarchyElement> addAllHierarchyElement(List<HierarchyElement> newlist, List<HierarchyElement> formList) {
        for (HierarchyElement h:formList) {
            if(h.getType()==COLLAPSED || h.getType()==EXPANDED){
                addAllHierarchyElement(newlist, h.getChildren());
            }else newlist.add(h);
        }
        return newlist;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void goUpLevel() {
        Collect.getInstance().getFormController().stepToOuterScreenEvent();

        refreshView();
    }

    public void refreshView() {
        try {
            FormController formController = Collect.getInstance().getFormController();
            // Record the current index so we can return to the same place if the user hits 'back'.
            currentIndex = formController.getFormIndex();

            // If we're not at the first level, we're inside a repeated group so we want to only
            // display
            // everything enclosed within that group.
            String contextGroupRef = "";
            formList = new ArrayList<HierarchyElement>();

            // If we're currently at a repeat node, record the name of the node and step to the next
            // node to display.
            if (formController.getEvent() == FormEntryController.EVENT_REPEAT) {
                contextGroupRef =
                        formController.getFormIndex().getReference().toString(true);
                formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
            } else {
                FormIndex startTest = formController.stepIndexOut(currentIndex);
                // If we have a 'group' tag, we want to step back until we hit a repeat or the
                // beginning.
                while (startTest != null
                        && formController.getEvent(startTest) == FormEntryController.EVENT_GROUP) {
                    startTest = formController.stepIndexOut(startTest);
                }
                if (startTest == null) {
                    // check to see if the question is at the first level of the hierarchy. If it
                    // is,
                    // display the root level from the beginning.
                    formController.jumpToIndex(FormIndex
                            .createBeginningOfFormIndex());
                } else {
                    // otherwise we're at a repeated group
                    formController.jumpToIndex(startTest);
                }

                // now test again for repeat. This should be true at this point or we're at the
                // beginning
                if (formController.getEvent() == FormEntryController.EVENT_REPEAT) {
                    contextGroupRef =
                            formController.getFormIndex().getReference().toString(true);
                    formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
                }
            }

            int event = formController.getEvent();
            if (event == FormEntryController.EVENT_BEGINNING_OF_FORM) {
                // The beginning of form has no valid prompt to display.
                formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
                contextGroupRef =
                        formController.getFormIndex().getReference().getParentRef().toString(true);
                mPath.setVisibility(View.GONE);
                goUpArrow.setVisibility(View.GONE);
            } else {
                mPath.setVisibility(View.VISIBLE);
                mPath.setText(getCurrentPath());
                goUpArrow.setVisibility(View.VISIBLE);
            }

            // Refresh the current event in case we did step forward.
            event = formController.getEvent();

            // Big change from prior implementation:
            //
            // The ref strings now include the instance number designations
            // i.e., [0], [1], etc. of the repeat groups (and also [1] for
            // non-repeat elements).
            //
            // The contextGroupRef is now also valid for the top-level form.
            //
            // The repeatGroupRef is null if we are not skipping a repeat
            // section.
            //
            String repeatGroupRef = null;
            boolean isFoundGroup = false;

            event_search:
            while (event != FormEntryController.EVENT_END_OF_FORM) {

                // get the ref to this element
                String currentRef = formController.getFormIndex().getReference().toString(true);

                // retrieve the current group
                String curGroup = (repeatGroupRef == null) ? contextGroupRef : repeatGroupRef;

                if (!currentRef.startsWith(curGroup)) {
                    // We have left the current group
                    if (repeatGroupRef == null) {
                        // We are done.
                        break event_search;
                    } else {
                        // exit the inner repeat group
                        repeatGroupRef = null;
                    }
                    //reset founded group
                    isFoundGroup = false;
                }

                if (repeatGroupRef != null) {
                    // We're in a repeat group within the one we want to list
                    // skip this question/group/repeat and move to the next index.
                    if(!isFoundGroup) {
                        event = formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
                        continue;
                    }
                }

                switch (event) {
                    case FormEntryController.EVENT_QUESTION:
                        FormEntryPrompt fp = formController.getQuestionPrompt();
                        String label = fp.getLongText();
                        if (!fp.isReadOnly() || (label != null && label.length() > 0)) {
                            // show the question if it is an editable field.
                            // or if it is read-only and the label is not blank.
                            String answerDisplay = FormEntryPromptUtils.getAnswerText(fp);
                            boolean isErrorFound = formController.getxPathErrors().contains("question." +currentRef);

                            if(isFoundGroup) {
                                // Add this question to group collapsed
                                HierarchyElement h = formList.get(formList.size() - 1);
                                h.addChild(new HierarchyElement(fp.getLongText(), answerDisplay, null,
                                        isErrorFound?ContextCompat.getColor(getContext(), R.color.red):Color.WHITE,
                                        QUESTION, fp.getIndex(), 1));
                            }else formList.add(
                                    new HierarchyElement(fp.getLongText(), answerDisplay, null,
                                            isErrorFound?ContextCompat.getColor(getContext(), R.color.red):Color.WHITE,
                                            QUESTION, fp.getIndex()));
                        }
                        break;
                    case FormEntryController.EVENT_GROUP:
                        // make group event like collapse expand
                        isFoundGroup = true;
                        FormEntryCaption formC = formController.getCaptionPrompt();
                        repeatGroupRef = currentRef;
                        HierarchyElement groupQuestion =
                                new HierarchyElement(formC.getLongText(), null, ContextCompat
                                        .getDrawable(getContext().getApplicationContext(), R.drawable.expander_ic_minimized),
                                        ContextCompat.getColor(getContext(), R.color.white),
                                        COLLAPSED, formC.getIndex());
                        formList.add(groupQuestion);
                        break;
                    case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                        // this would display the 'add new repeat' dialog
                        // ignore it.
                        break;
                    case FormEntryController.EVENT_REPEAT:
                        FormEntryCaption fc = formController.getCaptionPrompt();
                        // push this repeat onto the stack.
                        repeatGroupRef = currentRef;
                        // Because of the guard conditions above, we will skip
                        // everything until we exit this repeat.
                        //
                        // Note that currentRef includes the multiplicity of the
                        // repeat (e.g., [0], [1], ...), so every repeat will be
                        // detected as different and reach this case statement.
                        // Only the [0] emits the repeat header.
                        // Every one displays the descend-into action element.

                        if (fc.getMultiplicity() == 0) {
                            // Display the repeat header for the group.
                            HierarchyElement group =
                                    new HierarchyElement(fc.getLongText(), null, ContextCompat
                                            .getDrawable(getContext().getApplicationContext(), R.drawable.expander_ic_minimized),
                                            ContextCompat.getColor(getContext(),R.color.roster_color),
                                            COLLAPSED, fc.getIndex());
                            formList.add(group);
                        }
                        // Add this group name to the drop down list for this repeating group.
                        HierarchyElement h = formList.get(formList.size() - 1);
                        h.addChild(new HierarchyElement("rensponse "
                                + (fc.getMultiplicity() + 1), null, null, Color.WHITE, CHILD, fc
                                .getIndex(),1));
                        break;
                }
                event = formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
            }

            mAdapter.setListItems(formList);
            listView.setAdapter(mAdapter);
            listView.setOnItemClickListener(this);

            // set the controller back to the current index in case the user hits 'back'
            formController.jumpToIndex(currentIndex);
        } catch (Exception e) {
            Timber.e(e);
            createErrorDialog(e.getMessage());
        }
    }

    private String getCurrentPath() {
        FormController formController = Collect.getInstance().getFormController();
        FormIndex index = formController.getFormIndex();
        // move to enclosing group...
        index = formController.stepIndexOut(index);

        String path = "";
        while (index != null) {

            path =
                    formController.getCaptionPrompt(index).getLongText()
                            + " ("
                            + (formController.getCaptionPrompt(index)
                            .getMultiplicity() + 1) + ") > " + path;

            index = formController.stepIndexOut(index);
        }
        // return path?
        return path.substring(0, path.length() - 2);
    }


    /**
     * Creates and displays dialog with the given errorMsg.
     */
    private void createErrorDialog(String errorMsg) {
        Collect.getInstance()
                .getActivityLogger()
                .logInstanceAction(this, "createErrorDialog", "show.");

        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();

        alertDialog.setIcon(android.R.drawable.ic_dialog_info);
        alertDialog.setTitle(getString(R.string.error_occured));
        alertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Collect.getInstance().getActivityLogger()
                                .logInstanceAction(this, "createErrorDialog", "OK");
                        FormController formController = Collect.getInstance().getFormController();
                        formController.jumpToIndex(currentIndex);
                        break;
                }
            }
        };
        alertDialog.setCancelable(false);
        alertDialog.setButton(getString(R.string.ok), errorListener);
        alertDialog.show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        HierarchyElement h = (HierarchyElement) parent.getItemAtPosition(position);
        FormIndex index = h.getFormIndex();
        if (index == null) {
            goUpLevel();
            return;
        }
        switch (h.getType()) {
            case EXPANDED:
                Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick",
                        "COLLAPSED", h.getFormIndex());
                h.setType(COLLAPSED);
                ArrayList<HierarchyElement> children = h.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    formList.remove(position + 1);
                }
                h.setIcon(ContextCompat.getDrawable(getContext().getApplicationContext(), R.drawable.expander_ic_minimized));
                break;
            case COLLAPSED:
                Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick",
                        "EXPANDED", h.getFormIndex());
                h.setType(EXPANDED);
                ArrayList<HierarchyElement> children1 = h.getChildren();
                for (int i = 0; i < children1.size(); i++) {
                    Timber.i("adding child: %s", children1.get(i).getFormIndex());
                    formList.add(position + 1 + i, children1.get(i));
                }
                h.setIcon(ContextCompat.getDrawable(getContext().getApplicationContext(), R.drawable.expander_ic_maximized));
                break;
            case QUESTION:
                Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick",
                        "QUESTION-JUMP", index);
                Collect.getInstance().getFormController().jumpToIndex(getIndexAdaptionFromSetting(index));

                ((FormEntryActivity) getActivity()).refreshCurrentView();
                return;
            case CHILD:
                Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick",
                        "REPEAT-JUMP", h.getFormIndex());
                Collect.getInstance().getFormController().jumpToIndex(h.getFormIndex());
                refreshView();
//                ((FormEntryActivity) getActivity()).refreshCurrentView();
                //if table view was shown, dont do this
                return;
        }

        // Should only get here if we've expanded or collapsed a group
        mAdapter.setListItems(formList);
        listView.setAdapter(mAdapter);
        listView.setSelection(position);
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
