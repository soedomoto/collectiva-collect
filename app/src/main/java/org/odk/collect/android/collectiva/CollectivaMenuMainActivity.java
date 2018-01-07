package org.odk.collect.android.collectiva;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.squareup.picasso.Picasso;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.FileManagerTabs;
import org.odk.collect.android.activities.CollectivaListForms;
import org.odk.collect.android.activities.InstanceChooserList;
import org.odk.collect.android.activities.InstanceUploaderList;
import org.odk.collect.android.activities.SplashScreenActivity;
import org.odk.collect.android.utilities.ApplicationConstants;

import java.util.ArrayList;

public class CollectivaMenuMainActivity extends AppCompatActivity {

    private AccountHeader accountHeader = null;
    private Drawer drawer = null;

    //Identifier
    private static final int PROJECT1 = 1;
    private static final int PROJECT2 = 2;
    private static final int PROJECT3 = 3;
    private static final int MENUSURVEY = 4;
    private static final int MENUPROGRESS = 5;
    private static final int MENUNOTIFICATIOn = 6;
    private static final int MENUPROFILE = 7;
    private static final int MENUSETTING = 8;
    private static final int MENULOGOUT = 9;
    private static final int MANAGEPROJECT = 10;

    private static final int SUBMENUDELETESAVEDFORM = 11;
    private static final int SUBMENUEDITINSTANCE = 12;
    private static final int SUBMENUSENDFINALIZEDFORM = 13;
    private static final int SUBMENUVIEWSENDFORM = 14;

    public static final String ACTIVE_SURVEY_KEY = "Activesurveykey";
    private SharedPreferences sharedPreferences;

    private CollectivaListForms collectivaListForms;
    private String activedSurvey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collectiva_activity_sample_menu_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("List Questionnaire");
        sharedPreferences = getSharedPreferences(CollectivaPreferences.COLLECTIVA_PREFERENCES_KEY, MODE_PRIVATE);

        boolean isComplexSurvey = sharedPreferences.getBoolean(Var.COLLECTIVA_MODE_COMPLEKS, false);

        collectivaListForms = new CollectivaListForms();

//        IProfile profile1 = new ProfileDrawerItem().withName("SENSUS EKONOMI").withEmail("Anda sebagai PCL").withIcon(R.drawable.cl_profile_se).withIdentifier(PROJECT1);
//        IProfile profile2 = new ProfileDrawerItem().withName("SUSENAS").withEmail("Anda sebagai PCL").withIcon(R.drawable.cl_profile_sp).withIdentifier(PROJECT2);

