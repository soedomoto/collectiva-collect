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

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.odk.collect.android.R;
import org.odk.collect.android.adapters.CollectivaAdapterListInstance;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.collectiva.CollectivaInstances;
import org.odk.collect.android.collectiva.CollectivaPreferences;
import org.odk.collect.android.collectiva.ParseXml;
import org.odk.collect.android.collectiva.Var;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.dto.Instance;
import org.odk.collect.android.listeners.DeleteInstancesListener;
import org.odk.collect.android.listeners.DiskSyncListener;
import org.odk.collect.android.preferences.PreferenceKeys;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.receivers.NetworkReceiver;
import org.odk.collect.android.tasks.DeleteInstancesTask;
import org.odk.collect.android.tasks.InstanceSyncTask;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.PlayServicesUtil;
import org.odk.collect.android.utilities.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_DESC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_DESC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_STATUS_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_STATUS_DESC;

/**
 * Responsible for displaying all the valid instances in the instance directory.
 *
 * @author Muhamad Tohir (maztohir@gmail.com)
 */
public class CollectivaListRespons extends AppCompatActivity implements DiskSyncListener, DeleteInstancesListener, SearchView.OnQueryTextListener {
    private static final String INSTANCE_LIST_ACTIVITY_SORTING_ORDER = "instanceListActivitySortingOrder";

    private InstanceSyncTask instanceSyncTask;
    protected String[] mSortingOptions;
    private Integer mSelectedSortingOrder;

    //modification
    private RecyclerView recycleView;
    private LinearLayout progressBarHolder, messageHolder;
    private TextView messageError;
    private FloatingActionButton fab;
    private CollectivaAdapterListInstance mAdapter;
    private SharedPreferences preferences;

    private static final String FORM_ID_KEY = "formid";
    private static final String FORMNAME = "formname";

    private static final int INSTANCE_UPLOADER = 0;
    String formIDKey = "";

