package org.odk.collect.android.adapters;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.CollectivaListRespons;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.collectiva.CollectivaInstanceOptionMenuCallback;
import org.odk.collect.android.collectiva.CollectivaInstances;
import org.odk.collect.android.collectiva.Var;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.dto.Instance;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.utilities.ApplicationConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.app.Activity.RESULT_OK;

/**
 * Created by lenovo on 5/17/2017.
 */

public class CollectivaAdapterListInstance extends RecyclerView.Adapter<CollectivaAdapterListInstance.ViewHolder> implements Filterable{
    private Activity context;
    private List<CollectivaInstances> mDataSet;
    private List<CollectivaInstances> mDataSetOri;
    private ActionMode actionMode;
    private String formname = "";
    private SparseBooleanArray selectedItems = new SparseBooleanArray();
    private CollectivaAdapterListInstance itsMe;

    public CollectivaAdapterListInstance(Activity activity, List<CollectivaInstances> cursors, String formname){
        this.context = activity;
        this.mDataSet = cursors;
        this.mDataSetOri = cursors;
        itsMe = this;
        actionMode = null;
        this.formname = formname;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.collectiva_item_instance, null);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CollectivaInstances clInstance = mDataSet.get(position);
        Instance instance =clInstance.getInstances();

        holder.title.setText(instance.getDisplayName());
        holder.subtitle.setText(instance.getDisplaySubtext());

        if(clInstance.getPrimaryTitle()!=null && !clInstance.getPrimaryTitle().equals("")){
            holder.title.setText(clInstance.getPrimaryTitle());
        }

        if(clInstance.getInformation()!=null && clInstance.getInformation().size()>0){
            holder.divider.setVisibility(View.VISIBLE);
            setDescription(holder, clInstance.getInformation());
        }else {
            holder.divider.setVisibility(View.GONE);
            holder.holderDescription.removeAllViews();
            holder.holderDescription.setVisibility(View.GONE);
        }

        holder.iconInstance.setBackgroundResource(R.drawable.cl_cicle_bg_trans_stroke_accent);
        String status = instance.getStatus();

        if(status.equals(InstanceProviderAPI.STATUS_COMPLETE) ||
                status.equals(InstanceProviderAPI.STATUS_SUBMISSION_FAILED) ||
                status.equals(InstanceProviderAPI.STATUS_INCOMPLETE)){
            setVisible(true, new View[]{holder.btnEdit});
            setVisible(false, new View[]{holder.statusHolder});
        }else if(status.equals(InstanceProviderAPI.STATUS_SUBMITTED)){
            setVisible(true, new View[]{holder.statusHolder});
            setVisible(false, new View[]{holder.btnEdit});
            setStatus(holder, R.drawable.cl_round_bggreensoft, R.drawable.ic_check, "Uploaded", R.drawable.cl_cicle_bg_trans_stroke_greensoft);
        }

        if(selectedItems.get(position)){
            setSelected(holder);
        }else {
            holder.selection.setVisibility(View.GONE);
            holder.iconSelected.setVisibility(View.GONE);
            holder.btnEdit.setSelected(true);
            holder.cardView.setSelected(true);
        }

        Long dateDeleted = instance.getDeletedDate();
        String formId = instance.getJrFormId();
        Cursor cursor = new FormsDao().getFormsCursorForFormId(formId);

