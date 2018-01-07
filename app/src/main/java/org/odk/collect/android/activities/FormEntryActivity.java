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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.collectiva.CollectivaPreferences;
import org.odk.collect.android.collectiva.Var;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.exception.GDriveConnectionException;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.listeners.AdvanceToNextListener;
import org.odk.collect.android.listeners.FormLoaderListener;
import org.odk.collect.android.listeners.FormSavedListener;
import org.odk.collect.android.listeners.SavePointListener;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.FormController.FailedConstraint;
import org.odk.collect.android.preferences.AdminKeys;
import org.odk.collect.android.preferences.AdminPreferencesActivity;
import org.odk.collect.android.preferences.PreferenceKeys;
import org.odk.collect.android.provider.FormsProviderAPI.FormsColumns;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.tasks.FormLoaderTask;
import org.odk.collect.android.tasks.SavePointTask;
import org.odk.collect.android.tasks.SaveResult;
import org.odk.collect.android.tasks.SaveToDiskTask;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.utilities.ToastUtils;
import org.odk.collect.android.views.ODKView;
import org.odk.collect.android.widgets.BooleanWidget;
import org.odk.collect.android.widgets.ExStringWidget;
import org.odk.collect.android.widgets.ItemsetWidget;
import org.odk.collect.android.widgets.ListMultiWidget;
import org.odk.collect.android.widgets.ListWidget;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.SelectMultiWidget;
import org.odk.collect.android.widgets.SelectOneAutoAdvanceWidget;
import org.odk.collect.android.widgets.SelectOneSearchWidget;
import org.odk.collect.android.widgets.SelectOneWidget;
import org.odk.collect.android.widgets.SpinnerMultiWidget;
import org.odk.collect.android.widgets.SpinnerWidget;
import org.odk.collect.android.widgets.StringWidget;
import org.odk.collect.android.widgets.TriggerWidget;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * FormEntryActivity is responsible for displaying questions, animating
 * transitions between questions, and allowing the user to enter data.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Thomas Smyth, Sassafras Tech Collective (tom@sassafrastech.com; constraint behavior
 *         option)
 */
