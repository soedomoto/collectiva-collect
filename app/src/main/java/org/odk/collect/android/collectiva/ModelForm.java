package org.odk.collect.android.collectiva;

/**
 * Created by maztohir on 8/21/2017.
 */

class ModelForm {
    public ModelForm(String formId, String formName, String hash, String formUrl) {
        this.formId = formId;
        this.formName = formName;
        this.hash = hash;
        this.formUrl = formUrl;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFormUrl() {
        return formUrl;
    }

    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }

    private String formId;
    private String formName;
    private String hash;
    private String formUrl;
}
