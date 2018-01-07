package org.odk.collect.android.activities;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.odk.collect.android.R;
import org.odk.collect.android.adapters.CollectivaAdapterListForm;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.collectiva.CollectivaMenuMainActivity;
import org.odk.collect.android.collectiva.CollectivaPreferences;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.listeners.FormDownloaderListener;
import org.odk.collect.android.listeners.FormListDownloaderListener;
import org.odk.collect.android.logic.FormDetails;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.tasks.DownloadFormListTask;
import org.odk.collect.android.tasks.DownloadFormsTask;
import org.odk.collect.android.utilities.AuthDialogUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import timber.log.Timber;

import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_DESC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_DESC;

/**
 * Created by lenovo on 5/17/2017.
 */

public class CollectivaListForms extends Fragment implements FormListDownloaderListener,
        FormDownloaderListener, AuthDialogUtility.AuthDialogUtilityResultListener , SearchView.OnQueryTextListener {

    private static final String FORM_DOWNLOAD_LIST_SORTING_ORDER = "formDownloadListSortingOrder";

    private static final int PROGRESS_DIALOG = 1;
    private static final int AUTH_DIALOG = 2;

    private static final String BUNDLE_FORM_MAP = "formmap";
    private static final String DIALOG_TITLE = "dialogtitle";
    private static final String DIALOG_MSG = "dialogmsg";
    private static final String DIALOG_SHOWING = "dialogshowing";
    private static final String FORMLIST = "formlist";

    private static final String FORMNAME = "formname";
    private static final String FORMDETAIL_KEY = "formdetailkey";
    private static final String FORMID_DISPLAY = "formiddisplay";

    private static final String FORM_ID_KEY = "formid";
    private static final String FORM_VERSION_KEY = "formversion";

    private String mAlertMsg;
    private boolean mAlertShowing = false;
    private String mAlertTitle;

    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;

    private DownloadFormListTask mDownloadFormListTask;
    private DownloadFormsTask mDownloadFormsTask;

    private HashMap<String, FormDetails> mFormNamesAndURLs = new HashMap<String, FormDetails>();
    private ArrayList<HashMap<String, String>> mFormList = new ArrayList<>();
    private ArrayList<HashMap<String, String>> mFilteredFormList = new ArrayList<>();

    private static final boolean EXIT = true;
    private static final boolean DO_NOT_EXIT = false;
    private boolean mShouldExit;
    private static final String SHOULD_EXIT = "shouldexit";

    protected String[] mSortingOptions;

    private Integer mSelectedSortingOrder;

    //modification
    private RecyclerView recycleView;
    private LinearLayout progressBarHolder, messageHolder;
    private TextView messageError;
    private CollectivaAdapterListForm mAdapter;
    private static final String HAS_BEEN_DOWNLOADED ="hasbeendownloaded";

    private SharedPreferences sharedPreferences;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.collectiva_fragment_list_forms, container, false);
        mAlertMsg = getString(R.string.please_wait);
        recycleView = (RecyclerView) v.findViewById(R.id.recycler_view);
        progressBarHolder = (LinearLayout) v.findViewById(R.id.holder_progress_bar);
        messageHolder = (LinearLayout) v.findViewById(R.id.holder_message);
        messageError = (TextView) v.findViewById(R.id.message);
        setHasOptionsMenu(true);


        sharedPreferences = getActivity().getSharedPreferences(CollectivaPreferences.COLLECTIVA_PREFERENCES_KEY,Context.MODE_PRIVATE);

        recycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        recycleView.setHasFixedSize(true);
        mAdapter = new CollectivaAdapterListForm(getActivity(), this);
        recycleView.setAdapter(mAdapter);

        mSortingOptions = new String[]{
                getString(R.string.sort_by_name_asc), getString(R.string.sort_by_name_desc)
        };

        restoreSelectedSortingOrder();
        setUpDownloadedFormsOffline();

        if (mFormNamesAndURLs.isEmpty()) {
            // first time, so get the formlist
            onRefresPage();
        }

        return v;
    }

    private void setUpDownloadedFormsOffline() {
        ArrayList<HashMap<String, String>> downlaodedFormsBefore = new ArrayList<>();
        Cursor c = new FormsDao().getFormsCursor(getSortingOrder());
        if(c.moveToFirst()){
            do{
                HashMap<String, String> item = new HashMap<>();
                String formName = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.DISPLAY_NAME));
                String formVersion = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.JR_FORM_ID));
                String formId = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.JR_FORM_ID));

                item.put(FORMNAME, formName);
                item.put(FORM_ID_KEY, formId);
                item.put(FORM_VERSION_KEY, formVersion);
                item.put(HAS_BEEN_DOWNLOADED, "true");
                downlaodedFormsBefore.add(item);

            }while (c.moveToNext());

            mFilteredFormList = downlaodedFormsBefore;
            updateAdapter();
        }
    }


    private void onRefresPage(){
        Collect.getInstance().getActivityLogger().logAction(this, "refreshForms", "");

        mFilteredFormList.clear();
        downloadFormList();
    }

    private void onStartLoading(){
        progressBarHolder.setVisibility(View.VISIBLE);
        messageHolder.setVisibility(View.GONE);
    }

    private void onErrorLoading(String message){
        progressBarHolder.setVisibility(View.GONE);
        messageError.setText(message);
        messageHolder.setVisibility(View.VISIBLE);
    }
    private void onDoneLoading(){
        progressBarHolder.setVisibility(View.GONE);
        messageHolder.setVisibility(View.GONE);
    }

    /**
     * Starts the download task and shows the progress dialog.
     */
    private void downloadFormList() {
        Log.d("DEBUGCOLL","start downloading form");
        onStartLoading();

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

        if (ni == null || !ni.isConnected()) {
            onErrorLoading(getString(R.string.no_connection));
//            ToastUtils.showShortToast(R.string.no_connection);
        } else {
            mFormNamesAndURLs = new HashMap<String, FormDetails>();
            if (mProgressDialog != null) {
                // This is needed because onPrepareDialog() is broken in 1.6.
                mProgressDialog.setMessage(getString(R.string.please_wait));
            }
//            showDialog(PROGRESS_DIALOG);

            if (mDownloadFormListTask != null
                    && mDownloadFormListTask.getStatus() != AsyncTask.Status.FINISHED) {
                return; // we are already doing the download!!!
            } else if (mDownloadFormListTask != null) {
                mDownloadFormListTask.setDownloaderListener(null);
                mDownloadFormListTask.cancel(true);
                mDownloadFormListTask = null;
            }

            mDownloadFormListTask = new DownloadFormListTask();
            mDownloadFormListTask.setDownloaderListener(this);
            mDownloadFormListTask.execute();

        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_forms, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setMaxWidth(1000);
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preferences:
                startActivity(new Intent(getActivity(), PreferencesActivity.class));
//                startActivity(new Intent(getActivity(), CollectivaPreferences.class));
                break;
            case R.id.action_sort:
                //show dialog sort
                showDialogSortOptions();
                break;
            case R.id.action_refresh:
                Toast.makeText(getContext(), "Refreshing..", Toast.LENGTH_SHORT).show();
                onRefresPage();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDialogSortOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Sort Options");
        builder.setItems(mSortingOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performSelectedSearch(which);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void createDialog(int id){
        switch (id) {
            case PROGRESS_DIALOG:
                Collect.getInstance().getActivityLogger().logAction(this,
                        "onCreateDialog.PROGRESS_DIALOG", "show");
                mProgressDialog = new ProgressDialog(getContext());
                DialogInterface.OnClickListener loadingButtonListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Collect.getInstance().getActivityLogger().logAction(this,
                                        "onCreateDialog.PROGRESS_DIALOG", "OK");
                                dialog.dismiss();
                                // we use the same progress dialog for both
                                // so whatever isn't null is running
                                if (mDownloadFormListTask != null) {
                                    mDownloadFormListTask.setDownloaderListener(null);
                                    mDownloadFormListTask.cancel(true);
                                    mDownloadFormListTask = null;
                                }
                                if (mDownloadFormsTask != null) {
                                    mDownloadFormsTask.setDownloaderListener(null);
                                    mDownloadFormsTask.cancel(true);
                                    mDownloadFormsTask = null;
                                }
                            }
                        };
                mProgressDialog.setTitle(getString(R.string.downloading_data));
                mProgressDialog.setMessage(mAlertMsg);
//                mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                mProgressDialog.show();
            case AUTH_DIALOG:
                Collect.getInstance().getActivityLogger().logAction(this,
                        "onCreateDialog.AUTH_DIALOG", "show");

                mAlertShowing = false;
                new AuthDialogUtility().createDialog(getContext(), this);
        }
    }

    protected String getSortingOrderKey() {
        return FORM_DOWNLOAD_LIST_SORTING_ORDER;
    }

    protected void updateAdapter() {
        sortList();
        dividedFormByStatusDownload();
    }

    public void triggerActiveSurvey(){
        Log.d("DEBUGCOLL","update adapter called size "+mFilteredFormList.size());
        if(mFilteredFormList.size()==0)  downloadFormList();
        else {
            updateAdapter();
        }
    }

    private void sortList() {
        Collections.sort(mFilteredFormList, new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> lhs, HashMap<String, String> rhs) {
                if (getSortingOrder().equals(FormsProviderAPI.FormsColumns.DISPLAY_NAME + " ASC")) {
                    return lhs.get(FORMNAME).compareToIgnoreCase(rhs.get(FORMNAME));
                } else {
                    return rhs.get(FORMNAME).compareToIgnoreCase(lhs.get(FORMNAME));
                }
            }
        });
    }


    /**
     * Determines if a local form on the device is superseded by a given version (of the same form
     * presumably available
     * on the server).
     *
     * @param formId        the form to be checked. A form with this ID may or may not reside on the
     *                      local device.
     * @param latestVersion the version against which the local form (if any) is tested.
     * @return true if a form with id <code>formId</code> exists on the local device and its version
     * is less than
     * <code>latestVersion</code>.
     */
    public static boolean isLocalFormSuperseded(String formId, String latestVersion) {

        if (formId == null) {
            Timber.e("isLocalFormSuperseded: server is not OpenRosa-compliant. <formID> is null!");
            return true;
        }

        Cursor formCursor = null;
        try {
            formCursor = new FormsDao().getFormsCursorForFormId(formId);
            if (formCursor.getCount() == 0) {
                // form does not already exist locally
                return true;
            }
            formCursor.moveToFirst();
            int idxJrVersion = formCursor.getColumnIndex(FormsProviderAPI.FormsColumns.JR_VERSION);
            if (formCursor.isNull(idxJrVersion)) {
                // any non-null version on server is newer
                return (latestVersion != null);
            }
            String jrVersion = formCursor.getString(idxJrVersion);
            // apparently, the isNull() predicate above is not respected on all Android OSes???
            if (jrVersion == null && latestVersion == null) {
                return false;
            }
            if (jrVersion == null) {
                return true;
            }
            if (latestVersion == null) {
                return false;
            }
            // if what we have is less, then the server is newer
            return (jrVersion.compareTo(latestVersion) < 0);
        } finally {
            if (formCursor != null) {
                formCursor.close();
            }
        }
    }

    /**
     * Causes any local forms that have been updated on the server to become checked in the list.
     * This is a prompt and a
     * convenience to users to download the latest version of those forms from the server.
     */
    private void dividedFormByStatusDownload() {
        ArrayList<HashMap<String, String>> downloadedFilteredFormList = new ArrayList<>();
        ArrayList<HashMap<String, String>> unDownloadedFilteredFormList = new ArrayList<>();

        for (int idx = 0; idx < mFilteredFormList.size(); idx++) {
            HashMap<String, String> item = mFilteredFormList.get(idx);
            if(item.get(HAS_BEEN_DOWNLOADED)!=null && item.get(HAS_BEEN_DOWNLOADED).equals("true")) {
                if(isFormInActiveSurvey(item)) downloadedFilteredFormList.add(item);
            }else {
                if (isLocalFormSuperseded(item.get(FORM_ID_KEY), item.get(FORM_VERSION_KEY))) {
                    //need to downloaded
                    item.put(HAS_BEEN_DOWNLOADED, "false");
                    if(isFormInActiveSurvey(item)) unDownloadedFilteredFormList.add(item);
                } else {
                    //has been downloaded
                    item.put(HAS_BEEN_DOWNLOADED, "true");
                    if(isFormInActiveSurvey(item)) downloadedFilteredFormList.add(item);
                }
            }
        }

        downloadedFilteredFormList.addAll(unDownloadedFilteredFormList);

        mAdapter.setmDataSet(downloadedFilteredFormList);
        if(downloadedFilteredFormList.size()==0){
            //show that there is no form
            onErrorLoading("There is no form for you");
        }else {
            messageHolder.setVisibility(View.GONE);
        }
    }
    private boolean isFormInActiveSurvey(HashMap<String, String> item) {
//        String activeSurvey = sharedPreferences.getString(CollectivaMenuMainActivity.ACTIVE_SURVEY_KEY,"");
//        if(item.get(FORMNAME).toLowerCase().contains(activeSurvey.toLowerCase())){
//            return true;
//        }
//        return false;
        return true;
    }

    /**
     * Called when the form list has finished downloading. results will either contain a set of
     * <formname, formdetails> tuples, or one tuple of DL.ERROR.MSG and the associated message.
     */
    public void formListDownloadingComplete(HashMap<String, FormDetails> result) {
        mDownloadFormListTask.setDownloaderListener(null);
        mDownloadFormListTask = null;

        if (result == null) {
            Timber.e("Formlist Downloading returned null.  That shouldn't happen");
            Timber.e("Formlist Downloading returned null.  That shouldn't happen");
            onErrorLoading("Error occured");
            return;
        }

        if (result.containsKey(DownloadFormListTask.DL_AUTH_REQUIRED)) {
            // need authorization
            onErrorLoading("Need authentification");
            createDialog(AUTH_DIALOG);
        } else if (result.containsKey(DownloadFormListTask.DL_ERROR_MSG)) {
            onErrorLoading("Error getting form list");
        } else {
            // Everything worked. Clear the list and add the results.
            mFormNamesAndURLs = result;
            mFormList.clear();
            onDoneLoading();

            ArrayList<String> ids = new ArrayList<String>(mFormNamesAndURLs.keySet());
            for (int i = 0; i < result.size(); i++) {
                String formDetailsKey = ids.get(i);
                FormDetails details = mFormNamesAndURLs.get(formDetailsKey);
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(FORMNAME, details.formName);
                item.put(FORMDETAIL_KEY, formDetailsKey);
                item.put(FORM_ID_KEY, details.formID);
                item.put(FORM_VERSION_KEY, details.formVersion);

                if(!mFormListContain(details.formName)){
                    // Insert the new form in alphabetical order.
                    if (mFormList.size() == 0) {
                        mFormList.add(item);
                    } else {
                        int j;
                        for (j = 0; j < mFormList.size(); j++) {
                            HashMap<String, String> compareMe = mFormList.get(j);
                            String name = compareMe.get(FORMNAME);
                            if (name.compareTo(mFormNamesAndURLs.get(ids.get(i)).formName) > 0) {
                                break;
                            }
                        }
                        mFormList.add(j, item);
                    }
                }
            }
            //if download success, certain that adapter is reset
//            mFilteredFormList.clear();
            mFilteredFormList.addAll(mFormList);
            updateAdapter();
        }
    }

    private boolean mFormListContain(String formName) {
        for (HashMap<String, String> has:mFilteredFormList) {
            if(has.get(FORMNAME).equals(formName)){
                return true;
            }
        }
        return false;
    }


    /**
     * Creates an alert dialog with the given title and message. If shouldExit is set to true, the
     * activity will exit when the user clicks "ok".
     */
    private void createAlertDialog(String title, String message, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger().logAction(this, "createAlertDialog", "show");
        mAlertDialog = new AlertDialog.Builder(getContext()).create();
        mAlertDialog.setTitle(title);
        mAlertDialog.setMessage(message);
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE: // ok
                        Collect.getInstance().getActivityLogger().logAction(this,
                                "createAlertDialog", "OK");
                        // just close the dialog
                        mAlertShowing = false;
                        // successful download, so quit
                        if (shouldExit) {
                            getActivity().finish();
                        }
                        break;
                }
            }
        });
        mAlertMsg = message;
        mAlertTitle = title;
        mAlertShowing = true;
        mShouldExit = shouldExit;
        mAlertDialog.show();
    }


    @Override
    public void progressUpdate(String currentFile, int progress, int total) {
        mAlertMsg = getString(R.string.fetching_file, currentFile, String.valueOf(progress), String.valueOf(total));
        mProgressDialog.setMessage(mAlertMsg);
    }


    @Override
    public void formsDownloadingComplete(HashMap<FormDetails, String> result) {
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(null);
        }

        if (mProgressDialog.isShowing()) {
            // should always be true here
            mProgressDialog.dismiss();
        }

        Set<FormDetails> keys = result.keySet();
        StringBuilder b = new StringBuilder();
        for (FormDetails k : keys) {
            b.append(k.formName + " ("
                    + ((k.formVersion != null)
                    ? (this.getString(R.string.version) + ": " + k.formVersion + " ")
                    : "") + "ID: " + k.formID + ") - " + result.get(k));
            b.append("\n\n");
        }
        //update form status downloaded or nay
        updateAdapter();
        createAlertDialog(getString(R.string.download_forms_result), b.toString().trim(), DO_NOT_EXIT);
    }

    @Override
    public void updatedCredentials() {
        downloadFormList();
    }

    @Override
    public void cancelledUpdatingCredentials() {
        getActivity().finish();
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

    protected String getSortingOrder() {
        String sortingOrder = FormsProviderAPI.FormsColumns.DISPLAY_NAME + " ASC";
        switch (getSelectedSortingOrder()) {
            case BY_NAME_ASC:
                sortingOrder = FormsProviderAPI.FormsColumns.DISPLAY_NAME + " ASC";
                break;
            case BY_NAME_DESC:
                sortingOrder = FormsProviderAPI.FormsColumns.DISPLAY_NAME + " DESC";
                break;
            case BY_DATE_ASC:
                sortingOrder = FormsProviderAPI.FormsColumns.DATE + " ASC";
                break;
            case BY_DATE_DESC:
                sortingOrder = FormsProviderAPI.FormsColumns.DATE + " DESC";
                break;
        }
        return sortingOrder;
    }

    /**
     * starts the task to download the selected forms, also shows progress dialog
     */
    public void downloadForm(HashMap<String, String> item) {
        ArrayList<FormDetails> filesToDownload = new ArrayList<FormDetails>();
        filesToDownload.add(mFormNamesAndURLs.get(item.get(FORMDETAIL_KEY)));

        Collect.getInstance().getActivityLogger().logAction(this, "downloadSelectedFiles",
                Integer.toString(filesToDownload.size()));

        // show dialog box
        createDialog(PROGRESS_DIALOG);

        mDownloadFormsTask = new DownloadFormsTask();
        mDownloadFormsTask.setDownloaderListener(this);
        mDownloadFormsTask.execute(filesToDownload);
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
