package org.odk.collect.android.collectiva;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import org.odk.collect.android.R;
import org.odk.collect.android.adapters.CollectivaAdapterListInstance;

/**
 * Created by maztohir on 5/31/2017.
 */

public class CollectivaInstanceOptionMenuCallback implements ActionMode.Callback {

    private CollectivaAdapterListInstance mAdapter;
    private Context mContext;

    public CollectivaInstanceOptionMenuCallback(Context context, CollectivaAdapterListInstance mAdapter){
        this.mContext = context;
        this.mAdapter = mAdapter;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_instance_option, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.findItem(R.id.action_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.action_upload).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setMessage("Are you sure delete this selected respons?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAdapter.deleteSelectedItems();
                    }
                });
                builder.setNegativeButton("No", null);
                builder.show();
                break;
            case R.id.action_upload:
                mAdapter.uploadedSelectedItems();
                break;
        }

        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        //remove selection && and set null action mode
        mAdapter.removeSelected();
        mAdapter.setNullToActionMode();
    }
}
