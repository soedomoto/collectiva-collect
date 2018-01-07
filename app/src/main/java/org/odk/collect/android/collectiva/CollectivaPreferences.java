package org.odk.collect.android.collectiva;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.odk.collect.android.R;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.preferences.PreferenceKeys;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.FormsProviderAPI;

import java.util.ArrayList;
import java.util.HashSet;

public class CollectivaPreferences extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private LinearLayout primaryHolder;
    private LinearLayout secondaryHolder;
    private LinearLayout constraintOption;
    private LinearLayout advancedSetting;
    private TextView primaryTextSummary, secondaryTextSummary, constraintValue;
    private Switch showTableSwitch, showGroupInOneScreenSwitch;
    private String formIds = "";
    private static final String FORM_ID_KEY = "formid";
    private SharedPreferences preferences;

    public static final String TITLE_RESPONS_KEY = "titleresponskey";
    public static final String SUBTITLE_RESPONS_KEY = "subtitleresponskey";
    public static final String SHOW_REPEATGROUP_ASTABLE_KEY = "showrepeatgrouopastable";
    public static final String SHOW_ALLGROUP_INONE_SCREEN_KEY = "showgroupinonescreen";
    public static final String CONSTRAINT_TYPE_KEY = "constraint_type";

    public static final String COLLECTIVA_PREFERENCES_KEY = "collectivapreferences";

    public static final String labelConstraint[] = new String[]{"Dont allow error for every question","Let me choice to skip or not","Allow error for every question"};

    private HashSet<String> subtitles;
    private String title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collectiva_activity_preferences);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("Quick Preferences");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayout responsLabelCategory = (LinearLayout) findViewById(R.id.category_respons_label);
        primaryHolder = (LinearLayout) findViewById(R.id.primary_text_holder);
        secondaryHolder = (LinearLayout) findViewById(R.id.secondary_text_holder);
        primaryTextSummary = (TextView) findViewById(R.id.primary_text_summary);
        secondaryTextSummary = (TextView) findViewById(R.id.secondary_text_summary);
        showTableSwitch = (Switch) findViewById(R.id.switch_show_table);
        showGroupInOneScreenSwitch = (Switch) findViewById(R.id.switch_show_onscreen);
        advancedSetting = (LinearLayout) findViewById(R.id.advanced_setting);
        constraintOption = (LinearLayout) findViewById(R.id.constraint_option);
        constraintValue = (TextView) findViewById(R.id.constraint_value);

        preferences = getSharedPreferences(COLLECTIVA_PREFERENCES_KEY, MODE_PRIVATE);

        if(getIntent().getStringExtra(FORM_ID_KEY)!=null){
            formIds = getIntent().getStringExtra(FORM_ID_KEY);
            getSupportActionBar().setTitle("Instance Preferences");
        }else {
            responsLabelCategory.setVisibility(View.GONE);
        }

        initValuePreferences();

        constraintOption.setOnClickListener(this);
        advancedSetting.setOnClickListener(this);
        primaryHolder.setOnClickListener(this);
        secondaryHolder.setOnClickListener(this);
        showTableSwitch.setOnCheckedChangeListener(this);
        showGroupInOneScreenSwitch.setOnCheckedChangeListener(this);
    }

    private void initValuePreferences() {
        if(getIntent().getStringExtra(FORM_ID_KEY)!=null){
            title = preferences.getString(formIds+"_"+TITLE_RESPONS_KEY,"");
            subtitles = (HashSet<String>) preferences.getStringSet(formIds+"_"+SUBTITLE_RESPONS_KEY, new HashSet<String>());
            if(!title.equals("")) primaryTextSummary.setText(title);
            if(subtitles.size()>0) secondaryTextSummary.setText(getStringCommaFromArray(subtitles));
        }

        int constraintType = preferences.getInt(CONSTRAINT_TYPE_KEY, 0);
        constraintValue.setText(labelConstraint[constraintType]);

        boolean showRepeatGroupAsTable = preferences.getBoolean(SHOW_REPEATGROUP_ASTABLE_KEY, true);

        if(showRepeatGroupAsTable) showTableSwitch.setChecked(true);
        else showTableSwitch.setChecked(false);

        boolean showGroupInOneScreen = preferences.getBoolean(SHOW_ALLGROUP_INONE_SCREEN_KEY, true);

        if(showGroupInOneScreen) showGroupInOneScreenSwitch.setChecked(true);
        else showGroupInOneScreenSwitch.setChecked(false);
    }

    private String getStringCommaFromArray(HashSet<String> subtitles) {
        String a = "";
        for (String s:subtitles) {
            a += a.equals("")?"":"," + s;
        }
        return a;
    }

    @Override
    public void onClick(View v) {
        if(v == primaryHolder){
            selectPrimaryText();
        }else if(v==secondaryHolder){
            selectSecondaryText();
        }else if(v==constraintOption){
            selectConstraintType();
        } else if(v==advancedSetting){
//            showAdvancedSettingWithPassword();
            startActivity(new Intent(CollectivaPreferences.this, PreferencesActivity.class));
        }
    }

    private void showAdvancedSettingWithPassword(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Masukan password admin");
        
        View v = View.inflate(this, R.layout.collectiva_dialogbox_input_password, null);
        final EditText text = (EditText) v.findViewById(R.id.editText);
        builder.setView(v);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(text.getText().toString().equals("admin")) {
                    dialog.dismiss();
                    startActivity(new Intent(CollectivaPreferences.this, PreferencesActivity.class));
                    finish();
                }else {
                    Toast.makeText(CollectivaPreferences.this, "You have not allowed to open advanced setting", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void selectConstraintType() {
        AlertDialog.Builder builf = new AlertDialog.Builder(this);
        builf.setTitle("Choose action when error happen");
        builf.setSingleChoiceItems(labelConstraint, preferences.getInt(CONSTRAINT_TYPE_KEY, 0), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor edit = preferences.edit();
                edit.putInt(CONSTRAINT_TYPE_KEY, which);

                if(which==0 || which==1){
                    edit.putString(PreferenceKeys.KEY_CONSTRAINT_BEHAVIOR,PreferenceKeys.CONSTRAINT_BEHAVIOR_ON_SWIPE);
                }else {
                    edit.putString(PreferenceKeys.KEY_CONSTRAINT_BEHAVIOR,PreferenceKeys.CONSTRAINT_BEHAVIOR_DEFAULT);
                }

                edit.apply();
                constraintValue.setText(labelConstraint[which]);
                dialog.dismiss();
            }
        });
        builf.show();
    }



    private void selectSecondaryText() {
        Cursor cursor = new FormsDao().getFormsCursorForFormId(formIds);
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        build.setTitle("Choose wich question to display in respons subtitle");
        if(cursor.moveToFirst()){
            final HashSet<String> newSubtitles = new HashSet<>();
            newSubtitles.addAll(subtitles);
            final ArrayList<String> variableForm = new ArrayList<>();
            variableForm.addAll(ParseXml.getVariabelForm(cursor.getString(cursor.getColumnIndex(FormsProviderAPI.FormsColumns.FORM_FILE_PATH)), formIds));
            build.setMultiChoiceItems(variableForm.toArray(new CharSequence[variableForm.size()]), getCheckedSubtitleItems(variableForm) , new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    if(isChecked) newSubtitles.add(variableForm.get(which));
                    else newSubtitles.remove(variableForm.get(which));
                }
            });

            build.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    savePreferences(formIds+"_"+SUBTITLE_RESPONS_KEY, newSubtitles);
                    initValuePreferences();
                }
            });
            build.setNegativeButton("Cancel", null);
        }else {
            build.setMessage("Error, Form not found");
            build.setNegativeButton("Ok", null);
        }
        build.show();
    }

    private boolean[] getCheckedSubtitleItems(ArrayList<String> lists) {
        boolean[] check = new boolean[lists.size()];
        for (int i =0; i<lists.size();i++) {
            if(subtitles.contains(lists.get(i))) check[i] = true;
            else check[i] =false;
        }
        return check;
    }

    private void selectPrimaryText() {
        Cursor cursor = new FormsDao().getFormsCursorForFormId(formIds);
        final AlertDialog.Builder build = new AlertDialog.Builder(this);
        build.setTitle("Choose wich question to display in respons title");

        if(cursor.moveToFirst()){
            final ArrayList<String> options = new ArrayList<>();
            options.add("Default Questionaire name");

            final ArrayList<String> variableForm = ParseXml.getVariabelForm(cursor.getString(cursor.getColumnIndex(FormsProviderAPI.FormsColumns.FORM_FILE_PATH)), formIds);
            options.addAll(variableForm);

            final String[] newValue = {title};
            build.setSingleChoiceItems(options.toArray(new CharSequence[options.size()]), (getPositionFromArray(variableForm, title)+1), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==0) newValue[0] = "";
                    else newValue[0] = options.get(which);
                }
            });
            build.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    savePreferences(formIds+"_"+TITLE_RESPONS_KEY, newValue[0]);
                    initValuePreferences();
                }
            });
            build.setNegativeButton("Cancel", null);
        }else {
            build.setMessage("Error, Form not found");
            build.setNegativeButton("Ok", null);
        }

        build.show();
    }

    private void savePreferences(String titleResponsKey, String s) {
        SharedPreferences.Editor edit = preferences.edit();
        edit.putString(titleResponsKey,s);
        edit.apply();
    }
    private void savePreferences(String titleResponsKey, boolean s) {
        SharedPreferences.Editor edit = preferences.edit();
        edit.putBoolean(titleResponsKey,s);
        edit.apply();
    }
    private void savePreferences(String titleResponsKey, HashSet<String> s) {
        SharedPreferences.Editor edit = preferences.edit();
        edit.putStringSet(titleResponsKey,s);
        edit.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(buttonView == showTableSwitch){
            savePreferences(SHOW_REPEATGROUP_ASTABLE_KEY, isChecked);
        }else if(buttonView == showGroupInOneScreenSwitch){
            savePreferences(SHOW_ALLGROUP_INONE_SCREEN_KEY, isChecked);
        }
        initValuePreferences();
    }

    private int getPositionFromArray(ArrayList<String> as, String a){
        int i = -1;
        for (String s:as) {
            i++;
            if(s.equals(a)){
                return i;
            }
        }
        return i;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
