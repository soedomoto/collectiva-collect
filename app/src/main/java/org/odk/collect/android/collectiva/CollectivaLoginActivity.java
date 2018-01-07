package org.odk.collect.android.collectiva;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.odk.collect.android.R;

import java.util.HashMap;
import java.util.Map;

public class CollectivaLoginActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView loginBtn;
    private EditText username, password;
    public static final String ISLOGIN = "isloginkey";
    public SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collectiva_activity_login);

        loginBtn = (TextView) findViewById(R.id.btn_login);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);

        loginBtn.setOnClickListener(this);

        sharedPreferences = getSharedPreferences(CollectivaPreferences.COLLECTIVA_PREFERENCES_KEY, MODE_PRIVATE);

    }

    @Override
    public void onClick(View v) {
        if(TextUtils.isEmpty(username.getText().toString())){
            Toast.makeText(this, "Username can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if(TextUtils.isEmpty(password.getText().toString())){
            Toast.makeText(this, "Pasword can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        login(username.getText().toString(), password.getText().toString());
    }

    public void login(final String username, final String password){
        loginBtn.setText("Signin....");
        MySingleton.getmInstance(this).addToRequestQueue(new StringRequest(Request.Method.POST, sharedPreferences.getString(Var.URL_MAIN_SERVER, "")+"/login.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                onResponLogin(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(CollectivaLoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                loginBtn.setText("LOGIN");
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> maps = new HashMap<String, String>();
                maps.put("username", username);
                maps.put("password", password);
                return maps;
            }
        });
    }

    private void onResponLogin(String response){
        loginBtn.setText("LOGIN");
        ModelResponse modelResponse = Helper.getResponsLogin(response);
        if(modelResponse.isSuccess()){
            Map<String, String> data = (Map<String, String>) modelResponse.getRespons();
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putBoolean(ISLOGIN, true);
            edit.apply();
            getDataSurveys(data.get("token"));

        }else {
            Toast.makeText(CollectivaLoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void getDataSurveys(final String token){
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Get data....");
        progressDialog.show();

        progressDialog.dismiss();
        responOfGetData(Var.DATA_SURVEY);

//        MySingleton.getmInstance(this).addToRequestQueue(new StringRequest(Request.Method.POST, Var.URL_MAIN_SERVER, new Response.Listener<String>() {
//            @Override
//            public void onResponse(String response) {
//                responOfGetData(response);
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                progressDialog.dismiss();
//                createDialogNoServisAvailable(token);
//            }
//        }){
//            @Override
//            protected Map<String, String> getParams() throws AuthFailureError {
//                Map<String, String> map = new HashMap<String, String>();
//                map.put("token", token);
//                return map;
//            }
//        });
    }

    private void responOfGetData(String response){
        ModelResponse modelResponse = Helper.getResponsListSurvey(response);
        if(modelResponse.isSuccess()){
            //save result
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString("json_surveys", response);
            edit.apply();

            startActivity(new Intent(CollectivaLoginActivity.this, CollectivaMenuMainActivity.class ));
            finish();
        }
//        else{
//            createDialogNoServisAvailable(token);
//        }
    }

    public void createDialogNoServisAvailable(final String token){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("You server has a problem");
        builder.setMessage("We seen that your server not return a correct value as survey management server. " +
                "You sould go to https://git.stis.ac.id and see this requirement");
        builder.setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getDataSurveys(token);
            }
        });
        builder.setNegativeButton("Oke", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }
}
