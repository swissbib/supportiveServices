package org.swissbib.extern.xSwissBib.librarysystems.rero;

import org.apache.log4j.Logger;
import org.swissbib.extern.xSwissBib.librarysystems.LibrarySystem;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateResponse;
import org.swissbib.extern.xSwissBib.services.circulation.responsemodel.AvailabilityStatus;
import org.swissbib.extern.xSwissBib.services.common.XServiceException;
import org.swissbib.extern.xSwissBib.services.common.XServiceUtilities;
import org.swissbib.utilities.web.HTTPConnectionHandling;

import org.json.simple.JSONObject;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Scanner;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.swissbib.extern.xSwissBib.librarysystems.LibrarySystem;


/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Matthias Edel
 * Date: 06.05.2019
 */
public class ReroLibrarySystem extends LibrarySystem implements XMLStreamConstants{
    private final static int AVAILABILITY_STATE_GREEN = 0;
    private final static int AVAILABILITY_STATE_RED = 1;
    private final static int AVAILABILITY_STATE_UNKNOWN = 2;
    private final static int AVAILABILITY_STATE_ERROR = 3;

    private final static Logger availLog = Logger.getLogger("swissbibavail");

    public CirculationStateResponse requestCircultation(int type) throws XServiceException {
        return requestCircultation(type, null);
    }

    public CirculationStateResponse requestCircultation(int type, String idls) throws XServiceException {
        CirculationStateResponse response = new CirculationStateResponse();

        for(String barcode : this.getBarcode()) {
            // 1. do rero call
            String url = getLibraryProperties().getUrlsystem() +
                    String.format(getLibraryProperties().getCircQueryParameter(),XServiceUtilities.getStringArrayAsString(getDocItems()))
                    + getLibraryProperties().getAnonymusUser() + "item/" + barcode + "/availability";

            ReroAvailability responseJson;
            try {
                URL obj = new URL(url);

                ObjectMapper mapper = new ObjectMapper();
                try {
                    responseJson = mapper.readValue(obj, ReroAvailability.class);

                    // 2. transform returnvalue and put it into circulationstateresponse object
                    String state = "";
                    switch (responseJson.getAvailability()) {
                        case "available":
                            state = "lendable_available";
                            break;
                        case "unavailable":
                            state = "lendable_borrowed";
                            break;
                    }
                    CirculationStateItem item = new CirculationStateItem();
                    item.setIdentifierBarcode(responseJson.getBarcode());
                    item.setCirculationState(state);
                    JSONObject statusInformation = new JSONObject();
                    statusInformation.put("statusfield", state);
                    item.setItemStatusInfomation(statusInformation);
                    response.setItemList(item);
                } catch (Exception e) {
                    availLog.warn(e.getMessage() + "\\r\\n" + e);
                }

            } catch (MalformedURLException ex) {
                availLog.warn("Please use correct url for request: [domain]/item/[barcode]/availability");
                throw new XServiceException("Please use correct url for request: [domain]/item/[barcode]/availability");
            }
        }

        return response;
    }

    public void checkResponseFromSystem(String xServerResponse) throws XServiceException {
        //json validieren?? falls invalid, exception werfen
            //throw new XServiceException(sB.toString());
    }

    private boolean isNullOrEmpty(String s) {return s == null || s.isEmpty();}
}

class ReroAvailability {
    private String barcode;
    public String getBarcode() { return this.barcode; }
    public void setBarcode(String barcode ) { this.barcode = barcode; }
    private String availability;
    public String getAvailability() { return this.availability; }
    public void setAvailability(String availability) { this.availability = availability; }
}
