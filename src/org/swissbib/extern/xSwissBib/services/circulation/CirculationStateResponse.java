package org.swissbib.extern.xSwissBib.services.circulation;

import org.swissbib.extern.xSwissBib.services.circulation.responsemodel.Institution;
import org.swissbib.extern.xSwissBib.services.common.XServiceException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 25.07.2009
 * Time: 17:31:25
 */
public class CirculationStateResponse {

    private ArrayList<CirculationStateItem> itemList = new ArrayList<CirculationStateItem>();
    private String session;
    private String requestedURL = null;
    private String errorMessage = null;
    private final String languageDefault = "de";

    public CirculationStateResponse(CirculationStateItem item) {
        this.itemList.add(item);
    }



    public CirculationStateResponse(CirculationStateItem[] item) {

        itemList.addAll(Arrays.asList(item));
        //for (CirculationStateItem t: item){
        //    this.itemList.add(t);
        //}
    }

    public CirculationStateResponse() {}

    public boolean hasError() {
        return this.errorMessage != null; 
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public CirculationStateItem[] getItemList() {
        //return itemList.toArray(new CirculationStateItem[]{});
        return itemList.toArray(new CirculationStateItem[itemList.size()]);
    }

    public void setItemList(CirculationStateItem item) {
        this.itemList.add(item) ;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getRequestedURL() {
        return requestedURL;
    }

    public void setRequestedURL(String requestedURL) {
        this.requestedURL = requestedURL;
    }

    public void formatItemsResponse(Institution institution) {

        for (CirculationStateItem itemState : itemList) {

            formatItemsResponse(institution, languageDefault);

        }


    }


    public void formatItemsResponse(Institution institution, String language) {

        for (CirculationStateItem itemState : itemList) {

            institution.formatItem(itemState, language);

        }


    }


}
