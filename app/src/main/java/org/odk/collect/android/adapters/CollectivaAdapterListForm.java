package org.odk.collect.android.adapters;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
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
import org.odk.collect.android.activities.CollectivaListForms;
import org.odk.collect.android.activities.CollectivaListRespons;
import org.odk.collect.android.collectiva.Var;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.utilities.ApplicationConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by lenovo on 5/17/2017.
 */

public class CollectivaAdapterListForm extends RecyclerView.Adapter<CollectivaAdapterListForm.ViewHolder> implements Filterable{
    private Activity context;
    private Fragment fragment;
    private ArrayList<HashMap<String, String>> mDataSet;
    private ArrayList<HashMap<String, String>> mDataSetOri;

    private static final String FORMNAME = "formname";
    private static final String FORM_ID_KEY = "formid";
    private static final String FORM_VERSION_KEY = "formversion";

    private static final String HAS_BEEN_DOWNLOADED ="hasbeendownloaded";
    private int imagePatternDrawables[] = new int[]{R.drawable.pattern1, R.drawable.pattern2, R.drawable.pattern3, R.drawable.pattern4,
            R.drawable.pattern5, R.drawable.pattern6, R.drawable.pattern7};

    public CollectivaAdapterListForm(Activity activity, Fragment fragment){
        this.context = activity;
        mDataSet = new ArrayList<>();
        mDataSetOri = mDataSet;
        this.fragment = fragment;
    }


    public void setmDataSet(ArrayList<HashMap<String, String>> data){
        this.mDataSet = data;
        this.mDataSetOri = data;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.collectiva_item_form, null);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final HashMap<String, String> dataSet = mDataSet.get(position);
        holder.title.setText(dataSet.get(FORMNAME));
        holder.shortDescription.setText("Questionaire Id : "+dataSet.get(FORM_ID_KEY));

        if(dataSet.get(HAS_BEEN_DOWNLOADED) != null && dataSet.get(HAS_BEEN_DOWNLOADED).equals("true")){
            holder.cardView.setCardElevation(2);
            holder.imgPattern.setVisibility(View.VISIBLE);
            holder.imgProject.setVisibility(View.GONE);
            holder.dividerUnDownloaded.setVisibility(View.GONE);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            holder.divider.setVisibility(View.GONE);
            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seeListRespons(position);
                }
            });
            holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showOptionDelete(position);
                    return false;
                }
            });
//            holder.imgPattern.setImageResource(imagePatternDrawables[position&7]);
        }else {
            //undownloaded
            holder.cardView.setCardElevation(0);
            holder.imgPattern.setVisibility(View.GONE);
            holder.imgProject.setVisibility(View.VISIBLE);
            holder.dividerUnDownloaded.setVisibility(View.VISIBLE);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            if((position==0) || (position > 0 && mDataSet.get(position-1).get(HAS_BEEN_DOWNLOADED).equals("true")))
                holder.divider.setVisibility(View.VISIBLE);
            else holder.divider.setVisibility(View.GONE);
            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    downloadForm(position);
                }
            });
        }
    }

    private void seeListRespons(int position){
        Intent intent = new Intent(context, CollectivaListRespons.class);
        intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                ApplicationConstants.FormModes.VIEW_SENT);
        intent.putExtra(FORM_ID_KEY, mDataSet.get(position).get(FORM_ID_KEY));
        intent.putExtra(FORMNAME, mDataSet.get(position).get(FORMNAME));
        context.startActivity(intent);
    }

    private void showOptionDelete(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(mDataSet.get(position).get(FORMNAME));
        builder.setItems(new String[]{"See List Response","Open new Form","Delete Form"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        seeListRespons(position);
                        break;
                    case 1:
                        openNewForm(position);
                        break;
                    case 2:
                        showConfirmDelete(position);
                        break;
                }
            }
        });
        builder.show();
    }

    private void openNewForm(int position){
        Cursor formslist = new FormsDao().getFormsCursor();
        //getting id
        long itemIdPosition = -1;
        if(formslist.moveToFirst()){
            do {
                if(formslist.getString(formslist.getColumnIndex(FormsProviderAPI.FormsColumns.JR_FORM_ID)).equals(mDataSet.get(position).get(FORM_ID_KEY))){
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
            intent.putExtra(Var.CURRENT_FORMNAME, mDataSet.get(position).get(FORMNAME));
            intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
            context.startActivity(intent);
        }else {
            //xxxseharusnya tidak terjadi, tidak mungkin itemIdposition < 1
            Toast.makeText(context, "Form not exist", Toast.LENGTH_SHORT).show();
        }
    }
    private void deleteForm(int position){
        ContentResolver cr = context.getContentResolver();
        String formIdKey = mDataSet.get(position).get(FORM_ID_KEY);
        Cursor formslist = new FormsDao().getFormsCursor();
        //getting id
        Long itemIdPosition = Long.valueOf("-1");
        if(formslist.moveToFirst()){
            do {
                if(formslist.getString(formslist.getColumnIndex(FormsProviderAPI.FormsColumns.JR_FORM_ID)).equals(formIdKey)){
                    itemIdPosition = formslist.getLong(formslist.getColumnIndex(FormsProviderAPI.FormsColumns._ID));
                }
            }while (formslist.moveToNext());
        }

        Uri deleteForm = Uri.withAppendedPath(FormsProviderAPI.FormsColumns.CONTENT_URI, itemIdPosition.toString());
        cr.delete(deleteForm, null, null);

        HashMap<String, String> deletedItems = mDataSet.get(position);
        mDataSet.remove(position);
        deletedItems.put(HAS_BEEN_DOWNLOADED,"false");
        mDataSet.add(deletedItems);
        notifyDataSetChanged();
    }

    private void showConfirmDelete(final int position){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Form");
        builder.setMessage("Are you sure to delete \""+mDataSet.get(position).get(FORMNAME)+"form?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteForm(position);
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void downloadForm(final int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Download Form");
        builder.setMessage("Are you sure to download this form?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((CollectivaListForms) fragment).downloadForm(mDataSet.get(position));
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    @Override
    public Filter getFilter() {
        return new ValueFilter();
    }

    class ValueFilter extends Filter{
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint.toString().trim().equals("")) {
                CollectivaAdapterListForm.this.mDataSet = CollectivaAdapterListForm.this.mDataSetOri;
            } else {
                CollectivaAdapterListForm.this.mDataSet = new ArrayList();
                for (HashMap<String, String> as :CollectivaAdapterListForm.this.mDataSetOri) {
                    if(as.get(FORMNAME).toLowerCase().contains(constraint)){
                        CollectivaAdapterListForm.this.mDataSet.add(as);
                    }
                }
            }
            FilterResults res = new FilterResults();
            res.count = CollectivaAdapterListForm.this.mDataSet.size();
            res.values = CollectivaAdapterListForm.this.mDataSet;
            return res;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            CollectivaAdapterListForm.this.mDataSet = (ArrayList) results.values;
            CollectivaAdapterListForm.this.notifyDataSetChanged();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, shortDescription;
        LinearLayout divider;
        View dividerUnDownloaded;
        CardView cardView;
        ImageView imgProject;
        ImageView imgPattern;

        ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            shortDescription = (TextView) itemView.findViewById(R.id.short_description);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            divider = (LinearLayout) itemView.findViewById(R.id.divider);
            dividerUnDownloaded = itemView.findViewById(R.id.divider_undownload);
            imgProject = (ImageView) itemView.findViewById(R.id.img_project);
            imgPattern = (ImageView) itemView.findViewById(R.id.image_pattern);

        }
    }
}
