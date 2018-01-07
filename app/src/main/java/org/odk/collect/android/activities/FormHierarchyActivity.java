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
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
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
import org.odk.collect.android.collectiva.Var;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.HierarchyElement;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.FormEntryPromptUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class FormHierarchyActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, SearchView.OnQueryTextListener {

    private static final int CHILD = 1;
    private static final int EXPANDED = 2;
    private static final int COLLAPSED = 3;
    private static final int QUESTION = 4;

    private static final String mIndent = "     ";

    private List<HierarchyElement> formList;
    private TextView mPath;
    private ListView listView;
    private HierarchyListAdapter mAdapter;

    private FormIndex mStartIndex;
    private FormIndex currentIndex;

    private boolean isGoUpButtonEnabled = false;

    private static String searched = "";

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
            mAdapter.setListItems(newList);
        }else {
            mAdapter.setListItems(formList);
        }
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
        setContentView(R.layout.hierarchy_layout);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FormController formController = Collect.getInstance().getFormController();

        // We use a static FormEntryController to make jumping faster.
        mStartIndex = formController.getFormIndex();

        String instanName = getIntent().getStringExtra(Var.CURRENT_INSTANCE_TITLE);
        if(instanName!=null&& !instanName.equals("")) instanName = instanName +" - ";
        else instanName = "";

        getSupportActionBar().setTitle(instanName+getIntent().getStringExtra(Var.CURRENT_FORMNAME));

        mPath = (TextView) findViewById(R.id.pathtext);
        listView = (ListView) findViewById(android.R.id.list);

        listView.setDivider(null);
        listView.setDividerHeight(0);

        TextView editRespons = (TextView) findViewById(R.id.edit_respons);
        if(getIntent().getBooleanExtra(Var.ISCANEDIT,false)) editRespons.setVisibility(View.VISIBLE);
        editRespons.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DEBUGCOLL",getIntent().getSerializableExtra(Var.INSTANCE_ID).toString());
                if(getIntent().getSerializableExtra(Var.INSTANCE_ID)!=null) {
                    Uri instanceUri = ContentUris.withAppendedId(InstanceProviderAPI.InstanceColumns.CONTENT_URI,
                            (Long) getIntent().getSerializableExtra(Var.INSTANCE_ID));

                    // caller wants to view/edit a form, so launch formentryactivity
                    Intent intent = new Intent(Intent.ACTION_EDIT, instanceUri);
                    intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
                    startActivity(intent);
                    finish();
                }
            }
        });

        String formMode = getIntent().getStringExtra(ApplicationConstants.BundleKeys.FORM_MODE);
        if (ApplicationConstants.FormModes.VIEW_SENT.equalsIgnoreCase(formMode)) {
            Collect.getInstance().getFormController().stepToOuterScreenEvent();

        }

        refreshView();

        // kinda slow, but works.
        // this scrolls to the last question the user was looking at
        if (mAdapter != null && listView != null) {
            listView.post(new Runnable() {
                @Override
                public void run() {
                    int position = 0;
                    for (int i = 0; i < mAdapter.getCount(); i++) {
                        HierarchyElement he = (HierarchyElement) mAdapter.getItem(i);
                        if (mStartIndex.equals(he.getFormIndex())) {
                            position = i;
                            break;
                        }
                    }
                    listView.setSelection(position);
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Collect.getInstance().getActivityLogger().logOnStart(this);
    }

    @Override
    protected void onStop() {
        Collect.getInstance().getActivityLogger().logOnStop(this);
        super.onStop();
    }

    private void goUpLevel() {
        Collect.getInstance().getFormController().stepToOuterScreenEvent();
        refreshView();
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
                isGoUpButtonEnabled = false;
            } else {
                mPath.setVisibility(View.VISIBLE);
                mPath.setText(getCurrentPath());
                isGoUpButtonEnabled = true;
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
                                        isErrorFound?ContextCompat.getColor(this,R.color.red):Color.WHITE,
                                        QUESTION, fp.getIndex(), 1));
                            }else formList.add(
                                    new HierarchyElement(fp.getLongText(), answerDisplay, null,
                                            isErrorFound?ContextCompat.getColor(this,R.color.red):Color.WHITE,
                                            QUESTION, fp.getIndex()));
                        }
                        break;
                    case FormEntryController.EVENT_GROUP:
                        // ignore group events
                        isFoundGroup = true;
                        FormEntryCaption formC = formController.getCaptionPrompt();
                        repeatGroupRef = currentRef;
                        HierarchyElement groupQuestion =
                                new HierarchyElement(formC.getLongText(), null, ContextCompat
                                        .getDrawable(getApplicationContext(), R.drawable.expander_ic_minimized),
                                        ContextCompat.getColor(this, R.color.white),
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
                                            .getDrawable(getApplicationContext(), R.drawable.expander_ic_minimized),
                                            ContextCompat.getColor(this,R.color.roster_color),
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

            mAdapter = new HierarchyListAdapter(this, false);
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

    /**
     * Creates and displays dialog with the given errorMsg.
     */
    private void createErrorDialog(String errorMsg) {
        Collect.getInstance()
                .getActivityLogger()
                .logInstanceAction(this, "createErrorDialog", "show.");

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

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
    public void onBackPressed() {
        if(isGoUpButtonEnabled) goUpLevel();
        else {
            Collect.getInstance().getFormController().jumpToIndex(mStartIndex);
            super.onBackPressed();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        HierarchyElement h = (HierarchyElement) listView.getItemAtPosition(position);
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
                h.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.expander_ic_minimized));
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
                h.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.expander_ic_maximized));
                break;
            case QUESTION:
                Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick",
                        "QUESTION-JUMP", index);
                Collect.getInstance().getFormController().jumpToIndex(index);
                if (Collect.getInstance().getFormController().indexIsInFieldList()) {
                    try {
                        Collect.getInstance().getFormController().stepToPreviousScreenEvent();
                    } catch (JavaRosaException e) {
                        Timber.e(e);
                        createErrorDialog(e.getCause().getMessage());
                        return;
                    }
                }