        //default project
//        if(sharedPreferences.getString(ACTIVE_SURVEY_KEY,"").equals("")){
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putString(ACTIVE_SURVEY_KEY,activedSurvey);
//            editor.apply();
//        }

        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(true)
                .withHeaderBackground(R.drawable.cl_bg_header)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        if(profile.getIdentifier()==PROJECT1){
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(ACTIVE_SURVEY_KEY,"SENSUS EKONOMI");
                            editor.apply();
                        }else {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(ACTIVE_SURVEY_KEY,"SUSENAS");
                            editor.apply();
                        }
                        collectivaListForms.triggerActiveSurvey();
                        return false;
                    }
                })
                .withOnProfileClickDrawerCloseDelay(700)
                .withSavedInstance(savedInstanceState)
                .build();

        if(isComplexSurvey){
            ArrayList<ModelSurvey> modelSurveys = new ArrayList<>();
            ModelResponse modelResponse = Helper.getResponsListSurvey(sharedPreferences.getString("json_surveys",""));
            modelSurveys = (ArrayList<ModelSurvey>) modelResponse.getRespons();

            for (int i=0;i<modelSurveys.size();i++) {
                ModelSurvey item = modelSurveys.get(i);
                IProfile profile = new ProfileDrawerItem().withName(item.getLabelSurvey()).withEmail(item.getUserLevelLabel()).withIcon(R.drawable.cl_profile_se).withIdentifier(PROJECT1);
                accountHeader.addProfile(profile,(i+1));


            }

//        String activeSurvey = sharedPreferences.getString(ACTIVE_SURVEY_KEY,"");
            ModelSurvey item = modelSurveys.get(0);
            IProfile activeProfile = new ProfileDrawerItem().withName(item.getLabelSurvey()).withEmail(item.getUserLevelLabel()).withIcon(R.drawable.cl_profile_se).withIdentifier(PROJECT1);;
//        if(activeSurvey.equals(activeSurvey)) activeProfile = ;

            accountHeader.addProfile(new ProfileSettingDrawerItem().withName("Manage Project").withIcon(R.drawable.ic_setting).withIdentifier(MANAGEPROJECT),modelSurveys.size());
            accountHeader.setActiveProfile(activeProfile);
        }

        DrawerBuilder builder = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withHasStableIds(true)
                .withItemAnimator(new DefaultItemAnimator())
                .withAccountHeader(accountHeader)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        switch ((int) drawerItem.getIdentifier()){
                            case MENUSURVEY:
                                getSupportActionBar().setTitle("List Questionnaire");
                                openFragment(collectivaListForms);
                                break;

                            case MENULOGOUT:
                                AlertDialog.Builder builder = new AlertDialog.Builder(CollectivaMenuMainActivity.this);
                                builder.setMessage("Are you sure to logout?");
                                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        logout();
                                    }
                                });
                                builder.setNegativeButton("No", null);
                                builder.show();
                                break;

                            case MENUSETTING:
                                startActivity(new Intent(CollectivaMenuMainActivity.this, CollectivaPreferences.class));
                                break;
                            case SUBMENUDELETESAVEDFORM:
                                startActivity(new Intent(getApplicationContext(),
                                        FileManagerTabs.class));
                                break;
                            case SUBMENUSENDFINALIZEDFORM:
                                startActivity(new Intent(getApplicationContext(),
                                        InstanceUploaderList.class));
                                break;
                            case SUBMENUVIEWSENDFORM:
                                Intent i = new Intent(getApplicationContext(), InstanceChooserList.class);
                                i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                                        ApplicationConstants.FormModes.VIEW_SENT);
                                startActivity(i);
                                break;
                            case SUBMENUEDITINSTANCE:
                                Intent e = new Intent(getApplicationContext(), InstanceChooserList.class);
                                e.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                                        ApplicationConstants.FormModes.EDIT_SAVED);
                                startActivity(e);
                                break;
                        }

                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(true);

        if(isComplexSurvey) {
            drawer = builder.build();

            drawer.addItem(new PrimaryDrawerItem().withName("List Questionaire").withIcon(R.drawable.ic_survey).withIdentifier(MENUSURVEY));
            drawer.addItem(new DividerDrawerItem());
            drawer.addItem(new PrimaryDrawerItem().withName("My Profile").withIcon(R.drawable.ic_profile).withIdentifier(MENUPROFILE));
            drawer.addItem(new PrimaryDrawerItem().withName("Setting").withIcon(R.drawable.ic_setting).withIdentifier(MENUSETTING));
            drawer.addItem(new PrimaryDrawerItem().withName("Logout").withIcon(R.drawable.ic_logout).withIdentifier(MENULOGOUT));

            if(savedInstanceState == null){
                drawer.setSelection(MENUSURVEY);
            }
        }

//        drawer.addItem(new SecondaryDrawerItem().withName("  ").withIcon(R.drawable.ic_delete).withIdentifier(SUBMENUDELETESAVEDFORM));


        openFragment(collectivaListForms);
        initLoader();
    }

    private void logout() {
        SharedPreferences shr = getSharedPreferences(CollectivaPreferences.COLLECTIVA_PREFERENCES_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = shr.edit();
        editor.putBoolean(CollectivaLoginActivity.ISLOGIN, false);
        editor.apply();
        startActivity(new Intent(CollectivaMenuMainActivity.this, SplashScreenActivity.class));
        finish();
    }

    private void openFragment(Fragment fragment){
        try {
            if(fragment != null){
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_main_menu, fragment).commit();
                drawer.closeDrawer();
            }
        }catch (Exception e){
            Log.d("DEBUG", e.getMessage());
        }
    }

    private void initLoader(){
        //init when url has been found
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
}
