package org.swissbib.extern.xSwissBib.services.circulation;

import org.json.simple.JSONObject;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 25.07.2009
 * Time: 17:33:33
 */
public class CirculationStateItem {

    String identifier = null;
    String identifierBarcode = null;
    String circulationState = null;
    String numberRequests = null;
    String dueDate = null;
    String loanState = null;
    String dueHour = null;
    String z30description = null;
    String sublibrary = null;
    String collection = null;
    String location = null;
    String pages = null;
    String location2 = null;
    String opacnote = null;

    JSONObject itemStatusInfomation;
    


    public CirculationStateItem (){}


    public CirculationStateItem (String identifier, String identifierBarcode,
                                 String numberRequests, String dueDate,
                                 String loanState){
        
        this.identifier = identifier;
        this.identifierBarcode = identifierBarcode;
        this.numberRequests = numberRequests;
        this.dueDate = dueDate;
        this.loanState = loanState;

    }


    //now getter because it is processed using loanState and dueDate
    public String getCirculationState() {
        return circulationState;
    }


    public String getNumberRequests() {
        return numberRequests;
    }

    public void setNumberRequests(String numberRequests) {
        this.numberRequests = numberRequests;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifierBarcode() {
        return identifierBarcode;
    }

    public void setIdentifierBarcode(String identifierBarcode) {
        this.identifierBarcode = identifierBarcode;
    }

    public String getLoanState() {
        return loanState;
    }

    public void setLoanState(String loanState) {
        this.loanState = loanState;
    }

    public void setDueHour(String dueHour) {
        this.dueHour = dueHour;
    }

    public String getDueHour() {
        return dueHour;
    }


    public void setCirculationState(String circulationState) {
        this.circulationState = circulationState;
    }

    public String getZ30description() {
        return z30description;
    }

    public void setZ30description(String z30description) {
        this.z30description = z30description;
    }

    public String getSublibrary() {
        return sublibrary;
    }

    public void setSublibrary(String sublibrary) {
        this.sublibrary = sublibrary;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getLocation2() {
        return location2;
    }

    public void setLocation2(String location2) {
        this.location2 = location2;
    }

    public String getOpacnote() {
        return opacnote;
    }

    public void setOpacnote(String opacnote) {
        this.opacnote = opacnote;
    }


    public JSONObject getItemStatusInfomation() {

        if (null == this.itemStatusInfomation) {
            return new JSONObject();
        }

        return this.itemStatusInfomation;
    }

    public void setItemStatusInfomation(JSONObject itemStatusInfomation) {
        this.itemStatusInfomation = itemStatusInfomation;
    }
}
