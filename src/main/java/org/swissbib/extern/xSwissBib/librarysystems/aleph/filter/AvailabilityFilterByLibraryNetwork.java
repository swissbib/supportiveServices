package org.swissbib.extern.xSwissBib.librarysystems.aleph.filter;

import org.apache.log4j.Logger;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateResponse;
import org.swissbib.extern.xSwissBib.services.circulation.responsemodel.Institution;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 18.12.2009
 * Time: 09:20:59
 */
public class AvailabilityFilterByLibraryNetwork extends AlephStreamFilter{

    private CirculationStateItem circItem = null;

    private String[] barcodes;
    private ArrayList<CirculationStateItem> itemList = new ArrayList<CirculationStateItem>();

    Institution institution;

    private boolean rootCircStatus = false;
    private String sessionId = null;
    private boolean loginElement = false;
    private boolean errorOccured = false;
    private String errorMessage = null;
    private Pattern pNebisError = Pattern.compile("Error");
    private final static Logger availLog = Logger.getLogger("swissbibavail");


    public AvailabilityFilterByLibraryNetwork(String sysnumber, Institution institution) {
        super(sysnumber);
        this.institution = institution;
    }

    public CirculationStateItem[] getCircStateItems(){
        return itemList.toArray(new CirculationStateItem[itemList.size()]);
    }

    public CirculationStateResponse getCircStateResponse(){

        CirculationStateResponse response = new CirculationStateResponse(this.getCircStateItems());
        if (this.errorOccured) {
            response.setErrorMessage(this.errorMessage);
        }
        response.setSession(this.sessionId);
        return response;
    }

    @Override
    public boolean accept(XMLStreamReader reader) {

        boolean toParse = false;
        final int eventType = reader.getEventType();

        try {
            switch (eventType) {
                case XMLStreamConstants.ATTRIBUTE:
                case XMLStreamConstants.CDATA:
                case XMLStreamConstants.CHARACTERS:
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.DTD:
                case XMLStreamConstants.ENTITY_DECLARATION:
                case XMLStreamConstants.ENTITY_REFERENCE:
                case XMLStreamConstants.NAMESPACE:
                case XMLStreamConstants.NOTATION_DECLARATION:
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    //Did we find an error constellation??

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    String tempName = reader.getLocalName();
                    if (this.rootCircStatus  && tempName.equalsIgnoreCase("item-data")) {
                        if (this.circItem != null && this.isBarcodeRequested(this.circItem.getIdentifierBarcode()) ) {

                            this.circItem.setIdentifier(this.sysnumber);
                            this.itemList.add(this.circItem);
                            toParse = true;
                        }
                        this.circItem = null;
                    }
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    toParse = false;
                    String name = reader.getLocalName();
                    if (name.equalsIgnoreCase("circ-status")) {
                        //we got the correct rootElement within the XMLResponse
                        this.rootCircStatus = true;
                        break;
                    } else if (this.rootCircStatus && name.equalsIgnoreCase("item-data")) {
                        //did we create former a stateItemObject related to a barcode which is fetched by the client? -> instace != null
                        this.circItem = new CirculationStateItem();
                        toParse = true;
                    } else if (this.rootCircStatus && name.equalsIgnoreCase("loan-status") && this.circItem != null) {
                        this.circItem.setLoanState(reader.getElementText());
                        toParse = true;
                    } else if (this.rootCircStatus && name.equalsIgnoreCase("due-date") && this.circItem != null) {
                        this.circItem.setDueDate(reader.getElementText());
                        toParse = true;
                    } else if (this.rootCircStatus && name.equalsIgnoreCase("sub-library") && this.circItem != null) {

                        this.circItem.setSublibrary(reader.getElementText());
                        toParse = true;
                    }
                     else if (name.equalsIgnoreCase("login")) {
                        /*
                        this happens quite often because xSwissBib doesn't have permission to access the cinfigured xServer
                        <?xml version = "1.0" encoding = "UTF-8"?>
                        <login>
                        <error>Error 0005 Not defined in file www_x_login.</error>
                        </login>
                         */
                        this.errorOccured = true;
                        this.loginElement = true;
                    } else if (name.equalsIgnoreCase("error") && this.rootCircStatus) {
                        //in this combination an application specific error occured e.g. a document with the requested sysnumber wasn't found
                        /*
                            <circ-status>
                            <error>
                            Document: 7363763al doesn't exist in library: DSV51. Make sure you insert BIB library.
                            </error>
                            <session-id>Q4Y7YQHSGAY9LI5AQPU5HB7AI11D1Q5F2YD3SNP7NT1IMTXPS2</session-id>
                            </circ-status>
                         */
                        this.errorOccured = true;
                        this.errorMessage = reader.getElementText();


                    } else if (name.equalsIgnoreCase("error") && this.loginElement) {
                        //error without login or circ-state element
                        //I haven't seen this so far - may be
                        this.errorOccured = true;
                        this.errorMessage = reader.getElementText();

                    } else if (name.equalsIgnoreCase("error") && !this.rootCircStatus && !this.loginElement) {
                        //errorconstellation I haven't seen so far but might be possible

                        this.errorOccured = true;
                        this.errorMessage = reader.getElementText();
                    } else if (name.equalsIgnoreCase("title") && !this.rootCircStatus ) {
                        /*
                        used by NEBIS
                            <html>
                            <head>
                            <title>Error 403</title>
                            </head>
                            <body>
                            <h1>Error 403 Forbidden</h1>
                            Access from IP address <i>192.87.101.148</i> not allowed.

                            </body>
                            </html>
                         */
                        String tempElementText = reader.getElementText();
                        Matcher numbR = pNebisError.matcher(tempElementText);
                        if (numbR.find() ){
                            this.errorOccured = true;
                            this.errorMessage = tempElementText;
                        }


                    } else if (name.equalsIgnoreCase("session-id")) {

                        this.sessionId = reader.getElementText();
                        toParse = true;
                    }
                    break;
            }
        } catch (XMLStreamException sEx){
            availLog.warn("AvailabilityFilter error", sEx);
        }
        catch (Exception ex ) {
            availLog.warn("AvailabilityFilter error", ex);
        }

        catch (Throwable th) {
            availLog.warn("AvailabilityFilter error", th);
        }


        return toParse;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected boolean isBarcodeRequested(String barcode) {
        boolean isRequested = true;
        return isRequested;
    }

}