public class FormEntryActivity extends SlidingActivity implements AnimationListener,
        FormLoaderListener, FormSavedListener, AdvanceToNextListener,
        OnGestureListener, SavePointListener {
    private static final String t = "FormEntryActivity";

    // save with every swipe forward or back. Timings indicate this takes .25
    // seconds.
    // if it ever becomes an issue, this value can be changed to save every n'th
    // screen.
    private static final int SAVEPOINT_INTERVAL = 1;

    // Defines for FormEntryActivity
    private static final boolean EXIT = true;
    private static final boolean DO_NOT_EXIT = false;
    private static final int EVALUATE_CONSTRAINTS = 1;
    private static final int DO_NOT_EVALUATE_CONSTRAINTS = 2;
    private static final int EVALUATE_CONSTRAINTS_BUT_CAN_SKIP = 3;

    // Request codes for returning data from specified intent.
    public static final int IMAGE_CAPTURE = 1;
    public static final int BARCODE_CAPTURE = 2;
    public static final int AUDIO_CAPTURE = 3;
    public static final int VIDEO_CAPTURE = 4;
    public static final int LOCATION_CAPTURE = 5;
    public static final int HIERARCHY_ACTIVITY = 6;
    public static final int IMAGE_CHOOSER = 7;
    public static final int AUDIO_CHOOSER = 8;
    public static final int VIDEO_CHOOSER = 9;
    public static final int EX_STRING_CAPTURE = 10;
    public static final int EX_INT_CAPTURE = 11;
    public static final int EX_DECIMAL_CAPTURE = 12;
    public static final int DRAW_IMAGE = 13;
    public static final int SIGNATURE_CAPTURE = 14;
    public static final int ANNOTATE_IMAGE = 15;
    public static final int ALIGNED_IMAGE = 16;
    public static final int BEARING_CAPTURE = 17;
    public static final int EX_GROUP_CAPTURE = 18;
    public static final int OSM_CAPTURE = 19;
    public static final int GEOSHAPE_CAPTURE = 20;
    public static final int GEOTRACE_CAPTURE = 21;

    // Extra returned from gp activity
    public static final String LOCATION_RESULT = "LOCATION_RESULT";
    public static final String BEARING_RESULT = "BEARING_RESULT";
    public static final String GEOSHAPE_RESULTS = "GEOSHAPE_RESULTS";
    public static final String GEOTRACE_RESULTS = "GEOTRACE_RESULTS";

    public static final String KEY_INSTANCES = "instances";
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_ERROR = "error";

    // Identifies the gp of the form used to launch form entry
    public static final String KEY_FORMPATH = "formpath";

    // Identifies whether this is a new form, or reloading a form after a screen
    // rotation (or similar)
    private static final String NEWFORM = "newform";
    // these are only processed if we shut down and are restoring after an
    // external intent fires

    public static final String KEY_INSTANCEPATH = "instancepath";
    public static final String KEY_XPATH = "xpath";
    public static final String KEY_XPATH_WAITING_FOR_DATA = "xpathwaiting";

    // Tracks whether we are autosaving
    public static final String KEY_AUTO_SAVED = "autosaved";

    private static final int MENU_LANGUAGES = Menu.FIRST;
    private static final int MENU_HIERARCHY_VIEW = Menu.FIRST + 1;
    private static final int MENU_SAVE = Menu.FIRST + 2;
    private static final int MENU_PREFERENCES = Menu.FIRST + 3;

    private static final int PROGRESS_DIALOG = 1;
    private static final int SAVING_DIALOG = 2;
    private static final int SAVING_IMAGE_DIALOG = 3;

    private boolean mAutoSaved;

    // Random ID
    private static final int DELETE_REPEAT = 654321;

    private String mFormPath;
    private GestureDetector mGestureDetector;

    private Animation mInAnimation;
    private Animation mOutAnimation;
    private View mStaleView = null;

    private LinearLayout mQuestionHolder;
    private View mCurrentView;

    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    private String mErrorMessage;
    private boolean mShownAlertDialogIsGroupRepeat;

    // used to limit forward/backward swipes to one per question
    private boolean mBeenSwiped = false;

    private final Object saveDialogLock = new Object();
    private int viewCount = 0;

    private FormLoaderTask mFormLoaderTask;
    private SaveToDiskTask mSaveToDiskTask;

    private String stepMessage = "";


    enum AnimationType {
        LEFT, RIGHT, FADE
    }

    private SharedPreferences mAdminPreferences;
    private boolean mShowNavigationButtons = false;

    private FormsDao mFormsDao;

    private LinearLayout bottomNavHolder;
    private ImageView btnHierarchy, mBackBtn, mNextBtn, btnError;
    private Fragment errorFragment, hierarchyFragment;
    private TextView totalsError;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // must be at the beginning of any activity that can be called from an
        // external intent
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        setContentView(R.layout.form_entry);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.loading_form));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(getIntent().getStringExtra(Var.CURRENT_FORMNAME)!=null){
            getSupportActionBar().setTitle(getIntent().getStringExtra(Var.CURRENT_FORMNAME));
        }

        setBehindContentView(R.layout.collectiva_menu_frame_left);
        bottomNavHolder = (LinearLayout) findViewById(R.id.buttonholder);
        btnHierarchy = (ImageView) findViewById(R.id.show_hierarchy);
        mBackBtn = (ImageView) findViewById(R.id.form_back_button);
        mNextBtn = (ImageView) findViewById(R.id.form_forward_button);
        btnError = (ImageView) findViewById(R.id.show_error);
        totalsError = (TextView) findViewById(R.id.total_error);

        SlidingMenu sm = getSlidingMenu();
        sm.setShadowWidthRes(R.dimen.shadow_width);
        sm.setFadeDegree(0.35f);
        sm.setMode(SlidingMenu.LEFT_RIGHT);
        sm.setBehindWidth(260);
        sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        sm.setTouchModeBehind(SlidingMenu.TOUCHMODE_NONE);
        sm.setShadowDrawable(R.drawable.cl_shadow_left);
        sm.setSecondaryMenu(R.layout.collectiva_menu_frame_right);
        sm.setSecondaryShadowDrawable(R.drawable.cl_shadow_right);

        toggle();

        mFormsDao = new FormsDao();

        mErrorMessage = null;

        mBeenSwiped = false;
        mAlertDialog = null;
        mCurrentView = null;
        mInAnimation = null;
        mOutAnimation = null;
        mGestureDetector = new GestureDetector(this, this);
        mQuestionHolder = (LinearLayout) findViewById(R.id.questionholder);

        // get admin preference settings
        mAdminPreferences = getSharedPreferences(
                AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

        mNextBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeenSwiped = true;
                showNextView(false);
            }
        });

        mBackBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeenSwiped = true;
                showPreviousView();
            }
        });

        String startingXPath = null;
        String waitingXPath = null;
        String instancePath = null;
        Boolean newForm = true;
        mAutoSaved = false;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_FORMPATH)) {
                mFormPath = savedInstanceState.getString(KEY_FORMPATH);
            }
            if (savedInstanceState.containsKey(KEY_INSTANCEPATH)) {
                instancePath = savedInstanceState.getString(KEY_INSTANCEPATH);
            }
            if (savedInstanceState.containsKey(KEY_XPATH)) {
                startingXPath = savedInstanceState.getString(KEY_XPATH);
                Timber.i("startingXPath is: %s", startingXPath);
            }
            if (savedInstanceState.containsKey(KEY_XPATH_WAITING_FOR_DATA)) {
                waitingXPath = savedInstanceState
                        .getString(KEY_XPATH_WAITING_FOR_DATA);
                Timber.i("waitingXPath is: %s", waitingXPath);
            }
            if (savedInstanceState.containsKey(NEWFORM)) {
                newForm = savedInstanceState.getBoolean(NEWFORM, true);
            }
            if (savedInstanceState.containsKey(KEY_ERROR)) {
                mErrorMessage = savedInstanceState.getString(KEY_ERROR);
            }
            if (savedInstanceState.containsKey(KEY_AUTO_SAVED)) {
                mAutoSaved = savedInstanceState.getBoolean(KEY_AUTO_SAVED);
            }
        }

        // If a parse error message is showing then nothing else is loaded
        // Dialogs mid form just disappear on rotation.
        if (mErrorMessage != null) {
            createErrorDialog(mErrorMessage, EXIT);
            return;
        }

        // Check to see if this is a screen flip or a new form load.
        Object data = getLastNonConfigurationInstance();
        if (data instanceof FormLoaderTask) {
            mFormLoaderTask = (FormLoaderTask) data;
        } else if (data instanceof SaveToDiskTask) {
            mSaveToDiskTask = (SaveToDiskTask) data;
        } else if (data == null) {
            if (!newForm) {
                if (Collect.getInstance().getFormController() != null) {
                    refreshCurrentView();
                } else {
                    Timber.w("Reloading form and restoring state.");
                    // we need to launch the form loader to load the form
                    // controller...
                    mFormLoaderTask = new FormLoaderTask(instancePath,
                            startingXPath, waitingXPath);
                    Collect.getInstance().getActivityLogger()
                            .logAction(this, "formReloaded", mFormPath);
                    // TODO: this doesn' work (dialog does not get removed):
                    // showDialog(PROGRESS_DIALOG);
                    // show dialog before we execute...
                    mFormLoaderTask.execute(mFormPath);
                }
                return;
            }

            // Not a restart from a screen orientation change (or other).
            Collect.getInstance().setFormController(null);
            invalidateOptionsMenu();

            Intent intent = getIntent();
            if (intent != null) {
                Uri uri = intent.getData();

                if (uri != null && getContentResolver().getType(uri).equals(InstanceColumns.CONTENT_ITEM_TYPE)) {
                    // get the formId and version for this instance...
                    String jrFormId = null;
                    String jrVersion = null;
                    {
                        Cursor instanceCursor = null;
                        try {
                            instanceCursor = getContentResolver().query(uri,
                                    null, null, null, null);
                            if (instanceCursor.getCount() != 1) {
                                this.createErrorDialog(getString(R.string.bad_uri, uri), EXIT);
                                return;
                            } else {
                                instanceCursor.moveToFirst();
                                instancePath = instanceCursor
                                        .getString(instanceCursor
                                                .getColumnIndex(
                                                        InstanceColumns.INSTANCE_FILE_PATH));
                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logAction(this, "instanceLoaded",
                                                instancePath);

                                jrFormId = instanceCursor
                                        .getString(instanceCursor
                                                .getColumnIndex(InstanceColumns.JR_FORM_ID));
                                int idxJrVersion = instanceCursor
                                        .getColumnIndex(InstanceColumns.JR_VERSION);

                                jrVersion = instanceCursor.isNull(idxJrVersion) ? null
                                        : instanceCursor
                                        .getString(idxJrVersion);
                            }
                        } finally {
                            if (instanceCursor != null) {
                                instanceCursor.close();
                            }
                        }
                    }

                    String[] selectionArgs;
                    String selection;

                    if (jrVersion == null) {
                        selectionArgs = new String[]{jrFormId};
                        selection = FormsColumns.JR_FORM_ID + "=? AND "
                                + FormsColumns.JR_VERSION + " IS NULL";
                    } else {
                        selectionArgs = new String[]{jrFormId, jrVersion};
                        selection = FormsColumns.JR_FORM_ID + "=? AND "
                                + FormsColumns.JR_VERSION + "=?";
                    }

                    {
                        Cursor formCursor = null;
                        try {
                            formCursor = mFormsDao.getFormsCursor(selection, selectionArgs);
                            if (formCursor.getCount() == 1) {
                                formCursor.moveToFirst();
                                mFormPath = formCursor
                                        .getString(formCursor
                                                .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                            } else if (formCursor.getCount() < 1) {
                                this.createErrorDialog(
                                        getString(
                                                R.string.parent_form_not_present,
                                                jrFormId)
                                                + ((jrVersion == null) ? ""
                                                : "\n"
                                                + getString(R.string.version)
                                                + " "
                                                + jrVersion),
                                        EXIT);
                                return;
                            } else if (formCursor.getCount() > 1) {
                                // still take the first entry, but warn that
                                // there are multiple rows.
                                // user will need to hand-edit the SQLite
                                // database to fix it.
                                formCursor.moveToFirst();
                                mFormPath = formCursor.getString(
                                        formCursor.getColumnIndex(FormsColumns.FORM_FILE_PATH));
                                this.createErrorDialog(
                                        getString(R.string.survey_multiple_forms_error), EXIT);
                                return;
                            }
                        } finally {
                            if (formCursor != null) {
                                formCursor.close();
                            }
                        }
                    }
                } else if (uri != null && getContentResolver().getType(uri).equals(
                        FormsColumns.CONTENT_ITEM_TYPE)) {
                    Cursor c = null;
                    try {
                        c = getContentResolver().query(uri, null, null, null,
                                null);
                        if (c.getCount() != 1) {
                            this.createErrorDialog(getString(R.string.bad_uri, uri), EXIT);
                            return;
                        } else {
                            c.moveToFirst();
                            mFormPath = c.getString(c.getColumnIndex(FormsColumns.FORM_FILE_PATH));
                            // This is the fill-blank-form code path.
                            // See if there is a savepoint for this form that
                            // has never been
                            // explicitly saved
                            // by the user. If there is, open this savepoint
                            // (resume this filled-in
                            // form).
                            // Savepoints for forms that were explicitly saved
                            // will be recovered
                            // when that
                            // explicitly saved instance is edited via
                            // edit-saved-form.
                            final String filePrefix = mFormPath.substring(
                                    mFormPath.lastIndexOf('/') + 1,
                                    mFormPath.lastIndexOf('.'))
                                    + "_";
                            final String fileSuffix = ".xml.save";
                            File cacheDir = new File(Collect.CACHE_PATH);
                            File[] files = cacheDir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File pathname) {
                                    String name = pathname.getName();
                                    return name.startsWith(filePrefix)
                                            && name.endsWith(fileSuffix);
                                }
                            });
                            // see if any of these savepoints are for a
                            // filled-in form that has never been
                            // explicitly saved by the user...
                            for (int i = 0; i < files.length; ++i) {
                                File candidate = files[i];
                                String instanceDirName = candidate.getName()
                                        .substring(
                                                0,
                                                candidate.getName().length()
                                                        - fileSuffix.length());
                                File instanceDir = new File(
                                        Collect.INSTANCES_PATH + File.separator
                                                + instanceDirName);
                                File instanceFile = new File(instanceDir,
                                        instanceDirName + ".xml");
                                if (instanceDir.exists()
                                        && instanceDir.isDirectory()
                                        && !instanceFile.exists()) {
                                    // yes! -- use this savepoint file
                                    instancePath = instanceFile
                                            .getAbsolutePath();
                                    break;
                                }
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                } else {
                    Timber.e("Unrecognized URI: %s", uri);
                    this.createErrorDialog(getString(R.string.unrecognized_uri, uri), EXIT);
                    return;
                }

                mFormLoaderTask = new FormLoaderTask(instancePath, null, null);
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "formLoaded", mFormPath);
                showDialog(PROGRESS_DIALOG);
                // show dialog before we execute...
                mFormLoaderTask.execute(mFormPath);
            }
        }

        btnHierarchy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //to make save when user navigate from navigation
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                toggleLeftMenu();
                updateViewErrorBackground(((FormHierarchyFragmentErrorOnly)errorFragment).getReferencesError());
            }
        });

        btnError.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //to make save when user navigate from navigation
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                toggleRigthMenu();
//                updateViewErrorBackground(((FormHierarchyFragmentErrorOnly)errorFragment).getReferencesError());
            }
        });

        setSlidingListener();

        getSlidingMenu().showContent();
        int orientation = getResources().getConfiguration().orientation;
        if (Configuration.ORIENTATION_LANDSCAPE == orientation) {
            getSlidingMenu().showMenu();
            getSlidingMenu().setBehindWidth(260);
            btnHierarchy.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }

    /**
     * Create save-points asynchronously in order to not affect swiping performance
     * on larger forms.
     */
    private void nonblockingCreateSavePointData() {
        try {
            SavePointTask savePointTask = new SavePointTask(this);
            savePointTask.execute();
        } catch (Exception e) {
            Timber.e("Could not schedule SavePointTask. Perhaps a lot of swiping is taking place?");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_FORMPATH, mFormPath);
        FormController formController = Collect.getInstance()
                .getFormController();
        if (formController != null) {
            outState.putString(KEY_INSTANCEPATH, formController
                    .getInstancePath().getAbsolutePath());
            outState.putString(KEY_XPATH,
                    formController.getXPath(formController.getFormIndex()));
            FormIndex waiting = formController.getIndexWaitingForData();
            if (waiting != null) {
                outState.putString(KEY_XPATH_WAITING_FOR_DATA,
                        formController.getXPath(waiting));
            }
            // save the instance to a temp path...
            nonblockingCreateSavePointData();
        }
        outState.putBoolean(NEWFORM, false);
        outState.putString(KEY_ERROR, mErrorMessage);
        outState.putBoolean(KEY_AUTO_SAVED, mAutoSaved);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        FormController formController = Collect.getInstance()
                .getFormController();
        if (formController == null) {
            // we must be in the midst of a reload of the FormController.
            // try to save this callback data to the FormLoaderTask
            if (mFormLoaderTask != null
                    && mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED) {
                mFormLoaderTask.setActivityResult(requestCode, resultCode,
                        intent);
            } else {
                Timber.e("Got an activityResult without any pending form loader");
            }
            return;
        }

        if (resultCode == RESULT_CANCELED) {
            // request was canceled...
            if (requestCode != HIERARCHY_ACTIVITY) {
                ((ODKView) mCurrentView).cancelWaitingForBinaryData();
            }
            return;
        }

        switch (requestCode) {
            case BARCODE_CAPTURE:
                String sb = intent.getStringExtra("SCAN_RESULT");
                ((ODKView) mCurrentView).setBinaryData(sb);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case OSM_CAPTURE:
                String osmFileName = intent.getStringExtra("OSM_FILE_NAME");
                ((ODKView) mCurrentView).setBinaryData(osmFileName);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case EX_STRING_CAPTURE:
            case EX_INT_CAPTURE:
            case EX_DECIMAL_CAPTURE:
                String key = "value";
                boolean exists = intent.getExtras().containsKey(key);
                if (exists) {
                    Object externalValue = intent.getExtras().get(key);
                    ((ODKView) mCurrentView).setBinaryData(externalValue);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                }
                break;
            case EX_GROUP_CAPTURE:
                try {
                    Bundle extras = intent.getExtras();
                    ((ODKView) mCurrentView).setDataForFields(extras);
                } catch (JavaRosaException e) {
                    Timber.e(e);
                    createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
                }
                break;
            case DRAW_IMAGE:
            case ANNOTATE_IMAGE:
            case SIGNATURE_CAPTURE:
            case IMAGE_CAPTURE:
                /*
                 * We saved the image to the tempfile_path, but we really want it to
                 * be in: /sdcard/odk/instances/[current instnace]/something.jpg so
                 * we move it there before inserting it into the content provider.
                 * Once the android image capture bug gets fixed, (read, we move on
                 * from Android 1.6) we want to handle images the audio and video
                 */
                // The intent is empty, but we know we saved the image to the temp
                // file
                File fi = new File(Collect.TMPFILE_PATH);
                String instanceFolder = formController.getInstancePath()
                        .getParent();
                String s = instanceFolder + File.separator
                        + System.currentTimeMillis() + ".jpg";

                File nf = new File(s);
                if (!fi.renameTo(nf)) {
                    Timber.e("Failed to rename %s", fi.getAbsolutePath());
                } else {
                    Timber.i("Renamed %s to %s", fi.getAbsolutePath(), nf.getAbsolutePath());
                }

                ((ODKView) mCurrentView).setBinaryData(nf);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case ALIGNED_IMAGE:
                /*
                 * We saved the image to the tempfile_path; the app returns the full
                 * path to the saved file in the EXTRA_OUTPUT extra. Take that file
                 * and move it into the instance folder.
                 */
                String path = intent
                        .getStringExtra(android.provider.MediaStore.EXTRA_OUTPUT);
                fi = new File(path);
                instanceFolder = formController.getInstancePath().getParent();
                s = instanceFolder + File.separator + System.currentTimeMillis()
                        + ".jpg";

                nf = new File(s);
                if (!fi.renameTo(nf)) {
                    Timber.e("Failed to rename %s", fi.getAbsolutePath());
                } else {
                    Timber.i("Renamed %s to %s", fi.getAbsolutePath(), nf.getAbsolutePath());
                }

                ((ODKView) mCurrentView).setBinaryData(nf);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case IMAGE_CHOOSER:
                /*
                 * We have a saved image somewhere, but we really want it to be in:
                 * /sdcard/odk/instances/[current instnace]/something.jpg so we move
                 * it there before inserting it into the content provider. Once the
                 * android image capture bug gets fixed, (read, we move on from
                 * Android 1.6) we want to handle images the audio and video
                 */

                showDialog(SAVING_IMAGE_DIALOG);
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        saveChosenImage(intent.getData());
                    }
                };
                new Thread(runnable).start();

                break;
            case AUDIO_CAPTURE:
            case VIDEO_CAPTURE:
                Uri mediaUri = intent.getData();
                saveAudioVideoAnswer(mediaUri);
                String filePath = MediaUtils.getDataColumn(this, mediaUri, null, null);
                if (filePath != null) {
                    new File(filePath).delete();
                }
                try {
                    getContentResolver().delete(mediaUri, null, null);
                } catch (Exception e) {
                    Timber.e(e);
                }
                break;


            case AUDIO_CHOOSER:
            case VIDEO_CHOOSER:
                saveAudioVideoAnswer(intent.getData());
                break;
            case LOCATION_CAPTURE:
                String sl = intent.getStringExtra(LOCATION_RESULT);
                ((ODKView) mCurrentView).setBinaryData(sl);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case GEOSHAPE_CAPTURE:
                //String ls = intent.getStringExtra(GEOSHAPE_RESULTS);
                String gshr = intent.getStringExtra(GEOSHAPE_RESULTS);
                ((ODKView) mCurrentView).setBinaryData(gshr);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case GEOTRACE_CAPTURE:
                String traceExtra = intent.getStringExtra(GEOTRACE_RESULTS);
                ((ODKView) mCurrentView).setBinaryData(traceExtra);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case BEARING_CAPTURE:
                String bearing = intent.getStringExtra(BEARING_RESULT);
                ((ODKView) mCurrentView).setBinaryData(bearing);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case HIERARCHY_ACTIVITY:
                // We may have jumped to a new index in hierarchy activity, so
                // refresh
                break;

        }
        refreshCurrentView();
    }

    private void saveChosenImage(Uri selectedImage) {
        // Copy file to sdcard
        String instanceFolder1 = Collect.getInstance().getFormController().getInstancePath()
                .getParent();
        String destImagePath = instanceFolder1 + File.separator
                + System.currentTimeMillis() + ".jpg";

        File chosenImage;
        try {
            chosenImage = MediaUtils.getFileFromUri(this, selectedImage, Images.Media.DATA);
            if (chosenImage != null) {
                final File newImage = new File(destImagePath);
                FileUtils.copyFile(chosenImage, newImage);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissDialog(SAVING_IMAGE_DIALOG);
                        ((ODKView) mCurrentView).setBinaryData(newImage);
                        saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                        refreshCurrentView();
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissDialog(SAVING_IMAGE_DIALOG);
                        Timber.e("Could not receive chosen image");
                        showCustomToast(getString(R.string.error_occured), Toast.LENGTH_SHORT);
                    }
                });
            }
        } catch (GDriveConnectionException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismissDialog(SAVING_IMAGE_DIALOG);
                    Timber.e("Could not receive chosen image due to connection problem");
                    showCustomToast(getString(R.string.gdrive_connection_exception), Toast.LENGTH_LONG);
                }
            });
        }
    }

    private void saveAudioVideoAnswer(Uri media) {
        // For audio/video capture/chooser, we get the URI from the content
        // provider
        // then the widget copies the file and makes a new entry in the
        // content provider.
        ((ODKView) mCurrentView).setBinaryData(media);
        saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
    }

    /**
     * Refreshes the current view. the controller and the displayed view can get
     * out of sync due to dialogs and restarts caused by screen orientation
     * changes, so they're resynchronized here.
     */
    public void refreshCurrentView() {
        FormController formController = Collect.getInstance()
                .getFormController();

        // When we refresh, repeat dialog state isn't maintained, so step back
        // to the previous
        // question.
        // Also, if we're within a group labeled 'field list', step back to the
        // beginning of that
        // group.
        // That is, skip backwards over repeat prompts, groups that are not
        // field-lists,
        // repeat events, and indexes in field-lists that is not the containing
        // group.

        //lets modified to tableview, its maintain that index is always from beginner repeat group
        if(formController.indexIsAsChildInRepeat() || formController.getEvent()==FormEntryController.EVENT_REPEAT){
            if(showAsTable(formController))
                formController.jumpToIndex(formController.getFirstRelatedRepeatGroupIndex(formController.getFormIndex()));
        }
        int event = formController.getEvent();

        View current = createView(event, false);
        showView(current, AnimationType.FADE);

        //if sliding menu show, close it
        if(getSlidingMenu()!=null) getSlidingMenu().showContent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Collect.getInstance().getActivityLogger()
                .logInstanceAction(this, "onCreateOptionsMenu", "show");
        super.onCreateOptionsMenu(menu);

        menu
                .add(0, MENU_SAVE, 0, R.string.save_all_answers)
                .setIcon(R.drawable.ic_menu_save)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

//        sudah digantikan dengan slide left
//        menu
//                .add(0, MENU_HIERARCHY_VIEW, 0, R.string.view_hierarchy)
//                .setIcon(R.drawable.ic_menu_goto)
//                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu
                .add(0, MENU_LANGUAGES, 0, R.string.change_language)
                .setIcon(R.drawable.ic_menu_start_conversation)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        menu
                .add(0, MENU_PREFERENCES, 0, "Preferences")
                .setIcon(R.drawable.ic_menu_preferences)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mAdminPreferences == null) {
            return false;
        }

        FormController formController = Collect.getInstance()
                .getFormController();

        boolean useability;
        useability = mAdminPreferences.getBoolean(
                AdminKeys.KEY_SAVE_MID, true);

        menu.findItem(MENU_SAVE).setVisible(useability).setEnabled(useability);

        useability = mAdminPreferences.getBoolean(
                AdminKeys.KEY_JUMP_TO, true);

