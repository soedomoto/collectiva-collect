package org.odk.collect.android.collectiva;

/**
 * Created by lenovo on 5/6/2017.
 */

public class ModelProjectSurvey {

    private String idProject;
    private String namaProject;
    private String timeCreated;
    private String creatorName;
    private String creatorID;
    private String description;

    //just for dummy
    public ModelProjectSurvey(String namaProject, String timeCreated, String creatorName, String description) {
        this.namaProject = namaProject;
        this.timeCreated = timeCreated;
        this.creatorName = creatorName;
        this.description = description;
    }

    public String getIdProject() {
        return idProject;
    }

    public void setIdProject(String idProject) {
        this.idProject = idProject;
    }

    public String getNamaProject() {
        return namaProject;
    }

    public void setNamaProject(String namaProject) {
        this.namaProject = namaProject;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(String creatorID) {
        this.creatorID = creatorID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
