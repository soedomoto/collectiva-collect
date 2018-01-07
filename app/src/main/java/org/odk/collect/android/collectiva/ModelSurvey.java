package org.odk.collect.android.collectiva;

import java.util.ArrayList;

/**
 * Created by lenovo on 5/6/2017.
 */

public class ModelSurvey {

    public String getIdSurvey() {
        return idSurvey;
    }

    public void setIdSurvey(String idSurvey) {
        this.idSurvey = idSurvey;
    }

    public String getLabelSurvey() {
        return labelSurvey;
    }

    public void setLabelSurvey(String labelSurvey) {
        this.labelSurvey = labelSurvey;
    }

    public String getIconSurvey() {
        return iconSurvey;
    }

    public void setIconSurvey(String iconSurvey) {
        this.iconSurvey = iconSurvey;
    }

    public String getAgregateServer() {
        return agregateServer;
    }

    public void setAgregateServer(String agregateServer) {
        this.agregateServer = agregateServer;
    }

    public String getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(String userLevel) {
        this.userLevel = userLevel;
    }

    public String getUserLevelLabel() {
        return userLevelLabel;
    }

    public void setUserLevelLabel(String userLevelLabel) {
        this.userLevelLabel = userLevelLabel;
    }

    private String idSurvey;
    private String labelSurvey;

    public ArrayList<ModelForm> getForms() {
        return forms;
    }

    private String iconSurvey;
    private String agregateServer;
    private String userLevel;

    public ModelSurvey(String idSurvey, String labelSurvey, String iconSurvey, String agregateServer, String userLevel, String userLevelLabel) {
        this.idSurvey = idSurvey;
        this.labelSurvey = labelSurvey;
        this.iconSurvey = iconSurvey;
        this.agregateServer = agregateServer;
        this.userLevel = userLevel;
        this.userLevelLabel = userLevelLabel;
        if(forms==null) forms = new ArrayList<>();
    }
    public void addForm(ModelForm modelForm){
        forms.add(modelForm);
    }
    private String userLevelLabel;
    private ArrayList<ModelForm> forms;
}