//        menu.findItem(MENU_HIERARCHY_VIEW).setVisible(useability)
//                .setEnabled(useability);

        useability = mAdminPreferences.getBoolean(
                AdminKeys.KEY_CHANGE_LANGUAGE, true)
                && (formController != null)
                && formController.getLanguages() != null
                && formController.getLanguages().length > 1;

        menu.findItem(MENU_LANGUAGES).setVisible(useability)
                .setEnabled(useability);

        useability = mAdminPreferences.getBoolean(
                AdminKeys.KEY_ACCESS_SETTINGS, true);

        menu.findItem(MENU_PREFERENCES).setVisible(useability)
                .setEnabled(useability);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FormController formController = Collect.getInstance()
                .getFormController();
        switch (item.getItemId()) {
            case MENU_LANGUAGES:
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "onOptionsItemSelected",
                                "MENU_LANGUAGES");
                createLanguageDialog();
                return true;
            case MENU_SAVE:
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "onOptionsItemSelected",
                                "MENU_SAVE");
                // don't exit
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                saveDataToDisk(DO_NOT_EXIT, isInstanceComplete(false), null);
                return true;
            case MENU_HIERARCHY_VIEW:
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "onOptionsItemSelected",
                                "MENU_HIERARCHY_VIEW");
                if (formController.currentPromptIsQuestion()) {
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                }
                Intent i = new Intent(this, FormHierarchyActivity.class);
                i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
                startActivityForResult(i, HIERARCHY_ACTIVITY);
                return true;
            case MENU_PREFERENCES:
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "onOptionsItemSelected",
                                "MENU_PREFERENCES");
                startActivity(new Intent(this, CollectivaPreferences.class));
                return true;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(getSlidingMenu().isMenuShowing() || getSlidingMenu().isSecondaryMenuShowing()){
            getSlidingMenu().showContent();
        }else {
            createQuitDialog();
        }
    }

    /**
     * Attempt to save the answer(s) in the current screen to into the data
     * model.
     *
     * @return false if any error occurs while saving (constraint violated,
     * etc...), true otherwise.
     */
    public boolean saveAnswersForCurrentScreen(int evaluateConstraints) {
        FormController formController = Collect.getInstance()
                .getFormController();
        // only try to save if the current event is a question or a field-list group
        // and current view is an ODKView (occasionally we show blank views that do not have any
        // controls to save data from)
        Log.d("DEBUGCOLL","saveAnswerForCurrentScreen, currentPrompt is question? "+ formController.currentPromptIsQuestion());
        if (formController.currentPromptIsQuestion() && mCurrentView instanceof ODKView) {
            LinkedHashMap<FormIndex, IAnswerData> answers = ((ODKView) mCurrentView)
                    .getAnswers();
            try {
                boolean isNeedToEvaluated = false;
                if(evaluateConstraints==1 || evaluateConstraints==3){
                    isNeedToEvaluated=true;
                }
                //even we pass "do not evaluated constraint", saveAllScreenAnwer still save xpath error
                ArrayList<FailedConstraint> constraints = formController.saveAllScreenAnswers(answers,
                        isNeedToEvaluated);
                updateViewQuestionManage();

                if (constraints != null && evaluateConstraints==1) {
                    createConstraintToast(constraints);
                    return false;
                }else if(constraints != null && evaluateConstraints==3){
                    createConstraintPopup(constraints);
                    return false;
                }
                //it is never happen, may be
//                else if (constraints != null && evaluateConstraints==2) {
//                    createConstraintToast(constraints);
//                    return false;
//                }
            } catch (JavaRosaException e) {
                Timber.e(e);
                createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
                return false;
            }
        }
        return true;
    }

    /**
     * Clears the answer on the screen.
     */
    private void clearAnswer(QuestionWidget qw) {
        if (qw.getAnswer() != null) {
            qw.clearAnswer();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        Collect.getInstance().getActivityLogger()
                .logInstanceAction(this, "onCreateContextMenu", "show");
        FormController formController = Collect.getInstance()
                .getFormController();

        menu.add(0, v.getId(), 0, getString(R.string.clear_answer));
        if (formController.indexContainsRepeatableGroup()) {
            menu.add(0, DELETE_REPEAT, 0, getString(R.string.delete_repeat));
        }
        menu.setHeaderTitle(getString(R.string.edit_prompt));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == DELETE_REPEAT) {
            Collect.getInstance()
                    .getActivityLogger()
                    .logInstanceAction(this, "onContextItemSelected",
                            "createDeleteRepeatConfirmDialog");
            createDeleteRepeatConfirmDialog();
        } else {
            /*
            * We don't have the right view here, so we store the View's ID as the
            * item ID and loop through the possible views to find the one the user
            * clicked on.
            */
            boolean shouldClearDialogBeShown;
            for (QuestionWidget qw : ((ODKView) mCurrentView).getWidgets()) {
                shouldClearDialogBeShown = false;
                if (qw instanceof StringWidget) {
                    for (int i = 0; i < qw.getChildCount(); i++) {
                        if (item.getItemId() == qw.getChildAt(i).getId()) {
                            shouldClearDialogBeShown = true;
                            break;
                        }
                    }
                } else if (item.getItemId() == qw.getId()) {
                    shouldClearDialogBeShown = true;
                }

                if (shouldClearDialogBeShown) {
                    Collect.getInstance()
                            .getActivityLogger()
                            .logInstanceAction(this, "onContextItemSelected",
                                    "createClearDialog", qw.getPrompt().getIndex());
                    createClearDialog(qw);
                    break;
                }
            }
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        FormController formController = Collect.getInstance()
                .getFormController();
        // if a form is loading, pass the loader task
        if (mFormLoaderTask != null
                && mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED) {
            return mFormLoaderTask;
        }

        // if a form is writing to disk, pass the save to disk task
        if (mSaveToDiskTask != null
                && mSaveToDiskTask.getStatus() != AsyncTask.Status.FINISHED) {
            return mSaveToDiskTask;
        }

        // mFormEntryController is static so we don't need to pass it.
        if (formController != null && formController.currentPromptIsQuestion()) {
            saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
        }
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("CONFIGCHANGE","device has been rotate "+newConfig.orientation);
        // Checks the orientation of the screen
        int orientation = getResources().getConfiguration().orientation;
        if (Configuration.ORIENTATION_LANDSCAPE == orientation) {
            getSlidingMenu().showMenu();
            getSlidingMenu().setBehindWidth(260);
            btnHierarchy.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray));
        } else {
            getSlidingMenu().showContent();
            getSlidingMenu().setBehindWidth(260);
            btnHierarchy.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
            btnError.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));

        }
    }

    /**
     * If we're loading, then we pass the loading thread to our next instance.
     */