        boolean formExists = false;
        boolean isFormEncrypted = false;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int base64RSAPublicKeyColumnIndex = cursor.getColumnIndex(FormsProviderAPI.FormsColumns.BASE64_RSA_PUBLIC_KEY);
                    String base64RSAPublicKey = cursor.getString(base64RSAPublicKeyColumnIndex);
                    isFormEncrypted = base64RSAPublicKey != null && !base64RSAPublicKey.isEmpty();
                    formExists = true;
                }
            } finally {
                cursor.close();
            }
        }

        if (dateDeleted != 0 || !formExists || isFormEncrypted) {
            String cause  = "";
            if (dateDeleted != 0) cause = new SimpleDateFormat(context.getString(R.string.deleted_on_date_at_time),
                    Locale.getDefault()).format(new Date(dateDeleted));
            else if (!formExists)  cause = context.getString(R.string.deleted_form);
            else cause = context.getString(R.string.encrypted_form);

            setStatus(holder, R.drawable.cl_round_bgdarkgray, R.drawable.ic_close,
                    cause, R.drawable.cl_cicle_bg_trans_stroke_darkgray);
        }
    }

    private void setDescription(ViewHolder holder, HashMap<String, String> information) {

        Set<String> keySet = information.keySet();
        holder.holderDescription.removeAllViews();
        for (String key:keySet){
            LinearLayout ln = new LinearLayout(context);
            ln.setOrientation(LinearLayout.HORIZONTAL);
            ln.setGravity(Gravity.CENTER_VERTICAL);
            ln.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView txtKey = new TextView(context);
            txtKey.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1f));
            txtKey.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            txtKey.setText("- "+key);
            setPadding(txtKey,4);
            txtKey.setTextColor(ContextCompat.getColor(context,R.color.black));
            ln.addView(txtKey);

            TextView txtVal = new TextView(context);
            txtVal.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,2f));
            txtVal.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            txtVal.setText(" : "+information.get(key));
            setPadding(txtVal,4);
            ln.addView(txtVal);

            holder.holderDescription.addView(ln);
        }
    }

    private void setPadding(View v, int dp){
        final float scale = context.getResources().getDisplayMetrics().density;
        int px = (int) (dp * scale + 0.5f);
        v.setPadding(px,0,px,0);
    }


    private void setStatus(ViewHolder holder, int bgStatusHolder, int iconStatus, String statusMsg, int bgIconInstance){
        holder.statusHolder.setVisibility(View.VISIBLE);
        holder.statusHolder.setBackgroundResource(bgStatusHolder);
        holder.iconStatus.setImageResource(iconStatus);
        holder.status.setText(statusMsg);
        holder.iconInstance.setBackgroundResource(bgIconInstance);
    }

    private void setVisible(boolean isVisible, View[] views){
        if(isVisible){
            for (View v:views) {
                v.setVisibility(View.VISIBLE);
            }
        }else {
            for (View v:views) {
                v.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public void setmDataSet(List<CollectivaInstances> mDataSet) {
        this.mDataSet = mDataSet;
        if(actionMode != null) actionMode.finish();
        notifyDataSetChanged();
    }

    public long[] getAllIds() {
        long[] ids = new long[getItemCount()];
        for (int i = 0; i<getItemCount(); i++) {
            ids[i] = mDataSet.get(i).getInstances().getmId();
        }
        return ids;
    }

    public void deleteSelectedItems() {
        ((CollectivaListRespons) context).deleteSelectedInstances();
    }

    @Override
    public Filter getFilter() {
        return new ValueFilter();
    }


    class ValueFilter extends Filter{
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint.toString().trim().equals("")) {
                CollectivaAdapterListInstance.this.mDataSet = CollectivaAdapterListInstance.this.mDataSetOri;
            } else {
                CollectivaAdapterListInstance.this.mDataSet = new ArrayList();
                for (CollectivaInstances instanc :CollectivaAdapterListInstance.this.mDataSetOri) {
                    if(instanc.getPrimaryTitle().toLowerCase().contains(constraint)){
                        CollectivaAdapterListInstance.this.mDataSet.add(instanc);
                    }
                }
            }
            FilterResults res = new FilterResults();
            res.count = CollectivaAdapterListInstance.this.mDataSet.size();
            res.values = CollectivaAdapterListInstance.this.mDataSet;
            return res;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            CollectivaAdapterListInstance.this.mDataSet = (List<CollectivaInstances>) results.values;
            CollectivaAdapterListInstance.this.notifyDataSetChanged();
        }
    }



    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView title, subtitle, status;
        ImageView iconInstance, iconStatus, iconSelected;
        LinearLayout statusHolder, holderDescription;
        ImageView selection, btnEdit;
        CardView cardView;
        View divider;

        ViewHolder(final View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            status = (TextView) itemView.findViewById(R.id.status);
            iconStatus = (ImageView) itemView.findViewById(R.id.icon_status);
            iconInstance = (ImageView) itemView.findViewById(R.id.icon_instance);
            btnEdit = (ImageView) itemView.findViewById(R.id.button_edit);
            statusHolder = (LinearLayout) itemView.findViewById(R.id.status_holder);
            selection = (ImageView) itemView.findViewById(R.id.selection);
            iconSelected  = (ImageView) itemView.findViewById(R.id.icon_selected);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            divider = itemView.findViewById(R.id.divider);
            holderDescription = (LinearLayout) itemView.findViewById(R.id.holder_description);

            btnEdit.setOnClickListener(this);
            cardView.setOnClickListener(this);
            cardView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v==cardView) {
                if(actionMode!=null){
                    toggleSelected(getLayoutPosition());
                }else {
                    Instance c = mDataSet.get(getLayoutPosition()).getInstances();
                    Uri instanceUri = ContentUris.withAppendedId(InstanceProviderAPI.InstanceColumns.CONTENT_URI,
                            c.getmId());

                    String action = context.getIntent().getAction();
                    if (Intent.ACTION_PICK.equals(action)) {
                        // caller is waiting on a picked form
                        context.setResult(RESULT_OK, new Intent().setData(instanceUri));
                    } else {
                        // caller wants to view a form, so launch hierrarchy
                        Intent intent = new Intent(Intent.ACTION_EDIT, instanceUri);
                        intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.VIEW_SENT);
                        intent.putExtra(Var.INSTANCE_ID, c.getmId());
                        intent.putExtra(Var.CURRENT_FORMNAME,formname);
                        intent.putExtra(Var.CURRENT_INSTANCE_TITLE, mDataSet.get(getLayoutPosition()).getPrimaryTitle());
                        intent.putExtra(Var.ISCANEDIT,
                                !mDataSet.get(getLayoutPosition()).getInstances().getStatus()
                                        .equals(InstanceProviderAPI.STATUS_SUBMITTED));
                        context.startActivity(intent);
                    }
                }
            }else if(v==btnEdit){
                if(actionMode==null) {
                    Instance c = mDataSet.get(getLayoutPosition()).getInstances();
                    Uri instanceUri = ContentUris.withAppendedId(InstanceProviderAPI.InstanceColumns.CONTENT_URI,
                            c.getmId());

                    // caller wants to edit a form, so launch formentryactivity
                    Intent intent = new Intent(Intent.ACTION_EDIT, instanceUri);
                    intent.putExtra(Var.CURRENT_FORMNAME,formname);
                    intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
                    context.startActivity(intent);
                }else {
                    toggleSelected(getLayoutPosition());
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            toggleSelected(getLayoutPosition());
            return true;
        }
    }


    private void toggleSelected(int layoutPosition){
        if(selectedItems.get(layoutPosition)){
            selectedItems.delete(layoutPosition);
        }else {
            selectedItems.put(layoutPosition, true);
        }
        if(getSelectedItemsCount()>0 && actionMode==null){
            actionMode = ((CollectivaListRespons) context).startSupportActionMode(new CollectivaInstanceOptionMenuCallback(context,itsMe));
        }else if (getSelectedItemsCount()==0 && actionMode!=null){
            actionMode.finish();
        }
        if(actionMode!=null){
            actionMode.setTitle(getSelectedItemsCount()+" selected");
        }
        notifyDataSetChanged();
    }

    public void setNullToActionMode() {
        if (actionMode != null)
            actionMode = null;
    }

    private void setSelected(ViewHolder holder){
        holder.selection.setVisibility(View.VISIBLE);
        holder.iconSelected.setVisibility(View.VISIBLE);
        holder.btnEdit.setSelected(false);
        holder.cardView.setSelected(false);
    }

    public void removeSelected(){
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemsCount(){
        return selectedItems==null?0:selectedItems.size();
    }

    public long[] getSelectedsItemsIds(){
        long[] ids = new long[getSelectedItemsCount()];
        int i = 0;
        for (int position = 0; position<getItemCount(); position++) {
            if(selectedItems.get(position)){
                ids[i] = mDataSet.get(position).getInstances().getmId();
                i++;
            }
        }
        return ids;
    }

    public void uploadedSelectedItems(){
        if(getTotalUnUploaded(true)>0){
            CollectivaListRespons.uploadInstance(context, getSelectedsItemsIds());
        }else Toast.makeText(context, "All response has been uploaded", Toast.LENGTH_SHORT).show();
    }

    public int getTotalUnUploaded(boolean isSelectedOnly){
        int total = 0;
        for (int position = 0; position<mDataSet.size();position++) {
            Instance c = mDataSet.get(position).getInstances();
            if(!c.getStatus().equals(InstanceProviderAPI.STATUS_SUBMITTED)){
                if(isSelectedOnly){
                    if(selectedItems.get(position)) total++;
                }else {
                    total++;
                }
            }
        }
        return total;
    }

}
