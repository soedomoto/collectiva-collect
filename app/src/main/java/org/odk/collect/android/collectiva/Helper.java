package org.odk.collect.android.collectiva;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Helper {
    public static ModelResponse getResponsLogin(String json){
        Log.d("DEBUGCOLL", "respons login : "+ json);
        try {
            JSONObject resp = new JSONObject(json);
            int code = resp.getInt("code");
            String message = resp.getString("message");

            ModelResponse respModel  = new ModelResponse(true, code, message);
            if(code == 1){
                JSONObject dataJson = resp.getJSONObject("data");
                Map<String, String> data = new HashMap<>();
                data.put("token",dataJson.getString("token"));
                data.put("name",dataJson.getString("name"));
                respModel.setRespons(data);
            }
            return respModel;
        } catch (JSONException e) {
            Log.d("DEBUGCOLL",e.toString());
        }
        return new ModelResponse(false,0,"Failed to login");
    }

    public static ModelResponse getResponsListSurvey(String json){
        Log.d("DEBUGCOLL", "respons login : "+ json);
        try {
            JSONObject resp = new JSONObject(json);
            int code = resp.getInt("code");
            String message = resp.getString("message");

            ModelResponse respModel  = new ModelResponse(true, code, message);
            if(code == 1){
                ArrayList<ModelSurvey> modelSurveys = new ArrayList<>();
                JSONArray surveys = resp.getJSONArray("surveys");
                for(int i=0; i<surveys.length();i++){
                    JSONObject itemSurvey = surveys.getJSONObject(i);
                    String idSurvey = itemSurvey.getString("id_survey");
                    String labelSurvey = itemSurvey.getString("label_survey");
                    String iconSurvey = itemSurvey.getString("icon_survey");
                    String agregateSurvey = itemSurvey.getString("agregate_survey");
                    String userLevel = itemSurvey.getString("user_level");
                    String userLevelLabel = itemSurvey.getString("user_level_label");

                    ModelSurvey modelSurvey = new ModelSurvey(idSurvey, labelSurvey, iconSurvey, agregateSurvey, userLevel, userLevelLabel);

                    JSONArray forms = itemSurvey.getJSONArray("forms");
                    for(int f=0;f<forms.length();f++){
                        JSONObject itemForm = forms.getJSONObject(f);
                        String formId = itemForm.getString("form_id");
                        String formName = itemForm.getString("form_name");
                        String hash = itemForm.getString("hash");
                        String formUrl = itemForm.getString("form_url");

                        ModelForm modelForm = new ModelForm(formId, formName, hash, formUrl);
                        modelSurvey.addForm(modelForm);
                    }
                    modelSurveys.add(modelSurvey);
                }
                respModel.setRespons(modelSurveys);
            }
            return respModel;
        } catch (JSONException e) {
            Log.d("DEBUGCOLL",e.toString());
        }
        return new ModelResponse(false,0,"Get data form server failed");
    }
}