    DeleteInstancesTask mDeleteInstancesTask = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // must be at the beginning of any activity that can be called from an external intent
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), true);
            return;
        }
        setContentView(R.layout.collectiva_activity_list_respons);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("Response of "+getIntent().getStringExtra(FORMNAME));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        formIDKey = getIntent().getStringExtra(FORM_ID_KEY);
        preferences = getSharedPreferences(CollectivaPreferences.COLLECTIVA_PREFERENCES_KEY, MODE_PRIVATE);

        recycleView = (RecyclerView) findViewById(R.id.recycler_view);
        recycleView.setLayoutManager(new LinearLayoutManager(this));
        recycleView.setHasFixedSize(true);

        progressBarHolder = (LinearLayout) findViewById(R.id.holder_progress_bar);
        messageHolder = (LinearLayout) findViewById(R.id.holder_message);
        messageError = (TextView) findViewById(R.id.message_error);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillBlankForm();
            }
        });

        mSortingOptions = new String[]{
                getString(R.string.sort_by_name_asc), getString(R.string.sort_by_name_desc),
                getString(R.string.sort_by_date_asc), getString(R.string.sort_by_date_desc),
                getString(R.string.sort_by_status_asc), getString(R.string.sort_by_status_desc)
        };

        instanceSyncTask = new InstanceSyncTask();
        instanceSyncTask.setDiskSyncListener(this);
        instanceSyncTask.execute();
        onStartLoading();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
            // returns with a form path, start entry
            case INSTANCE_UPLOADER:
                if (intent.getBooleanExtra(FormEntryActivity.KEY_SUCCESS, false)) {
                    //refresh page
                    instanceSyncTask = new InstanceSyncTask();
                    instanceSyncTask.setDiskSyncListener(this);
                    instanceSyncTask.execute();
                    onStartLoading();
                }
                return;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void onStartLoading(){
        Log.d("DEBUGCOLL","on start loading");
        progressBarHolder.setVisibility(View.VISIBLE);
        messageHolder.setVisibility(View.GONE);
    }

    private void onErrorLoading(String message){
        progressBarHolder.setVisibility(View.GONE);
        messageError.setText(message);
        messageHolder.setVisibility(View.VISIBLE);
    }
    private void onDoneLoading(){
        Log.d("DEBUGCOLL","on done loading");
        progressBarHolder.setVisibility(View.GONE);
        messageHolder.setVisibility(View.GONE);
    }
    @Override
    protected void onResume() {
        if (instanceSyncTask != null) {
            instanceSyncTask.setDiskSyncListener(this);
            onStartLoading();
        }
        super.onResume();
        if (instanceSyncTask.getStatus() == AsyncTask.Status.FINISHED) {
            syncComplete(instanceSyncTask.getStatusMessage());
        }

        if (mDeleteInstancesTask != null
                && mDeleteInstancesTask.getStatus() == AsyncTask.Status.FINISHED) {
            deleteComplete(mDeleteInstancesTask.getDeleteCount());
        }

        restoreSelectedSortingOrder();
    }


    @Override
    protected void onPause() {
        if (instanceSyncTask != null) {
            instanceSyncTask.setDiskSyncListener(null);
        }

        if (mDeleteInstancesTask != null) {
            mDeleteInstancesTask.setDeleteListener(null);
        }
        super.onPause();
    }

    @Override
    public void syncComplete(String result) {
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        onDoneLoading();
        setupAdapter();
    }

    public void deleteSelectedInstances() {
        if (mDeleteInstancesTask == null) {
            mDeleteInstancesTask = new DeleteInstancesTask();
            mDeleteInstancesTask.setContentResolver(getContentResolver());
            mDeleteInstancesTask.setDeleteListener(this);
            mDeleteInstancesTask.execute(getCheckedIdObjects());
        } else {
            ToastUtils.showLongToast(R.string.file_delete_in_progress);
        }
    }

    private Long[] getCheckedIdObjects() {
        long[] checkedIds = mAdapter.getSelectedsItemsIds();
        Long[] checkedIdObjects = new Long[checkedIds.length];
        for (int i = 0; i < checkedIds.length; i++) {
            checkedIdObjects[i] = checkedIds[i];
        }
        return checkedIdObjects;
    }


    private void setupAdapter() {
        List<CollectivaInstances> dataInstances = getDataInstance();
        mAdapter = new CollectivaAdapterListInstance(this, dataInstances, getIntent().getStringExtra(FORMNAME));
        recycleView.setAdapter(mAdapter);
        if(dataInstances.size()<1){
            onErrorLoading("Belum ada respons untuk kuestioner \""+getIntent().getStringExtra(FORMNAME)+"\"");
        }
    }

    protected String getSortingOrderKey() {
        return INSTANCE_LIST_ACTIVITY_SORTING_ORDER;
    }

    protected void updateAdapter() {
        mAdapter.setmDataSet(getDataInstance());
    }

    private List<CollectivaInstances> getDataInstance() {
        try {
            String titleSelected = preferences.getString(formIDKey+"_"+CollectivaPreferences.TITLE_RESPONS_KEY, "");
            HashSet<String> subtitles = (HashSet<String>) preferences.getStringSet(formIDKey+"_"+CollectivaPreferences.SUBTITLE_RESPONS_KEY, new HashSet<String>());
            HashSet<String> arrays = new HashSet<>();
            arrays.add(titleSelected);
            arrays.addAll(subtitles);

            Log.d("DEBUGCOLL","getting information for "+arrays);

            List<Instance> instanceList = new InstancesDao().getInstancesFromCursor(new InstancesDao().getInstancesCursor(null, "", null, getSortingOrder()));
            //filtered by form id
            List<CollectivaInstances> newInstance = new ArrayList<>();
            for (Instance instance : instanceList) {
                if (instance.getJrFormId().equals(formIDKey)) {
                    CollectivaInstances clInstances = new CollectivaInstances(instance);

                    if(arrays.size()>0) {
                        HashMap<String, String> informations = ParseXml.getLoadedXmlValues(instance.getInstanceFilePath(), arrays);
                        if(!titleSelected.equals("")) {
                            clInstances.setPrimaryTitle(informations.get(titleSelected));
                            informations.remove(titleSelected);
                        }
                        if(subtitles.size()>0) clInstances.setInformation(informations);
                    }

                    newInstance.add(clInstances);
                }
            }
            return newInstance;
        }catch (Exception e){
            Toast.makeText(this, "Trouble when getting data instance", Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
    }

    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        mAlertDialog.setCancelable(false);

        mAlertDialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        });
        mAlertDialog.show();
    }

    protected String getSortingOrder() {
        String sortingOrder = InstanceColumns.DISPLAY_NAME + " ASC, " + InstanceColumns.STATUS + " DESC";
        switch (getSelectedSortingOrder()) {
            case BY_NAME_ASC:
                sortingOrder = InstanceColumns.DISPLAY_NAME + " ASC, " + InstanceColumns.STATUS + " DESC";
                break;
            case BY_NAME_DESC:
                sortingOrder = InstanceColumns.DISPLAY_NAME + " DESC, " + InstanceColumns.STATUS + " DESC";
                break;
            case BY_DATE_ASC:
                sortingOrder = InstanceColumns.LAST_STATUS_CHANGE_DATE + " ASC";
                break;
            case BY_DATE_DESC:
                sortingOrder = InstanceColumns.LAST_STATUS_CHANGE_DATE + " DESC";
                break;
            case BY_STATUS_ASC:
                sortingOrder = InstanceColumns.STATUS + " ASC, " + InstanceColumns.DISPLAY_NAME + " ASC";
                break;
            case BY_STATUS_DESC:
                sortingOrder = InstanceColumns.STATUS + " DESC, " + InstanceColumns.DISPLAY_NAME + " ASC";
                break;
        }
        return sortingOrder;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Collect.getInstance().getActivityLogger().logInstanceAction(this, "onCreateOptionsMenu", "show");
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_instance, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setMaxWidth(1000);
        searchView.setQueryHint("Search by title...");
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                showDialogSortOptions();
                break;
            case R.id.action_upload:
                if(mAdapter.getTotalUnUploaded(false)>0) uploadInstance(this,mAdapter.getAllIds());
                else Toast.makeText(this, "All respons has been uploaded", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_instance_preferences:
                Intent intent = new Intent(this, CollectivaPreferences.class);
                intent.putExtra(FORM_ID_KEY, formIDKey);
                startActivity(intent);
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private void showDialogSortOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sort");
        builder.setItems(mSortingOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performSelectedSearch(which);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    private void performSelectedSearch(int position) {
        saveSelectedSortingOrder(position);
        updateAdapter();
    }
    private void saveSelectedSortingOrder(int selectedStringOrder) {
        mSelectedSortingOrder = selectedStringOrder;
        PreferenceManager.getDefaultSharedPreferences(Collect.getInstance())
                .edit()
                .putInt(getSortingOrderKey(), selectedStringOrder)
                .apply();
    }

    protected void restoreSelectedSortingOrder() {
        mSelectedSortingOrder = PreferenceManager
                .getDefaultSharedPreferences(Collect.getInstance())
                .getInt(getSortingOrderKey(), BY_NAME_ASC);
    }

    protected int getSelectedSortingOrder() {
        if (mSelectedSortingOrder == null) {
            restoreSelectedSortingOrder();
        }
        return mSelectedSortingOrder;
    }

    private void fillBlankForm(){
        Cursor formslist = new FormsDao().getFormsCursor();
        //getting id
        long itemIdPosition = -1;
        if(formslist.moveToFirst()){
            do {
                if(formslist.getString(formslist.getColumnIndex(FormsProviderAPI.FormsColumns.JR_FORM_ID)).equals(formIDKey)){
                    itemIdPosition = formslist.getLong(formslist.getColumnIndex(FormsProviderAPI.FormsColumns._ID));
                }
            }while (formslist.moveToNext());
        }

        if(itemIdPosition>-1){
            // get uri to form
            Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, itemIdPosition);

            // caller wants to view/edit a form, so launch formentryactivity
            Intent intent = new Intent(Intent.ACTION_EDIT, formUri);
            intent.putExtra("fillblankform",true);
            intent.putExtra(Var.CURRENT_FORMNAME,getIntent().getStringExtra(FORMNAME));
            intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
            startActivity(intent);
        }else {
            //xxxseharusnya tidak terjadi, tidak mungkin itemIdposition < 1
            Toast.makeText(this, "Form not exist", Toast.LENGTH_SHORT).show();
        }
    }

    //button function
    public static void uploadInstance(Activity context, long[] instanceIds){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

        if (NetworkReceiver.running) {
            ToastUtils.showShortToast(R.string.send_in_progress);
        } else if (ni == null || !ni.isConnected()) {
            Toast.makeText(context, "No Network Connection Available", Toast.LENGTH_SHORT).show();
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String server = prefs.getString(PreferenceKeys.KEY_PROTOCOL, null);
            if (server.equalsIgnoreCase(context.getString(R.string.protocol_google_sheets))) {
                // if it's Sheets, start the Sheets uploader
                // first make sure we have a google account selected

                if (PlayServicesUtil.isGooglePlayServicesAvailable(context)) {
                    Intent i = new Intent(context, GoogleSheetsUploaderActivity.class);
                    i.putExtra(FormEntryActivity.KEY_INSTANCES, instanceIds);
                    context.startActivityForResult(i, INSTANCE_UPLOADER);
                } else {
                    PlayServicesUtil.showGooglePlayServicesAvailabilityErrorDialog(context);
                }
            } else {
                // otherwise, do the normal aggregate/other thing.
                Intent i = new Intent(context, InstanceUploaderActivity.class);
                i.putExtra(FormEntryActivity.KEY_INSTANCES, instanceIds);
                context.startActivityForResult(i, INSTANCE_UPLOADER);
            }

        }
    }

    @Override
    public void deleteComplete(int deletedInstances) {
        updateAdapter();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        this.mAdapter.getFilter().filter(newText);
        return true;
    }
}
