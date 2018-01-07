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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.adapters.HierarchyListAdapter;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.HierarchyElement;
import org.odk.collect.android.utilities.FormEntryPromptUtils;

import java.util.ArrayList;

public class FormHierarchyFragmentErrorOnly extends Fragment{

    private ListView listView;
    private TextView noError;
    private HierarchyListAdapter hierarchyListAdapter;
    private EditText searchBox;
    private ImageView closeSearch;
    private ArrayList<HierarchyElement> listError;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.collectiva_fragment_hierarchy_error_layout, container, false);
        // We use a static FormEntryController to make jumping faster.
        listView = (ListView) v.findViewById(android.R.id.list);
        noError = (TextView) v.findViewById(R.id.no_error);
        searchBox = (EditText) v.findViewById(R.id.search_box);
        closeSearch = (ImageView) v.findViewById(R.id.search_close_btn);

        refreshView();

        searchBox.setHint("Search error..");
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });

        closeSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(searchBox.getText().toString().equals("")) closePanel();
                else {
                    searchBox.setText("");
                    filter("");
                }
            }
        });

        return v;
    }

    private void filter(String s) {
        if(!s.equals("")){
            ArrayList<HierarchyElement> newList = new ArrayList<>();
            for (HierarchyElement h: listError) {
                if(h.getPrimaryText()!=null) {
                    if(h.getSecondaryText()==null) h.setSecondaryText("");
                    if (h.getPrimaryText().toLowerCase().contains(s.toLowerCase()) || h.getSecondaryText().toLowerCase().contains(s.toLowerCase())) {
                        newList.add(h);
                    }
                }
            }
            hierarchyListAdapter.setListItems(newList);
        }else {
            hierarchyListAdapter.setListItems(listError);
        }
    }

    private void closePanel() {
        ((FormEntryActivity) getActivity()).getSlidingMenu().showContent();
    }

    public void refreshView() {
        listError = new ArrayList<>();
        FormController formController = Collect.getInstance().getFormController();
        ArrayList<String> errors = formController.getxPathErrors();

        for (int i = 0; i < errors.size(); i++) {
            String path = errors.get(i);
            Log.d("DEBUGC", "findex " + path);
            FormIndex fi = formController.getIndexFromXPath(path);
            if (fi != null) {
                FormEntryPrompt fp = formController.getQuestionPrompt(fi);
                String question = fp.getLongText();
                String answer = FormEntryPromptUtils.getAnswerText(fp);
                String error = getErrorMessage(fi);
                if(answer==null){
                    listError.add(new HierarchyElement(question, "Answer can't be empty", null,
                            getErrorColor(), 4, fi));
                }else {
                    listError.add(new HierarchyElement(question, "(" + answer + "), " + error, null,
                            getErrorColor(), 4, fi));
                }
            }
        }

        hierarchyListAdapter = new HierarchyListAdapter(getActivity(), true);
        hierarchyListAdapter.setListItems(listError);
        listView.setAdapter(hierarchyListAdapter);
        if(getActivity() instanceof FormEntryActivity){
            ((FormEntryActivity)getActivity()).updateTotalsError(listError.size());
            ((FormEntryActivity)getActivity()).updateViewErrorBackground(getReferencesError());
        }

        if(listError.size()>0){
            noError.setVisibility(View.GONE);
        }else noError.setVisibility(View.VISIBLE);
    }

    public ArrayList<String> getReferencesError(){
        return getReferencesFormError((ArrayList<HierarchyElement>) hierarchyListAdapter.getItems());
    }

    private ArrayList<String> getReferencesFormError(ArrayList<HierarchyElement> listError){
        ArrayList<String> arr = new ArrayList<>();
        for (HierarchyElement h:listError) {
            arr.add(h.getFormIndex().getReference().toString());
        }
        return arr;
    }


    public String getErrorMessage(FormIndex index){
        FormController formController = Collect.getInstance().getFormController();
        String constraintTextLocal = formController
                .getQuestionPromptConstraintText(index);
        Log.d("DEBUGC","error "+constraintTextLocal);

        if (constraintTextLocal == null || constraintTextLocal.trim().length()==0) {
            constraintTextLocal = formController.getQuestionPrompt(index)
                    .getSpecialFormQuestionText("constraintMsg");
            if (constraintTextLocal == null || constraintTextLocal.trim().length()==0) {
                constraintTextLocal = getString(R.string.invalid_answer_error);
            }
        }
        return constraintTextLocal;
    }

    private int getErrorColor(){
        return ContextCompat.getColor(getContext(),R.color.smooth_red);
    }

}