//                setResult(RESULT_OK);
//                String formMode = getIntent().getStringExtra(ApplicationConstants.BundleKeys.FORM_MODE);
//                if (formMode == null || ApplicationConstants.FormModes.EDIT_SAVED.equalsIgnoreCase(formMode)) {
//                    finish();
//                }
                return;
            case CHILD:
                Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick",
                        "REPEAT-JUMP", h.getFormIndex());
                Collect.getInstance().getFormController().jumpToIndex(h.getFormIndex());
                setResult(RESULT_OK);
                refreshView();
                return;
        }

        // Should only get here if we've expanded or collapsed a group
        mAdapter.setListItems(formList);
        listView.setAdapter(mAdapter);
        listView.setSelection(position);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_upload:
                uploadThisRespon();
                break;
            case R.id.action_instance_preferences:
                startActivity(new Intent(this, CollectivaPreferences.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void uploadThisRespon() {
        long[] longs = new long[1];
        if(getIntent().getSerializableExtra(Var.INSTANCE_ID)!=null){
            longs[0] = (Long) getIntent().getSerializableExtra(Var.INSTANCE_ID);
        }
        CollectivaListRespons.uploadInstance(this, longs);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_instance, menu);

        menu.findItem(R.id.action_sort).setVisible(false);
        if(!getIntent().getBooleanExtra(Var.ISCANEDIT,false)) menu.findItem(R.id.action_upload).setVisible(false);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setMaxWidth(1000);
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        searched = s;
        if(s.length()>0){
            lastEditText[0] = System.currentTimeMillis();
            handler.postDelayed(inputFinish, delay);
        }else filterFormList("");
        return true;
    }
}
