package org.odk.collect.android.collectiva;

import org.odk.collect.android.dto.Instance;

import java.util.HashMap;

/**
 * Created by maztohir on 7/3/2017.
 */

public class CollectivaInstances{
    private HashMap<String, String> information;
    private String primaryTitle;
    private Instance instances;

    public CollectivaInstances(Instance instances) {
        this.instances = instances;
    }


    public HashMap<String, String> getInformation() {
        return information;
    }

    public void setInformation(HashMap<String, String> information) {
        this.information = information;
    }


    public String getPrimaryTitle() {
        return primaryTitle;
    }

    public void setPrimaryTitle(String primaryTitle) {
        this.primaryTitle = primaryTitle;
    }

    public Instance getInstances() {
        return instances;
    }

    public void setInstances(Instance instances) {
        this.instances = instances;
    }
}