//    @Override
//    public Object onRetainNonConfigurationInstance() {
//        FormController formController = Collect.getInstance()
//                .getFormController();
//        // if a form is loading, pass the loader task
//        if (mFormLoaderTask != null
//                && mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED) {
//            return mFormLoaderTask;
//        }
//
//        // if a form is writing to disk, pass the save to disk task
//        if (mSaveToDiskTask != null
//                && mSaveToDiskTask.getStatus() != AsyncTask.Status.FINISHED) {
//            return mSaveToDiskTask;
//        }
//
//        // mFormEntryController is static so we don't need to pass it.
//        if (formController != null && formController.currentPromptIsQuestion()) {
//            saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
//        }
//        return null;
//    }

    /**
     * Creates a view given the View type and an event
     *
     * @param advancingPage -- true if this results from advancing through the form
     * @return newly created View
     */
    private View createView(int event, boolean advancingPage) {
        FormController formController = Collect.getInstance()
                .getFormController();
        setTitle(formController.getFormTitle());

        switch (event) {
            case FormEntryController.EVENT_BEGINNING_OF_FORM:
                View startView = View
                        .inflate(this, R.layout.form_entry_start, null);
                setTitle(getString(R.string.app_name) + " > "
                        + formController.getFormTitle());

                // change start screen based on navigation prefs
                String navigationChoice = PreferenceManager
                        .getDefaultSharedPreferences(this).getString(
                                PreferenceKeys.KEY_NAVIGATION,
                                PreferenceKeys.KEY_NAVIGATION);
                Boolean useSwipe = false;
                Boolean useButtons = false;
                ImageView ia = ((ImageView) startView
                        .findViewById(R.id.image_swipe));
                TextView d = ((TextView) startView.findViewById(R.id.description));

                if (navigationChoice
                        .contains(PreferenceKeys.NAVIGATION_SWIPE)) {
                    useSwipe = true;
                }
                if (navigationChoice
                        .contains(PreferenceKeys.NAVIGATION_BUTTONS)) {
                    useButtons = true;
                }

                if (useSwipe && !useButtons) {
                    d.setText(getString(R.string.swipe_instructions,
                            formController.getFormTitle()));
                } else if (useButtons && !useSwipe) {
                    ia.setVisibility(View.GONE);
                    d.setText(getString(R.string.buttons_instructions,
                            formController.getFormTitle()));
                } else {
                    d.setText(getString(R.string.swipe_buttons_instructions,
                            formController.getFormTitle()));
                }
                return startView;
            case FormEntryController.EVENT_END_OF_FORM:
                View endView = View.inflate(this, R.layout.form_entry_end, null);
                ((TextView) endView.findViewById(R.id.description))
                        .setText(getString(R.string.save_enter_data_description,
                                formController.getFormTitle()));

                // checkbox for if finished or ready to send
                final CheckBox instanceComplete = ((CheckBox) endView
                        .findViewById(R.id.mark_finished));
                instanceComplete.setChecked(isInstanceComplete(true));
                //i change default is not checked
                instanceComplete.setChecked(false);

                if (!mAdminPreferences.getBoolean(
                        AdminKeys.KEY_MARK_AS_FINALIZED, true)) {
                    instanceComplete.setVisibility(View.GONE);
                }

                // edittext to change the displayed name of the instance
                final EditText saveAs = (EditText) endView
                        .findViewById(R.id.save_name);
                final TextView sa = (TextView) endView
                        .findViewById(R.id.save_form_as);

                // disallow carriage returns in the name
                InputFilter returnFilter = new InputFilter() {
                    public CharSequence filter(CharSequence source, int start,
                                               int end, Spanned dest, int dstart, int dend) {
                        for (int i = start; i < end; i++) {
                            if (Character.getType((source.charAt(i))) == Character.CONTROL) {
                                return "";
                            }
                        }
                        return null;
                    }
                };
                saveAs.setFilters(new InputFilter[]{returnFilter});

                String saveName = formController.getSubmissionMetadata().instanceName;
                if (saveName == null) {
                    // no meta/instanceName field in the form -- see if we have a
                    // name for this instance from a previous save attempt...
                    if (getContentResolver().getType(getIntent().getData())
                            == InstanceColumns.CONTENT_ITEM_TYPE) {
                        Uri instanceUri = getIntent().getData();
                        Cursor instance = null;
                        try {
                            instance = getContentResolver().query(instanceUri,
                                    null, null, null, null);
                            if (instance.getCount() == 1) {
                                instance.moveToFirst();
                                saveName = instance
                                        .getString(instance
                                                .getColumnIndex(InstanceColumns.DISPLAY_NAME));
                            }
                        } finally {
                            if (instance != null) {
                                instance.close();
                            }
                        }
                    }
                    if (saveName == null) {
                        // last resort, default to the form title
                        saveName = formController.getFormTitle();
                    }
                    // present the prompt to allow user to name the form
                    sa.setVisibility(View.VISIBLE);
                    saveAs.setText(saveName);
                    saveAs.setEnabled(true);
                    saveAs.setVisibility(View.VISIBLE);
                } else {
                    // if instanceName is defined in form, this is the name -- no
                    // revisions
                    // display only the name, not the prompt, and disable edits
                    sa.setVisibility(View.GONE);
                    saveAs.setText(saveName);
                    saveAs.setEnabled(false);
                    saveAs.setVisibility(View.VISIBLE);
                }

                saveAs.setVisibility(View.GONE);
                sa.setVisibility(View.GONE);

                // override the visibility settings based upon admin preferences
                if (!mAdminPreferences.getBoolean(
                        AdminKeys.KEY_SAVE_AS, true)) {
                    saveAs.setVisibility(View.GONE);
                    sa.setVisibility(View.GONE);
                }

                 // Create 'save' button
                 endView.findViewById(R.id.save_exit_button)
                        .setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logInstanceAction(
                                                this,
                                                "createView.saveAndExit",
                                                instanceComplete.isChecked() ? "saveAsComplete"
                                                        : "saveIncomplete");
                                // Form is marked as 'saved' here.
                                if (saveAs.getText().length() < 1) {
                                    ToastUtils.showShortToast(R.string.save_as_error);
                                } else {
                                    saveDataToDisk(EXIT, instanceComplete
                                            .isChecked(), saveAs.getText()
                                            .toString());
                                }
                            }
                        });

                if (mShowNavigationButtons) {
                    setEnabled(mBackBtn,true);
                    setEnabled(mNextBtn,false);
                }

                return endView;
            case FormEntryController.EVENT_QUESTION:
            case FormEntryController.EVENT_GROUP:
            case FormEntryController.EVENT_REPEAT:

                ODKView odkv = null;
                // should only be a group here if the event_group is a field-list
                try {
                    FormEntryPrompt[] prompts = formController.getQuestionPrompts();
                    FormEntryCaption[] groups = formController.getGroupsForCurrentIndex();

                    if(formController.indexIsAsChildInRepeat() || event==FormEntryController.EVENT_REPEAT){
                        if(showAsTable(formController))
                            //show table view!
                            odkv = new ODKView(this, formController.getAllQuestionsPromts(), groups, advancingPage, true);
                        else
                            odkv = new ODKView(this, prompts, groups, advancingPage, false);
                    }else {
                        odkv = new ODKView(this, prompts, groups, advancingPage, false);
                    }

                } catch (RuntimeException e) {
                    Timber.e(e);
                    // this is badness to avoid a crash.
                    try {
                        event = formController.stepToNextScreenEvent();
                        createErrorDialog(e.getMessage(), DO_NOT_EXIT);
                    } catch (JavaRosaException e1) {
                        Timber.e(e1);
                        createErrorDialog(e.getMessage() + "\n\n" + e1.getCause().getMessage(),
                                DO_NOT_EXIT);
                    }
                    return createView(event, advancingPage);
                }

                // Makes a "clear answer" menu pop up on long-click
                for (QuestionWidget qw : odkv.getWidgets()) {
                    if (!qw.getPrompt().isReadOnly()) {
                        // If it's a StringWidget register all its elements apart from EditText as
                        // we want to enable paste option after long click on the EditText
//                        but we dont need this actually
                        if (qw instanceof StringWidget) {
                            for (int i = 0; i < qw.getChildCount(); i++) {
                                if (!(qw.getChildAt(i) instanceof EditText)) {
                                    registerForContextMenu(qw.getChildAt(i));
                                }
                            }
                        } else {
                            registerForContextMenu(qw);
                        }

                        //we want to listen on change
                        setChangeListner(qw);
                    }
                }

                if (mShowNavigationButtons) {
                    adjustBackNavigationButtonVisibility();
                    setEnabled(mNextBtn,true);
                }
                return odkv;

            case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                createRepeatDialog();
                return new EmptyView(this);

            default:
                Timber.e("Attempted to create a view that does not exist.");
                // this is badness to avoid a crash.
                try {
                    event = formController.stepToNextScreenEvent();
                    createErrorDialog(getString(R.string.survey_internal_error), EXIT);
                } catch (JavaRosaException e) {
                    Timber.e(e);
                    createErrorDialog(e.getCause().getMessage(), EXIT);
                }
                return createView(event, advancingPage);
        }
    }

    /**
     * Disables the back button if it is first question....
     */
    private void adjustBackNavigationButtonVisibility() {
        FormController formController = Collect.getInstance()
                .getFormController();
        try {
            boolean firstQuestion = formController.stepToPreviousScreenEvent() == FormEntryController.EVENT_BEGINNING_OF_FORM;
            setEnabled(mBackBtn,!firstQuestion);
            formController.stepToNextScreenEvent();
            if (formController.getEvent() == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
                setEnabled(mBackBtn,true);
                formController.stepToNextScreenEvent();
            }
        } catch (JavaRosaException e) {
            setEnabled(mBackBtn,true);
            Timber.e(e);
        }
    }

//    private View createViewForFormBeginning(int event, boolean advancingPage,
//                                            FormController formController) {
//        try {
//            event = formController.stepToNextScreenEvent();
//
//        } catch (JavaRosaException e) {
//            Timber.e(e);
//            createErrorDialog(e.getMessage() + "\n\n" + e.getCause().getMessage(), DO_NOT_EXIT);
//        }
//
//        return createView(event, advancingPage);
//    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent mv) {
        boolean handled = mGestureDetector.onTouchEvent(mv);
        if (!handled) {
            return super.dispatchTouchEvent(mv);
        }

        return handled; // this is always true
    }

    /**
     * Determines what should be displayed on the screen. Possible options are:
     * a question, an ask repeat dialog, or the submit screen. Also saves
     * answers to the data model after checking constraints.
     */

    public void showNextView(boolean isSkipBtnPressed) {
        try {
            FormController formController = Collect.getInstance()
                    .getFormController();

            int constType = getSharedPreferences(CollectivaPreferences.
                    COLLECTIVA_PREFERENCES_KEY,MODE_PRIVATE)
                    .getInt(CollectivaPreferences.CONSTRAINT_TYPE_KEY, 0);

            if (formController.currentPromptIsQuestion()) {

                if(constType==0 & !isSkipBtnPressed){
                    if (!saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS)) {
                        // A constraint was violated so a dialog should be showing.
                        mBeenSwiped = false;
                        return;
                    }
                }else if(constType == 1 & !isSkipBtnPressed){
                    if (!saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS_BUT_CAN_SKIP)) {
                        // A constraint was violated so a dialog should be showing.
                        mBeenSwiped = false;
                        return;
                    }
                }else {
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                }
            }

            View next;

            if(formController.indexIsAsChildInRepeat() || formController.getEvent()==FormEntryController.EVENT_REPEAT){
                //must be by pass event add new repeat
                if(showAsTable(formController))
                    formController.jumpToIndex(formController.getFinishedRepeatGroupIndex(formController.getFormIndex()));
            }

            int originalEvent = formController.getEvent();
            int event = formController.stepToNextScreenEvent();

            // Helps prevent transition animation at the end of the form (if user swipes left
            // she will stay on the same screen)
            if (originalEvent == event && originalEvent == FormEntryController.EVENT_END_OF_FORM) {
                mBeenSwiped = false;
                return;
            }


            switch (event) {
                case FormEntryController.EVENT_QUESTION:
                case FormEntryController.EVENT_GROUP:
                    // create a savepoint
                    if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
                        nonblockingCreateSavePointData();
                    }
                    next = createView(event, true);
                    showView(next, AnimationType.RIGHT);
                    break;
                case FormEntryController.EVENT_END_OF_FORM:
                case FormEntryController.EVENT_REPEAT:
                case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                    next = createView(event, true);
                    showView(next, AnimationType.RIGHT);
                    break;
                case FormEntryController.EVENT_REPEAT_JUNCTURE:
                    Timber.i("Repeat juncture: %s", formController.getFormIndex().getReference());
                    // skip repeat junctures until we implement them
                    break;
                default:
                    Timber.w("JavaRosa added a new EVENT type and didn't tell us... shame on them.");
                    break;
            }
        } catch (JavaRosaException e) {
            Timber.e(e);
            createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
        }
    }

    private boolean showAsTable(FormController formController) {

        FormIndex repeatIndex = formController.getFirstRelatedRepeatGroupIndex(formController.getFormIndex());
        String repeatAttr = formController.getFormDef().getChild(repeatIndex).getAdditionalAttribute(null,"show_as_table");
        Log.d("DEBUCOLL","ATTRIBUTE ON TABLE !, "+repeatAttr);
        if(repeatAttr!=null && repeatAttr.equals("yes")){
            return true;
        }

//        if we want to show table by preferences, here
//        SharedPreferences sh = getSharedPreferences(CollectivaPreferences.COLLECTIVA_PREFERENCES_KEY, MODE_PRIVATE);
//        if(sh.getBoolean(CollectivaPreferences.SHOW_REPEATGROUP_ASTABLE_KEY,true)){
//            return true;
//        }

        Log.d("DEBUCOLL","DONT SHOW TABLE!");
        return false;
    }

    /**
     * Determines what should be displayed between a question, or the start
     * screen and displays the appropriate view. Also saves answers to the data
     * model without checking constraints.
     */
    private void showPreviousView() {
        try {
            FormController formController = Collect.getInstance()
                    .getFormController();
            // The answer is saved on a back swipe, but question constraints are
            // ignored.
            if (formController.currentPromptIsQuestion()) {
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
            }

            //if question is a repeat, go to first position of repeat grouop
            if(formController.indexIsAsChildInRepeat() || formController.getEvent()==FormEntryController.EVENT_REPEAT
                    || formController.getEvent()==FormEntryController.EVENT_PROMPT_NEW_REPEAT){
                if(showAsTable(formController))
                    formController.jumpToIndex(formController.getFirstRelatedRepeatGroupIndex(formController.getFormIndex()));
            }

            if (formController.getEvent() != FormEntryController.EVENT_BEGINNING_OF_FORM) {
                int event = formController.stepToPreviousScreenEvent();
                // If we are the begining of the form, lets revert our actions and ignore
                // this swipe
                if (event == FormEntryController.EVENT_BEGINNING_OF_FORM) {
                    event = formController.stepToNextScreenEvent();
                    mBeenSwiped = false;

                    if (event != FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
                        // Returning here prevents the same view sliding when user is on the first screen
                        return;
                    }
                }

                if (event == FormEntryController.EVENT_GROUP
                        || event == FormEntryController.EVENT_QUESTION) {
                    // create savepoint
                    if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
                        nonblockingCreateSavePointData();
                    }
                }
                Log.d("DEBUGCOLL","bfprevious current event after"+event);
                View next = createView(event, false);
                showView(next, AnimationType.LEFT);
            } else {
                mBeenSwiped = false;
            }
        } catch (JavaRosaException e) {
            Timber.e(e);
            createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
        }
    }

    /**
     * Displays the View specified by the parameter 'next', animating both the
     * current view and next appropriately given the AnimationType. Also updates
     * the progress bar.
     */
    public void showView(View next, AnimationType from) {

        // disable notifications...
        if (mInAnimation != null) {
            mInAnimation.setAnimationListener(null);
        }
        if (mOutAnimation != null) {
            mOutAnimation.setAnimationListener(null);
        }

        // logging of the view being shown is already done, as this was handled
        // by createView()
        switch (from) {
            case RIGHT:
                mInAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_left_in);
                mOutAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_left_out);
                // if animation is left or right then it was a swipe, and we want to re-save on
                // entry
                mAutoSaved = false;
                break;
            case LEFT:
                mInAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_right_in);
                mOutAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_right_out);
                mAutoSaved = false;
                break;
            case FADE:
                mInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
                mOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
                break;
        }

        // complete setup for animations...
        mInAnimation.setAnimationListener(this);
        mOutAnimation.setAnimationListener(this);

        // drop keyboard before transition...
