package org.swissbib.extern.xSwissBib.librarysystems.alma;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.swissbib.extern.xSwissBib.librarysystems.LibrarySystem;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateResponse;
import org.swissbib.extern.xSwissBib.services.common.XServiceException;
import org.swissbib.extern.xSwissBib.services.common.XServiceUtilities;

import javax.xml.stream.XMLStreamConstants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.simple.JSONObject;


/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Matthias Edel
 * Date: 20.09.2019
 */
public class AlmaLibrarySystem extends LibrarySystem implements XMLStreamConstants{
    private final static int AVAILABILITY_STATE_GREEN = 0;
    private final static int AVAILABILITY_STATE_RED = 1;
    private final static int AVAILABILITY_STATE_UNKNOWN = 2;
    private final static int AVAILABILITY_STATE_ERROR = 3;

    private final static Logger availLog = Logger.getLogger("swissbibavail");

    public CirculationStateResponse requestCircultation(int type) throws XServiceException {
        return requestCircultation(type, null);
    }

    public CirculationStateResponse requestCircultation(int type, String idls) throws XServiceException {
        CirculationStateResponse returnValue = new CirculationStateResponse();

        // 1. get holdings from bibs (sysnr)
        AlmaHoldingsResponse almaHoldings;
        String docItem = getDocItems()[0];
        String url = getLibraryProperties().getUrlsystem() +
                String.format(getLibraryProperties().getCircQueryParameter(),XServiceUtilities.getStringArrayAsString(getDocItems()))
                + getLibraryProperties().getAnonymusUser() + "bibs/" + docItem + "/holdings"
                + "?view=brief&apikey=" + getLibraryProperties().getApiKey();
        try {
            String responseJson = getJsonResponse(url);
            ObjectMapper mapper = new ObjectMapper();
            AlmaHoldingsResponse almaHoldingResponse = mapper.readValue(responseJson, AlmaHoldingsResponse.class);
            if (almaHoldingResponse.getHolding() != null) {
                for (AlmaHolding holding : almaHoldingResponse.getHolding()) {
                    String holdingId = holding.getHolding_id();
                    // 2. get all items for holding:
                    url = getLibraryProperties().getUrlsystem() +
                            String.format(getLibraryProperties().getCircQueryParameter(), XServiceUtilities.getStringArrayAsString(getDocItems()))
                            + getLibraryProperties().getAnonymusUser() + "bibs/" + docItem + "/holdings/" + holdingId + "/items"
                            + "?view=brief&apikey=" + getLibraryProperties().getApiKey();
                    responseJson = getJsonResponse(url);
                    AlmaItemsResponse almaItemsResponse = mapper.readValue(responseJson, AlmaItemsResponse.class);
                    if (almaItemsResponse.getItem() != null) {
                        int bestApiState = AVAILABILITY_STATE_UNKNOWN;
                        CirculationStateItem csItem = new CirculationStateItem();
                        for (AlmaItem item : almaItemsResponse.getItem()) {
                            int state = Integer.parseInt(item.getItem_data().getBase_status().getValue());
                            if (type == LibrarySystem.AVAILABILITY_REQUEST_BY_LIBRARYCODE) {
                                if (state == 0) {
                                    bestApiState = AVAILABILITY_STATE_RED;
                                } else if (state == 1) {
                                    bestApiState = AVAILABILITY_STATE_GREEN;
                                    break;
                                }
                            } else if (type == LibrarySystem.AVAILABILITY_REQUEST_BY_BARCODE) {
                                String barcode = item.getItem_data().getBarcode();
                                if (!barcode.isEmpty()) {
                                    String stateTxt = (state == 0) ? "unavailable" : "lendable_available";
                                    csItem.setIdentifierBarcode(barcode);
                                    JSONObject json = new JSONObject();
                                    json.put("statusfield", stateTxt);
                                    csItem.setItemStatusInfomation(json);
                                    returnValue.setItemList(csItem);
                                }
                            }
                        }
                        if (type == LibrarySystem.AVAILABILITY_REQUEST_BY_LIBRARYCODE) {
                            csItem.setSubLibraryAvailability("NB001", new Integer(bestApiState)); // this sets SNL fix. use a konkordanztabelle when other holdings have to be respected
                            returnValue.setItemList(csItem);
                        }
                    }
                }
            }
        } catch (MalformedURLException ex) {
            availLog.warn("Please use correct url for request: [domain]/item/[barcode]/availability");
            throw new XServiceException("Please use correct url for request: [domain]/item/[barcode]/availability");
        } catch (Exception ex) {
            availLog.warn(ex.getMessage() + "\\r\\n" + ex);
        }

        return returnValue;
    }

    private String getJsonResponse(String url) throws XServiceException {
        String responseJson = "";
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String responseLine = "";
            while ((responseLine = br.readLine()) != null) {
                responseJson += responseLine;
            }
        } catch(MalformedURLException ex) {
            throw new XServiceException("AlmaLibrarySystem::getJsonResponse()");
        } catch(IOException ex) {
            throw new XServiceException("AlmaLibrarySystem::getJsonResponse()");
        }
        return responseJson;
    }

    public void checkResponseFromSystem(String xServerResponse) throws XServiceException {
        //json validieren?? falls invalid, exception werfen
            //throw new XServiceException(sB.toString());
    }

    private boolean isNullOrEmpty(String s) {return s == null || s.isEmpty();}
}



@JsonIgnoreProperties(ignoreUnknown = true)
class AlmaHoldingsResponse {
    private AlmaHolding[] holding;
    public AlmaHolding[] getHolding() { return this.holding; }
    public void setHolding(AlmaHolding[] holding) { this.holding = holding; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AlmaHolding {
    private String holding_id;
    public String getHolding_id() { return this.holding_id; }
    public void setHolding_id(String holding_id) { this.holding_id = holding_id; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AlmaItemsResponse {
    private AlmaItem[] item;
    public AlmaItem[] getItem() { return this.item; }
    public void setItem(AlmaItem[] item) { this.item = item; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AlmaItem {
    private AlmaItemBibData bib_data;
    public AlmaItemBibData getBib_data() { return this.bib_data; }
    public void setBib_data(AlmaItemBibData bib_data) { this.bib_data = bib_data; }
    private AlmaItemItemData item_data;
    public AlmaItemItemData getItem_data() { return this.item_data; }
    public void setItem_data(AlmaItemItemData item_data) { this.item_data = item_data; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AlmaItemBibData {
    private String mms_id;
    public String getMms_id() { return this.mms_id; }
    public void setMms_id(String mms_id) { this.mms_id = mms_id; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AlmaItemItemData {
    private AlmaItemItemDataBaseStatus base_status;
    public AlmaItemItemDataBaseStatus getBase_status() { return this.base_status; }
    public void setBase_status(AlmaItemItemDataBaseStatus base_status) { this.base_status = base_status; }
    private String barcode;
    public String getBarcode() { return this.barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AlmaItemItemDataBaseStatus {
    private String desc;
    public String getDesc() { return this.desc; }
    public void setDesc(String desc) { this.desc = desc; }
    private String value;
    public String getValue() { return this.value; }
    public void setValue(String value) { this.value = value; }
}
