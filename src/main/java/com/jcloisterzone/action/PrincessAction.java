package com.jcloisterzone.action;


//TODO undeploy type
public class PrincessAction extends UndeployAction {

    public PrincessAction() {
        super("princess");
    }

    @Override
    protected int getSortOrder() {
        return 1;
    }

    @Override
    public String toString() {
        return "undeploy with princesss";
    }
}