//        if (mCurrentView != null) {
//            InputMethodManager inputManager = (InputMethodManager) getSystemService(
//                    Context.INPUT_METHOD_SERVICE);
//            inputManager.hideSoftInputFromWindow(mCurrentView.getWindowToken(),
//                    0);
//        }

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        // adjust which view is in the layout container...
        mStaleView = mCurrentView;
        mCurrentView = next;
        mQuestionHolder.addView(mCurrentView, lp);
        mAnimationCompletionSet = 0;

        if (mStaleView != null) {
            // start OutAnimation for transition...
            mStaleView.startAnimation(mOutAnimation);
            // and remove the old view (MUST occur after start of animation!!!)
            mQuestionHolder.removeView(mStaleView);
        } else {
            mAnimationCompletionSet = 2;
        }
        // start InAnimation for transition...
        mCurrentView.startAnimation(mInAnimation);

        String logString = "";
        switch (from) {
            case RIGHT:
                logString = "next";
                break;
            case LEFT:
                logString = "previous";
                break;
            case FADE:
                logString = "refresh";
                break;
        }

        Collect.getInstance().getActivityLogger().logInstanceAction(this, "showView", logString);

        FormController formController = Collect.getInstance().getFormController();
        if (formController.getEvent() == FormEntryController.EVENT_QUESTION
                || formController.getEvent() == FormEntryController.EVENT_GROUP
                || formController.getEvent() == FormEntryController.EVENT_REPEAT) {
            FormEntryPrompt[] prompts = Collect.getInstance().getFormController()
                    .getQuestionPrompts();
            Log.d("DEBUGCOLL","form entry prompts "+prompts.length);
            for (FormEntryPrompt p : prompts) {
                List<TreeElement> attrs = p.getBindAttributes();
                for (int i = 0; i < attrs.size(); i++) {
                    if (!mAutoSaved && "saveIncomplete".equals(attrs.get(i).getName())) {
                        saveDataToDisk(false, false, null, false);
                        mAutoSaved = true;
                    }
                }
            }
        }

        updateViewErrorBackground(((FormHierarchyFragmentErrorOnly)errorFragment).getReferencesError());
    }

    // Hopefully someday we can use managed dialogs when the bugs are fixed
    /*
     * Ideally, we'd like to use Android to manage dialogs with onCreateDialog()
     * and onPrepareDialog(), but dialogs with dynamic content are broken in 1.5
     * (cupcake). We do use managed dialogs for our static loading
     * ProgressDialog. The main issue we noticed and are waiting to see fixed
     * is: onPrepareDialog() is not called after a screen orientation change.
     * http://code.google.com/p/android/issues/detail?id=1639
     */

    /**
     * Creates and displays a dialog displaying the violated constraint.
     */
    private void createConstraintToast(ArrayList<FailedConstraint> failedConstraints) {
        FormController formController = Collect.getInstance()
                .getFormController();
        String constraintText = "";
        if(failedConstraints.size()>1){
            constraintText = "Can't go to the next question, You have "+failedConstraints.size()+" errors input";
        }else {
            for (int i = 0; i < failedConstraints.size(); i++) {
                FailedConstraint failedConstraint = failedConstraints.get(i);
                switch (failedConstraint.status) {
                    case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this,
                                        "createConstraintToast.ANSWER_CONSTRAINT_VIOLATED",
                                        "show", failedConstraint.index);
                        String constraintTextLocal = formController
                                .getQuestionPromptConstraintText(failedConstraint.index);
                        if (constraintTextLocal == null || constraintTextLocal.trim().length() == 0) {
                            constraintTextLocal = formController.getQuestionPrompt(failedConstraint.index)
                                    .getSpecialFormQuestionText("constraintMsg");
                            if (constraintTextLocal == null || constraintTextLocal.trim().length() == 0) {
                                constraintTextLocal = getString(R.string.invalid_answer_error);
                            }
                        }
                        constraintText = constraintTextLocal;
//                    constraintText = constraintText+constraintTextLocal;
                        break;
                    case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this,
                                        "createConstraintToast.ANSWER_REQUIRED_BUT_EMPTY",
                                        "show", failedConstraint.index);
                        String constraintTextLocalB = formController
                                .getQuestionPromptRequiredText(failedConstraint.index);
                        if (constraintTextLocalB == null || constraintTextLocalB.trim().length() == 0) {
                            constraintTextLocalB = formController.getQuestionPrompt(failedConstraint.index)
                                    .getSpecialFormQuestionText("requiredMsg");
                            if (constraintTextLocalB == null || constraintTextLocalB.trim().length() == 0) {
                                constraintTextLocalB = getString(R.string.required_answer_error);
                            }
                        }
                        constraintText = constraintTextLocalB;
//                    constraintText=constraintText+constraintTextLocalB;
                        break;
                }
            }
        }
        showCustomToast(constraintText, Toast.LENGTH_SHORT);
    }
    /**
     * Creates and displays a dialog displaying the violated constraint.
     */
    private void createConstraintPopup(final ArrayList<FailedConstraint> failedConstraints) {
        final FormController formController = Collect.getInstance()
                .getFormController();
        String constraintText = "";
        if(failedConstraints.size()>1){
            constraintText = "Can't go to the next question, You have "+failedConstraints.size()+" errors input";
        }else {
            for (int i=0; i<failedConstraints.size(); i++) {
                FailedConstraint failedConstraint = failedConstraints.get(i);
                switch (failedConstraint.status) {
                    case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this,
                                        "createConstraintToast.ANSWER_CONSTRAINT_VIOLATED",
                                        "show", failedConstraint.index);
                        String constraintTextLocal = formController
                                .getQuestionPromptConstraintText(failedConstraint.index);
                        if (constraintTextLocal == null || constraintTextLocal.trim().length() == 0) {
                            constraintTextLocal = formController.getQuestionPrompt(failedConstraint.index)
                                    .getSpecialFormQuestionText("constraintMsg");
                            if (constraintTextLocal == null || constraintTextLocal.trim().length() == 0) {
                                constraintTextLocal = getString(R.string.invalid_answer_error);
                            }
                        }
                        constraintText = constraintText + constraintTextLocal;
                        break;
                    case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this,
                                        "createConstraintToast.ANSWER_REQUIRED_BUT_EMPTY",
                                        "show", failedConstraint.index);
                        String constraintTextLocalB = formController
                                .getQuestionPromptRequiredText(failedConstraint.index);
                        if (constraintTextLocalB == null || constraintTextLocalB.trim().length() == 0) {
                            constraintTextLocalB = formController.getQuestionPrompt(failedConstraint.index)
                                    .getSpecialFormQuestionText("requiredMsg");
                            if (constraintTextLocalB == null || constraintTextLocalB.trim().length() == 0) {
                                constraintTextLocalB = getString(R.string.required_answer_error);
                            }
                        }
                        constraintText = constraintText + constraintTextLocalB;
                        break;
                    default:
                        return;
                }
                if (i + 1 != failedConstraints.size()) {
                    constraintText = constraintText + "\n";
                }
            }
        }

        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle("Error Constraint");
        mAlertDialog.setMessage(constraintText);
        mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "SKIP TO NEXT QUESTION", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showNextView(true);
            }
        });
        mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAlertDialog.dismiss();
            }
        });
        mAlertDialog.show();
    }

    /**
     * Creates a toast with the specified message.
     */
    private void showCustomToast(String message, int duration) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.toast_view, null);

        // set the text in the view
        TextView tv = (TextView) view.findViewById(R.id.message);
        tv.setText(message);

        Toast t = new Toast(this);
        t.setView(view);
        t.setDuration(duration);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    /**
     * Creates and displays a dialog asking the user if they'd like to create a
     * repeat of the current group.
     */
    public void createRepeatDialog() {
        FormController formController = Collect.getInstance()
                .getFormController();
        Collect.getInstance().getActivityLogger()
                .logInstanceAction(this, "createRepeatDialog", "show");

        //be sure before add an repeat, you save all current screen before refreshing view.
        // and jump to NEW_REPEAT event index, before call newRepeat() method;
        formController.jumpToIndex(formController.getFirstRelatedRepeatGroupIndex(formController.getFormIndex()));
        saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
        formController.jumpToIndex(formController.getFinishedRepeatGroupIndex(formController.getFormIndex()));


        // In some cases dialog might be present twice because refreshView() is being called
        // from onResume(). This ensures that we do not preset this modal dialog if it's already
        // visible. Checking for mShownAlertDialogIsGroupRepeat because the same field
        // mAlertDialog is being used for all alert dialogs in this activity.
        if (mAlertDialog != null && mAlertDialog.isShowing() && mShownAlertDialogIsGroupRepeat) {
            return;
        }

        mAlertDialog = new AlertDialog.Builder(this).create();
        DialogInterface.OnClickListener repeatListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                mShownAlertDialogIsGroupRepeat = false;
                FormController formController = Collect.getInstance()
                        .getFormController();
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE: // yes, repeat
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this, "createRepeatDialog",
                                        "addRepeat");
                        try {
                            formController.newRepeat();
                        } catch (Exception e) {
                            FormEntryActivity.this.createErrorDialog(
                                    e.getMessage(), DO_NOT_EXIT);
                            return;
                        }
                        if (!formController.indexIsInFieldList()) {
                            // we are at a REPEAT event that does not have a
                            // field-list appearance
                            // step to the next visible field...
                            // which could be the start of a new repeat group...

                            //but, we want to every repeat is be an fieldList
                            if(!showAsTable(formController)) {
                                showNextView(false);
                            }else refreshCurrentView();
                        } else {
                            // we are at a REPEAT event that has a field-list
                            // appearance
                            // just display this REPEAT event's group.
                            refreshCurrentView();
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE: // no, no repeat
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this, "createRepeatDialog",
                                        "showNext");

                        //
                        // Make sure the error dialog will not disappear.
                        //
                        // When showNextView() popups an error dialog (because of a
                        // JavaRosaException)
                        // the issue is that the "add new repeat dialog" is referenced by
                        // mAlertDialog
                        // like the error dialog. When the "no repeat" is clicked, the error dialog
                        // is shown. Android by default dismisses the dialogs when a button is
                        // clicked,
                        // so instead of closing the first dialog, it closes the second.

                        if(!showAsTable(formController)){
                            new Thread() {

                                @Override
                                public void run() {
                                    FormEntryActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Thread.sleep(500);
                                            } catch (InterruptedException e) {
                                                //This is rare
                                                Timber.e(e);
                                            }
                                            //if in table view, dont do this
                                            showNextView(false);
                                        }
                                    });
                                }
                            }.start();
                        }

                        break;
                }
            }
        };
        if (formController.getLastRepeatCount() > 0) {
            mAlertDialog.setTitle(getString(R.string.leaving_repeat_ask));
            mAlertDialog.setMessage(getString(R.string.add_another_repeat,
                    formController.getLastGroupText()));
            mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE,getString(R.string.add_another),
                    repeatListener);
            mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,getString(R.string.leave_repeat_yes),
                    repeatListener);

        } else {
            mAlertDialog.setTitle(getString(R.string.entering_repeat_ask));
            mAlertDialog.setMessage(getString(R.string.add_repeat,
                    formController.getLastGroupText()));
            mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE,getString(R.string.add_another),
                    repeatListener);
            mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,getString(R.string.leave_repeat_yes),
                    repeatListener);
        }
        mAlertDialog.setCancelable(false);
        mBeenSwiped = false;
        mShownAlertDialogIsGroupRepeat = true;
        mAlertDialog.show();
    }

    /**
     * Creates and displays dialog with the given errorMsg.
     */
    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        Collect.getInstance()
                .getActivityLogger()
                .logInstanceAction(this, "createErrorDialog",
                        "show." + Boolean.toString(shouldExit));

        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            errorMsg = mErrorMessage + "\n\n" + errorMsg;
            mErrorMessage = errorMsg;
        } else {
            mAlertDialog = new AlertDialog.Builder(this).create();
            mErrorMessage = errorMsg;
        }

        mAlertDialog.setTitle(getString(R.string.error_occured));
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Collect.getInstance().getActivityLogger()
                                .logInstanceAction(this, "createErrorDialog", "OK");
                        if (shouldExit) {
                            mErrorMessage = null;
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mBeenSwiped = false;
        mAlertDialog.show();
    }

    /**
     * Creates a confirm/cancel dialog for deleting repeats.
     */
    public void createDeleteRepeatConfirmDialog() {
        Collect.getInstance()
                .getActivityLogger()
                .logInstanceAction(this, "createDeleteRepeatConfirmDialog",
                        "show");
        FormController formController = Collect.getInstance()
                .getFormController();

        mAlertDialog = new AlertDialog.Builder(this).create();
        String name = formController.getLastRepeatedGroupName();
        int repeatcount = formController.getLastRepeatedGroupRepeatCount();
        if (repeatcount != -1) {
            name += " (" + (repeatcount + 1) + ")";
        }
        mAlertDialog.setTitle(getString(R.string.delete_repeat_ask));
        mAlertDialog
                .setMessage(getString(R.string.delete_repeat_confirm, name));
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                FormController formController = Collect.getInstance()
                        .getFormController();
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE: // yes
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this,
                                        "createDeleteRepeatConfirmDialog", "OK");
                        formController.deleteRepeat();
                        refreshCurrentView();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE: // no
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this,
                                        "createDeleteRepeatConfirmDialog", "cancel");

                        refreshCurrentView();
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE,getString(R.string.discard_group), quitListener);
        mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,getString(R.string.delete_repeat_no),
                quitListener);
        mAlertDialog.show();
    }

    /**
     * Saves data and writes it to disk. If exit is set, program will exit after
     * save completes. Complete indicates whether the user has marked the
     * isntancs as complete. If updatedSaveName is non-null, the instances
     * content provider is updated with the new name
     */
    // by default, save the current screen
    private boolean saveDataToDisk(boolean exit, boolean complete, String updatedSaveName) {
        return saveDataToDisk(exit, complete, updatedSaveName, true);
    }

    // but if you want save in the background, can't be current screen
    private boolean saveDataToDisk(boolean exit, boolean complete, String updatedSaveName,
                                   boolean current) {

        // save current answer
        if (current) {
            int i=0;
            if(complete){
                i=1; //complete, so need to evaluate
            }else{
                i=2; //no need to evaluate
            }

//          if you want to mark form complete, even error happend, do this
//          form will not evaluate their error.
//            if(getSharedPreferences(CollectivaPreferences.COLLECTIVA_PREFERENCES_KEY,MODE_PRIVATE)
//                    .getBoolean(CollectivaPreferences.CONSTRAINT_TYPE_KEY,false)){
//                i = 2;
//            }

            if (!saveAnswersForCurrentScreen(i)) {
                ToastUtils.showShortToast(R.string.data_saved_error);
                return false;
            }
        }

        synchronized (saveDialogLock) {
            mSaveToDiskTask = new SaveToDiskTask(getIntent().getData(), exit, complete,
                    updatedSaveName);
            mSaveToDiskTask.setFormSavedListener(this);
            mAutoSaved = true;
            showDialog(SAVING_DIALOG);
            // show dialog before we execute...
            mSaveToDiskTask.execute();
        }

        return true;
    }

    /**
     * Create a dialog with options to save and exit, save, or quit without
     * saving
     */
    private void createQuitDialog() {
        String title;
        {
            FormController formController = Collect.getInstance().getFormController();
            title = (formController == null) ? null : formController.getFormTitle();
            if (title == null) {
                title = getString(R.string.no_form_loaded);
            }
        }

        String[] items;
        if (mAdminPreferences.getBoolean(AdminKeys.KEY_SAVE_MID,
                true)) {
            items = new String[]{getString(R.string.keep_changes),
                    getString(R.string.do_not_save)};
        } else {
            items = new String[]{getString(R.string.do_not_save)};
        }

        Collect.getInstance().getActivityLogger()
                .logInstanceAction(this, "createQuitDialog", "show");
        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle(
                        getString(R.string.quit_application, title))
                .setNeutralButton(getString(R.string.do_not_exit),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {

                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logInstanceAction(this,
                                                "createQuitDialog", "cancel");
                                dialog.cancel();

                            }
                        })
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {

                            case 0: // save and exit
                                // this is slightly complicated because if the
                                // option is disabled in
                                // the admin menu, then case 0 actually becomes
                                // 'discard and exit'
                                // whereas if it's enabled it's 'save and exit'
                                if (mAdminPreferences
                                        .getBoolean(
                                                AdminKeys.KEY_SAVE_MID,
                                                true)) {
                                    Collect.getInstance()
                                            .getActivityLogger()
                                            .logInstanceAction(this,
                                                    "createQuitDialog",
                                                    "saveAndExit");
                                    saveDataToDisk(EXIT, isInstanceComplete(false),
                                            null);
                                } else {
                                    Collect.getInstance()
                                            .getActivityLogger()
                                            .logInstanceAction(this,
                                                    "createQuitDialog",
                                                    "discardAndExit");
                                    removeTempInstance();
                                    finishReturnInstance();
                                }
                                break;

                            case 1: // discard changes and exit
                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logInstanceAction(this,
                                                "createQuitDialog",
                                                "discardAndExit");

                                // close all open databases of external data.
                                Collect.getInstance().getExternalDataManager().close();

                                removeTempInstance();
                                finishReturnInstance();
                                break;

                            case 2:// do nothing
                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logInstanceAction(this,
                                                "createQuitDialog", "cancel");
                                break;
                        }
                    }
                }).create();
        mAlertDialog.show();
    }

    /**
     * this method cleans up unneeded files when the user selects 'discard and
     * exit'
     */
    private void removeTempInstance() {
        FormController formController = Collect.getInstance()
                .getFormController();

        // attempt to remove any scratch file
        File temp = SaveToDiskTask.savepointFile(formController
                .getInstancePath());
        if (temp.exists()) {
            temp.delete();
        }

        boolean erase = false;
        {
            Cursor c = null;
            try {
                c = new InstancesDao().getInstancesCursorForFilePath(formController.getInstancePath()
                        .getAbsolutePath());
                erase = (c.getCount() < 1);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        // if it's not already saved, erase everything
        if (erase) {
            // delete media first
            String instanceFolder = formController.getInstancePath()
                    .getParent();
            Timber.i("Attempting to delete: %s", instanceFolder);
            int images = MediaUtils
                    .deleteImagesInFolderFromMediaProvider(formController
                            .getInstancePath().getParentFile());
            int audio = MediaUtils
                    .deleteAudioInFolderFromMediaProvider(formController
                            .getInstancePath().getParentFile());
            int video = MediaUtils
                    .deleteVideoInFolderFromMediaProvider(formController
                            .getInstancePath().getParentFile());

            Timber.i("Removed from content providers: %d image files, %d audio files and %d audio files.",
                    images, audio, video);
            File f = new File(instanceFolder);
            if (f.exists() && f.isDirectory()) {
                for (File del : f.listFiles()) {
                    Timber.i("Deleting file: %s", del.getAbsolutePath());
                    del.delete();
                }
                f.delete();
            }
        }
    }

    /**
     * Confirm clear answer dialog
     */
    private void createClearDialog(final QuestionWidget qw) {
        Collect.getInstance()
                .getActivityLogger()
                .logInstanceAction(this, "createClearDialog", "show",
                        qw.getPrompt().getIndex());
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle(getString(R.string.clear_answer_ask));

        String question = qw.getPrompt().getLongText();
        if (question == null) {
            question = "";
        }
        if (question.length() > 50) {
            question = question.substring(0, 50) + "...";
        }

        mAlertDialog.setMessage(getString(R.string.clearanswer_confirm,
                question));

        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE: // yes
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this, "createClearDialog",
                                        "clearAnswer", qw.getPrompt().getIndex());
                        clearAnswer(qw);
                        saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE: // no
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this, "createClearDialog",
                                        "cancel", qw.getPrompt().getIndex());
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog
                .setButton(DialogInterface.BUTTON_POSITIVE,getString(R.string.discard_answer), quitListener);
        mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,getString(R.string.clear_answer_no),
                quitListener);
        mAlertDialog.show();
    }

    /**
     * Creates and displays a dialog allowing the user to set the language for
     * the form.
     */
    private void createLanguageDialog() {
        Collect.getInstance().getActivityLogger()
                .logInstanceAction(this, "createLanguageDialog", "show");
        FormController formController = Collect.getInstance()
                .getFormController();
        final String[] languages = formController.getLanguages();
        int selected = -1;
        if (languages != null) {
            String language = formController.getLanguage();
            for (int i = 0; i < languages.length; i++) {
                if (language.equals(languages[i])) {
                    selected = i;
                }
            }
        }
        mAlertDialog = new AlertDialog.Builder(this)
                .setSingleChoiceItems(languages, selected,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                FormController formController = Collect
                                        .getInstance().getFormController();
                                // Update the language in the content provider
                                // when selecting a new
                                // language
                                ContentValues values = new ContentValues();
                                values.put(FormsColumns.LANGUAGE,
                                        languages[whichButton]);
                                String selection = FormsColumns.FORM_FILE_PATH
                                        + "=?";
                                String[] selectArgs = {mFormPath};
                                int updated = mFormsDao.updateForm(values, selection, selectArgs);
                                Timber.i("Updated language to: %s in %d rows",
                                        languages[whichButton],
                                        updated);

                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logInstanceAction(
                                                this,
                                                "createLanguageDialog",
                                                "changeLanguage."
                                                        + languages[whichButton]);
                                formController
                                        .setLanguage(languages[whichButton]);
                                dialog.dismiss();
                                if (formController.currentPromptIsQuestion()) {
                                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                                }
                                refreshCurrentView();
                            }
                        })
                .setTitle(getString(R.string.change_language))
                .setNegativeButton(getString(R.string.do_not_change),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logInstanceAction(this,
                                                "createLanguageDialog",
                                                "cancel");
                            }
                        }).create();
        mAlertDialog.show();
    }

    /**
     * We use Android's dialog management for loading/saving progress dialogs
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                Timber.e("Creating PROGRESS_DIALOG");
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "onCreateDialog.PROGRESS_DIALOG",
                                "show");
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logInstanceAction(this,
                                                "onCreateDialog.PROGRESS_DIALOG", "cancel");
                                dialog.dismiss();
                                mFormLoaderTask.setFormLoaderListener(null);
                                FormLoaderTask t = mFormLoaderTask;
                                mFormLoaderTask = null;
                                t.cancel(true);
                                t.destroy();
                                finish();
                            }
                        };
                mProgressDialog.setTitle(getString(R.string.loading_form));
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,getString(R.string.cancel_loading_form),
                        loadingButtonListener);
                return mProgressDialog;
            case SAVING_DIALOG:
                Timber.e("Creating SAVING_DIALOG");
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "onCreateDialog.SAVING_DIALOG",
                                "show");
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener cancelSavingButtonListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logInstanceAction(this,
                                                "onCreateDialog.SAVING_DIALOG", "cancel");
                                dialog.dismiss();
                                cancelSaveToDiskTask();
                            }
                        };
                mProgressDialog.setTitle(getString(R.string.saving_form));
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this,
                                        "onCreateDialog.SAVING_DIALOG", "OnDismissListener");
                        cancelSaveToDiskTask();
                    }
                });
                return mProgressDialog;

            case SAVING_IMAGE_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setCancelable(false);

                return mProgressDialog;
        }
        return null;
    }

    private void cancelSaveToDiskTask() {
        synchronized (saveDialogLock) {
            mSaveToDiskTask.setFormSavedListener(null);
            boolean cancelled = mSaveToDiskTask.cancel(true);
            Timber.w("Cancelled SaveToDiskTask! (%s)", cancelled);
            mSaveToDiskTask = null;
        }
    }

    /**
     * Dismiss any showing dialogs that we manually manage.
     */
    private void dismissDialogs() {
        Timber.e("Dismiss dialogs");
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onPause() {
        FormController formController = Collect.getInstance()
                .getFormController();
        dismissDialogs();
        // make sure we're not already saving to disk. if we are, currentPrompt
        // is getting constantly updated
        if (mSaveToDiskTask == null
                || mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
            if (mCurrentView != null && formController != null
                    && formController.currentPromptIsQuestion()) {
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
            }
        }
        if (mCurrentView != null && mCurrentView instanceof ODKView) {
            // stop audio if it's playing
            ((ODKView) mCurrentView).stopAudio();
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mErrorMessage != null) {
            if (mAlertDialog != null && !mAlertDialog.isShowing()) {
                createErrorDialog(mErrorMessage, EXIT);
            } else {
                return;
            }
        }

        FormController formController = Collect.getInstance().getFormController();
        Collect.getInstance().getActivityLogger().open();

        if (mFormLoaderTask != null) {
            mFormLoaderTask.setFormLoaderListener(this);
            if (formController == null
                    && mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
                FormController fec = mFormLoaderTask.getFormController();
                if (fec != null) {
                    loadingComplete(mFormLoaderTask);
                } else {
                    dismissDialog(PROGRESS_DIALOG);
                    FormLoaderTask t = mFormLoaderTask;
                    mFormLoaderTask = null;
                    t.cancel(true);
                    t.destroy();
                    // there is no formController -- fire MainMenu activity?
                    startActivity(new Intent(this, MainMenuActivity.class));
                }
            }
        } else {
            if (formController == null) {
                // there is no formController -- fire MainMenu activity?
                startActivity(new Intent(this, MainMenuActivity.class));
                return;
            } else {
                refreshCurrentView();
            }
        }

        if (mSaveToDiskTask != null) {
            mSaveToDiskTask.setFormSavedListener(this);
        }

        // only check the buttons if it's enabled in preferences
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        String navigation = sharedPreferences.getString(
                PreferenceKeys.KEY_NAVIGATION,
                PreferenceKeys.KEY_NAVIGATION);
        if (navigation.contains(PreferenceKeys.NAVIGATION_BUTTONS)) {
            mShowNavigationButtons = true;
        }

        //always show navigation buttons
//        if (mShowNavigationButtons) {
//            bottomNavHolder.setVisibility(View.VISIBLE);
//        } else {
//            bottomNavHolder.setVisibility(View.GONE);
//        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Collect.getInstance().getActivityLogger()
                        .logInstanceAction(this, "onKeyDown.KEYCODE_BACK", "quit");
                onBackPressed();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (event.isAltPressed() && !mBeenSwiped) {
                    mBeenSwiped = true;
                    Collect.getInstance()
                            .getActivityLogger()
                            .logInstanceAction(this,
                                    "onKeyDown.KEYCODE_DPAD_RIGHT", "showNext");
                    showNextView(false);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (event.isAltPressed() && !mBeenSwiped) {
                    mBeenSwiped = true;
                    Collect.getInstance()
                            .getActivityLogger()
                            .logInstanceAction(this, "onKeyDown.KEYCODE_DPAD_LEFT",
                                    "showPrevious");
                    showPreviousView();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (mFormLoaderTask != null) {
            mFormLoaderTask.setFormLoaderListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the FEC in memory.
            // but only if it's done, otherwise the thread never returns
            if (mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
                FormLoaderTask t = mFormLoaderTask;
                mFormLoaderTask = null;
                t.cancel(true);
                t.destroy();
            }
        }
        if (mSaveToDiskTask != null) {
            mSaveToDiskTask.setFormSavedListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the FEC in memory.
            if (mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
                mSaveToDiskTask.cancel(true);
                mSaveToDiskTask = null;
            }
        }

        super.onDestroy();

    }

    private int mAnimationCompletionSet = 0;

    private void afterAllAnimations() {
        Timber.i("afterAllAnimations");
        if (mStaleView != null) {
            if (mStaleView instanceof ODKView) {
                // http://code.google.com/p/android/issues/detail?id=8488
                ((ODKView) mStaleView).recycleDrawables();
            }
            mStaleView = null;
        }

        if (mCurrentView != null && mCurrentView instanceof ODKView) {
            //show keyboard if any edit text loaded in currentView
            //((ODKView) mCurrentView).setFocus(this);
        }
        mBeenSwiped = false;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        Timber.i("onAnimationEnd %s",
                ((animation == mInAnimation) ? "in"
                : ((animation == mOutAnimation) ? "out" : "other")));
        if (mInAnimation == animation) {
            mAnimationCompletionSet |= 1;
        } else if (mOutAnimation == animation) {
            mAnimationCompletionSet |= 2;
        } else {
            Timber.e("Unexpected animation");
        }

        if (mAnimationCompletionSet == 3) {
            this.afterAllAnimations();
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // Added by AnimationListener interface.
        Timber.i("onAnimationRepeat %s",
                ((animation == mInAnimation) ? "in"
                : ((animation == mOutAnimation) ? "out" : "other")));
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // Added by AnimationListener interface.
        Timber.i("onAnimationStart %s",
                ((animation == mInAnimation) ? "in"
                : ((animation == mOutAnimation) ? "out" : "other")));
    }

    /**
     * loadingComplete() is called by FormLoaderTask once it has finished
     * loading a form.
     */
    @Override
    public void loadingComplete(FormLoaderTask task) {
        dismissDialog(PROGRESS_DIALOG);

        FormController formController = task.getFormController();
        boolean pendingActivityResult = task.hasPendingActivityResult();
        boolean hasUsedSavepoint = task.hasUsedSavepoint();
        int requestCode = task.getRequestCode(); // these are bogus if
        // pendingActivityResult is
        // false
        int resultCode = task.getResultCode();
        Intent intent = task.getIntent();

        mFormLoaderTask.setFormLoaderListener(null);
        FormLoaderTask t = mFormLoaderTask;
        mFormLoaderTask = null;
        t.cancel(true);
        t.destroy();
        Collect.getInstance().setFormController(formController);
        invalidateOptionsMenu();


        Collect.getInstance().setExternalDataManager(task.getExternalDataManager());

        // Set the language if one has already been set in the past
        String[] languageTest = formController.getLanguages();
        if (languageTest != null) {
            String defaultLanguage = formController.getLanguage();
            String newLanguage = "";
            Cursor c = null;
            try {
                c = mFormsDao.getFormsCursorForFormFilePath(mFormPath);
                if (c.getCount() == 1) {
                    c.moveToFirst();
                    newLanguage = c.getString(c
                            .getColumnIndex(FormsColumns.LANGUAGE));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            // if somehow we end up with a bad language, set it to the default
            try {
                formController.setLanguage(newLanguage);
            } catch (Exception e) {
                Timber.e("Ended up with a bad language. %s", newLanguage);
                formController.setLanguage(defaultLanguage);
            }
        }

        if(!getIntent().getBooleanExtra("fillblankform",false) && !(formController.getxPathErrors().size()>0)){
            formController.validateAnswerAndSaveError();
        }

        if (pendingActivityResult) {
            // set the current view to whatever group we were at...
            refreshCurrentView();
            // process the pending activity request...
            onActivityResult(requestCode, resultCode, intent);
            return;
        }

        // it can be a normal flow for a pending activity result to restore from
        // a savepoint
        // (the call flow handled by the above if statement). For all other use
        // cases, the
        // user should be notified, as it means they wandered off doing other
        // things then
        // returned to ODK Collect and chose Edit Saved Form, but that the
        // savepoint for that
        // form is newer than the last saved version of their form data.
//        if (hasUsedSavepoint) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    ToastUtils.showLongToast(R.string.savepoint_used);
//                }
//            });
//        }

        // Set saved answer path
        if (formController.getInstancePath() == null) {

            // Create new answer folder.
            String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",
                    Locale.ENGLISH).format(Calendar.getInstance().getTime());
            String file = mFormPath.substring(mFormPath.lastIndexOf('/') + 1,
                    mFormPath.lastIndexOf('.'));
            String path = Collect.INSTANCES_PATH + File.separator + file + "_"
                    + time;
            if (FileUtils.createFolder(path)) {
                formController.setInstancePath(new File(path + File.separator
                        + file + "_" + time + ".xml"));
            }
            setUpListViewQuestionManage();
        } else {

            Intent reqIntent = getIntent();
            boolean showFirst = reqIntent.getBooleanExtra("start", false);

            if (!showFirst) {
                // we've just loaded a saved form, so start in the hierarchy view
                Intent i = new Intent(this, FormHierarchyActivity.class);
                String formMode = reqIntent.getStringExtra(ApplicationConstants.BundleKeys.FORM_MODE);
                if (formMode == null || ApplicationConstants.FormModes.EDIT_SAVED.equalsIgnoreCase(formMode)) {
                    setUpListViewQuestionManage();
                    refreshCurrentView();
                    return; // so we don't show the intro screen before jumping to the hierarchy
                } else {
                    if (ApplicationConstants.FormModes.VIEW_SENT.equalsIgnoreCase(formMode)) {
                        i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.VIEW_SENT);
                        i.putExtra(Var.INSTANCE_ID,reqIntent.getSerializableExtra(Var.INSTANCE_ID));
                        i.putExtra(Var.CURRENT_FORMNAME, reqIntent.getStringExtra(Var.CURRENT_FORMNAME));
                        i.putExtra(Var.CURRENT_INSTANCE_TITLE, reqIntent.getStringExtra(Var.CURRENT_INSTANCE_TITLE));
                        i.putExtra(Var.ISCANEDIT, reqIntent.getBooleanExtra(Var.ISCANEDIT,false));
                        startActivity(i);
                    }
                  finish();
                }
            }else setUpListViewQuestionManage();

        }
        refreshCurrentView();
    }

    private void setUpListViewQuestionManage() {
        errorFragment = new FormHierarchyFragmentErrorOnly();
        hierarchyFragment = new FormHierarchyFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.menu_frame, hierarchyFragment).commit();
        getSupportFragmentManager().beginTransaction().replace(R.id.menu_frame_right, errorFragment).commit();
    }


    private void updateViewQuestionManage() {
        ((FormHierarchyFragmentErrorOnly) errorFragment).refreshView();
        ((FormHierarchyFragment) hierarchyFragment).refreshView();
    }

    public void updateViewErrorBackground(ArrayList<String> listError){
        if(mCurrentView!=null &&  mCurrentView instanceof ODKView) {
            ArrayList<QuestionWidget> qs = ((ODKView) mCurrentView).getWidgets();
            for (QuestionWidget q : qs) {
                String ref = q.getPrompt().getIndex().getReference().toString();
                if (listError.contains(ref)) {
                    if(q instanceof StringWidget){
                        StringWidget sw = (StringWidget) q;
                        sw.setErrorInput();
                    }else {
                        q.setBackgroundColor(ContextCompat.getColor(this, R.color.smooth_red));
                    }
                }else {
                    //no error
                    if(q instanceof StringWidget){
                        StringWidget sw = (StringWidget) q;
                        sw.setNonErrorInput();
                    }else {
                        q.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
                    }
                }
            }
        }
    }


    public void updateTotalsError(int error){
        if(error==0){
            totalsError.setText("");
            totalsError.setVisibility(View.GONE);
        }else {
            totalsError.setVisibility(View.VISIBLE);
            totalsError.setText(""+error);
            totalsError.setBackgroundResource(R.drawable.cl_cicle_bg_red_stroke_white);
        }
    }

    /**
     * called by the FormLoaderTask if something goes wrong.
     */
    @Override
    public void loadingError(String errorMsg) {
        dismissDialog(PROGRESS_DIALOG);
        if (errorMsg != null) {
            createErrorDialog(errorMsg, EXIT);
        } else {
            createErrorDialog(getString(R.string.parse_error), EXIT);
        }
    }

    /**
     * Called by SavetoDiskTask if everything saves correctly.
     */
    @Override
    public void savingComplete(SaveResult saveResult) {
        dismissDialog(SAVING_DIALOG);

        int saveStatus = saveResult.getSaveResult();
        switch (saveStatus) {
            case SaveToDiskTask.SAVED:
                ToastUtils.showShortToast(R.string.data_saved_ok);
                sendSavedBroadcast();
                break;
            case SaveToDiskTask.SAVED_AND_EXIT:
                ToastUtils.showShortToast(R.string.data_saved_ok);
                sendSavedBroadcast();
                finishReturnInstance();
                break;
            case SaveToDiskTask.SAVE_ERROR:
                String message;
                if (saveResult.getSaveErrorMessage() != null) {
                    message = getString(R.string.data_saved_error) + ": "
                            + saveResult.getSaveErrorMessage();
                } else {
                    message = getString(R.string.data_saved_error);
                }
                ToastUtils.showLongToast(message);
                break;
            case SaveToDiskTask.ENCRYPTION_ERROR:
                ToastUtils.showLongToast(String.format(getString(R.string.encryption_error_message),
                        saveResult.getSaveErrorMessage()));
                finishReturnInstance();
                break;
            case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
            case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
                refreshCurrentView();

                // get constraint behavior preference value with appropriate default
                String constraintBehavior = PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(PreferenceKeys.KEY_CONSTRAINT_BEHAVIOR,
                                PreferenceKeys.CONSTRAINT_BEHAVIOR_DEFAULT);

                // an answer constraint was violated, so we need to display the proper toast(s)
                // if constraint behavior is on_swipe, this will happen if we do a 'swipe' to the
                // next question
                if (constraintBehavior.equals(PreferenceKeys.CONSTRAINT_BEHAVIOR_ON_SWIPE)) {
                    next();
                } else {
                    // otherwise, we can get the proper toast(s) by saving with constraint check
                    saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS);
                }

                break;
        }
    }

    @Override
    public void onProgressStep(String stepMessage) {
        this.stepMessage = stepMessage;
        if (mProgressDialog != null) {
            mProgressDialog.setMessage(getString(R.string.please_wait) + "\n\n" + stepMessage);
        }
    }

    /**
     * Checks the database to determine if the current instance being edited has
     * already been 'marked completed'. A form can be 'unmarked' complete and
     * then resaved.
     *
     * @return true if form has been marked completed, false otherwise.
     */
    private boolean isInstanceComplete(boolean end) {
        FormController formController = Collect.getInstance()
                .getFormController();
        // default to false if we're mid form
        boolean complete = false;

        // if we're at the end of the form, then check the preferences
        if (end) {
            // First get the value from the preferences
            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(this);
            complete = sharedPreferences.getBoolean(
                    PreferenceKeys.KEY_COMPLETED_DEFAULT, true);
        }

        // Then see if we've already marked this form as complete before
        Cursor c = null;
        try {
            c = new InstancesDao().getInstancesCursorForFilePath(formController.getInstancePath()
                    .getAbsolutePath());
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String status = c.getString(c
                        .getColumnIndex(InstanceColumns.STATUS));
                if (InstanceProviderAPI.STATUS_COMPLETE.compareTo(status) == 0) {
                    complete = true;
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return complete;
    }

    public void next() {
        if (!mBeenSwiped) {
            mBeenSwiped = true;
            showNextView(false);
        }
    }

    /**
     * Returns the instance that was just filled out to the calling activity, if
     * requested.
     */
    private void finishReturnInstance() {
        FormController formController = Collect.getInstance()
                .getFormController();
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action)
                || Intent.ACTION_EDIT.equals(action)) {
            // caller is waiting on a picked form
            Cursor c = null;
            try {
                c = new InstancesDao().getInstancesCursorForFilePath(formController.getInstancePath()
                        .getAbsolutePath());
                if (c.getCount() > 0) {
                    // should only be one...
                    c.moveToFirst();
                    String id = c.getString(c
                            .getColumnIndex(InstanceColumns._ID));
                    Uri instance = Uri.withAppendedPath(
                            InstanceColumns.CONTENT_URI, id);
                    setResult(RESULT_OK, new Intent().setData(instance));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        finish();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        // only check the swipe if it's enabled in preferences
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        String navigation = sharedPreferences.getString(
                PreferenceKeys.KEY_NAVIGATION,
                PreferenceKeys.NAVIGATION_SWIPE);
        Boolean doSwipe = false;

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        if (navigation.contains(PreferenceKeys.NAVIGATION_SWIPE)) {
            doSwipe = true;

            int height = dm.heightPixels;
            int width = dm.widthPixels;

            int perX= (int)((e1.getX()*100)/width);
            int perY= (int)((e1.getY()*100)/height);
            Log.d("DEBUGCOL","user swipe from X: "+perX+", Y: "+perY);
            if(perX < 5 || perX > 95 || perY < 5 || perY>95){
                //if getsliding menu not showing,
                // dont do swipe if user swipe from edge edge
                if(!getSlidingMenu().isMenuShowing() && !getSlidingMenu().isSecondaryMenuShowing())
                    doSwipe = false;
            }

            if (mCurrentView != null && mCurrentView instanceof ODKView) {
                if(((ODKView) mCurrentView).isTableView())
                    doSwipe = false;
                if(getSlidingMenu().isMenuShowing()||getSlidingMenu().isSecondaryMenuShowing())
                    doSwipe = true;
            }
        }
        if (doSwipe) {
            // Looks for user swipes. If the user has swiped, move to the
            // appropriate screen.

            // for all screens a swipe is left/right of at least
            // .25" and up/down of less than .25"
            // OR left/right of > .5"

            int xpixellimit = (int) (dm.xdpi * .25);
            int ypixellimit = (int) (dm.ydpi * .25);

            if (mCurrentView != null && mCurrentView instanceof ODKView) {
                if (((ODKView) mCurrentView).suppressFlingGesture(e1, e2,
                        velocityX, velocityY)) {
                    return false;
                }
            }

            if (mBeenSwiped) {
                return false;
            }

            if ((Math.abs(e1.getX() - e2.getX()) > xpixellimit && Math.abs(e1
                    .getY() - e2.getY()) < ypixellimit)
                    || Math.abs(e1.getX() - e2.getX()) > xpixellimit * 2) {
                mBeenSwiped = true;

                Log.d("DEBUGCOLL","debugfling velocity: "+velocityX);
                if (velocityX > 0) {
                    if (e1.getX() > e2.getX()) {
                        Timber.e("showNextView VelocityX is bogus! %f > %f", e1.getX(), e2.getX());
                        Collect.getInstance().getActivityLogger()
                                .logInstanceAction(this, "onFling", "showNext");
                        //swipe right to left //close left menu if showing
                        Log.d("DEBUGCOLL","debugfling swipe right to left");
                        if(getSlidingMenu().isMenuShowing()) {
                            toggleLeftMenu();
                            mBeenSwiped = false;
                        }
                        else showNextView(false);
                    } else {
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this, "onFling",
                                        "showPrevious");
                        //swipe left to right //close right menu if showing
                        Log.d("DEBUGCOLL","debugfling swipe left to right, secondMenushowing: "+getSlidingMenu().isSecondaryMenuShowing()+", ismenushowing: "+getSlidingMenu().isMenuShowing());
                        if(getSlidingMenu().isSecondaryMenuShowing()) {
                            toggleRigthMenu();
                            mBeenSwiped = false;
                        }
                        else showPreviousView();
                    }
                } else {
                    if (e1.getX() < e2.getX()) {
                        Timber.e("showPreviousView VelocityX is bogus! %f < %f", e1.getX(), e2.getX());
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this, "onFling",
                                        "showPrevious");
                        //swipe left to right //close right menu if showing
                        Log.d("DEBUGCOLL","debugfling swipe left to right ");
                        if(getSlidingMenu().isSecondaryMenuShowing()) {
                            toggleRigthMenu();
                            mBeenSwiped = false;
                        }
                        else showPreviousView();
                    } else {
                        Collect.getInstance().getActivityLogger()
                                .logInstanceAction(this, "onFling", "showNext");
                        //swipe  right to left //close left menu if showing
                        Log.d("DEBUGCOLL","debugfling swipe right to left, secondMenushowing: "+getSlidingMenu().isSecondaryMenuShowing()+", ismenushowing: "+getSlidingMenu().isMenuShowing());
                        if(getSlidingMenu().isMenuShowing()) {
                            toggleLeftMenu();
                            mBeenSwiped = false;
                        }
                        else showNextView(false);
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        // The onFling() captures the 'up' event so our view thinks it gets long
        // pressed.
        // We don't wnat that, so cancel it.
        if (mCurrentView != null) {
            mCurrentView.cancelLongPress();
        }
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void advance() {
        next();
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

    private void sendSavedBroadcast() {
        Intent i = new Intent();
        i.setAction("org.odk.collect.android.FormSaved");
        this.sendBroadcast(i);
    }

    @Override
    public void onSavePointError(String errorMessage) {
        if (errorMessage != null && errorMessage.trim().length() > 0) {
            ToastUtils.showLongToast(getString(R.string.save_point_error, errorMessage));
        }
    }

    /**
     * Used whenever we need to show empty view and be able to recognize it from the code
     */
    class EmptyView extends View {

        public EmptyView(Context context) {
            super(context);
        }
    }

    private void setEnabled(ImageView img, boolean isEnabled){
        if(isEnabled) img.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
        else img.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray));
    }

    private void toggleLeftMenu(){
        getSlidingMenu().toggle();
    }

    private void setSlidingListener(){
        final Context ctx = this;
        getSlidingMenu().setOnOpenedListener(new SlidingMenu.OnOpenedListener() {
            @Override
            public void onOpened() {
                btnHierarchy.setColorFilter(ContextCompat.getColor(ctx, android.R.color.darker_gray));
            }
        });
        getSlidingMenu().setOnCloseListener(new SlidingMenu.OnCloseListener() {
            @Override
            public void onClose() {
                btnHierarchy.setColorFilter(ContextCompat.getColor(ctx, R.color.colorPrimary));
                btnError.setColorFilter(ContextCompat.getColor(ctx, R.color.colorPrimary));
            }
        });
        getSlidingMenu().setSecondaryOnOpenListner(new SlidingMenu.OnOpenListener() {
            @Override
            public void onOpen() {
                btnError.setColorFilter(ContextCompat.getColor(ctx, android.R.color.darker_gray));
            }
        });
    }


    private void toggleRigthMenu(){
        if(!getSlidingMenu().isSecondaryMenuShowing()) {
            getSlidingMenu().showSecondaryMenu();
        }
        else {
            getSlidingMenu().showContent();
            btnError.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
        }
    }

    private void setChangeListner(QuestionWidget qw) {
        if(qw instanceof StringWidget) addEditTextListener(((StringWidget)qw).getEditText());
        else if(qw instanceof SelectOneWidget) addSelectListener(((SelectOneWidget)qw).getRadioButtons());
        else if(qw instanceof ListMultiWidget) addMultiSelectListener(((ListMultiWidget)qw).getmCheckboxes());
        else if(qw instanceof SelectMultiWidget) addMultiSelectListener(((SelectMultiWidget)qw).getmCheckboxes());
        else if(qw instanceof ItemsetWidget) addSelectListener(((ItemsetWidget)qw).getRadioButtons());
        else if(qw instanceof ExStringWidget) addEditTextListener(((ExStringWidget)qw).getEditText());
        else if(qw instanceof BooleanWidget) addSelectListener(((BooleanWidget)qw).getCheckbox());
        else if(qw instanceof SpinnerWidget) addSelectListener(((SpinnerWidget)qw).getSpinner());
        else if(qw instanceof SelectOneAutoAdvanceWidget) addSelectListener(((SelectOneAutoAdvanceWidget)qw).getRadioButtons());
        else if(qw instanceof ListWidget) addSelectListener(((ListWidget)qw).getRadioButtons());
        else if(qw instanceof SelectOneSearchWidget) addSelectListener(((SelectOneSearchWidget)qw).getRadioButtons());
        else if(qw instanceof TriggerWidget) addSelectListener(((TriggerWidget)qw).getCheckbox());
    }

    private void addSelectListener(ArrayList<RadioButton> radioButtons) {
        Log.d("DEBUGCOLL","radiobuttons size "+radioButtons.size());
        for (RadioButton rb:radioButtons) {
            rb.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                }
            });
        }
    }

    private void addMultiSelectListener(ArrayList<CheckBox> checkboxs) {
        Log.d("DEBUGCOLL","checkbox size "+checkboxs.size());
        for (final CheckBox cb:checkboxs) {
            addSelectListener(cb);
        }
    }

    private void addSelectListener(Spinner sp) {
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
            }
        });
    }

    private void addSelectListener(CheckBox cb) {
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("DEBUGCOLLONCHECK","onchecked : "+isChecked);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
            }
        });
    }

    private void addEditTextListener(final EditText ed){
        ed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("DEBUGCOLLONTYPING","ontype : "+s);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                ed.requestFocus();
            }
        });
    }

}
