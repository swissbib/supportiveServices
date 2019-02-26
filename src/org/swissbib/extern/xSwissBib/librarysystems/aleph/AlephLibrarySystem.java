package org.swissbib.extern.xSwissBib.librarysystems.aleph;

import org.apache.log4j.Logger;
import org.swissbib.extern.xSwissBib.librarysystems.LibrarySystem;
import org.swissbib.extern.xSwissBib.librarysystems.aleph.filter.AlephErrorStreamFilter;
import org.swissbib.extern.xSwissBib.librarysystems.aleph.filter.AlephStreamFilter;
import org.swissbib.extern.xSwissBib.librarysystems.aleph.filter.AvailabilityFilter;
import org.swissbib.extern.xSwissBib.librarysystems.aleph.filter.AvailabilityFilterByLibraryNetwork;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateResponse;
import org.swissbib.extern.xSwissBib.services.common.XServiceException;
import org.swissbib.extern.xSwissBib.services.common.XServiceUtilities;
import org.swissbib.utilities.web.HTTPConnectionHandling;

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


/**
 * Created by Project SwissBib, www.swissbib.org. 
 * Author: Günter Hipler
 * Date: 13.07.2009
 * Time: 15:51:33
 *
 */
public class AlephLibrarySystem extends LibrarySystem implements XMLStreamConstants{
    private final static String X_SERVER_CODE = "/X?";
    private final static String OP_PARAMETER = "op=";
    private final static String HTTP_GET_DELIMITER = "&";

    private final static String OP_CODE_GET_INTERNAL_ID = "bor-by-key";
    private final static String OP_CODE_PUBLISH_AVAIL = "publish_avail";
    private final static String OP_CODE_CIRC_STATUS = "circ-status";

    private final static String OAI_OAI_PMH = "OAI-PMH";
    private final static String OAI_SESSION_ID = "session-id";
    private final static String OAI_ListRecords = "ListRecords";
    private final static String OAI_HEADER = "header";
    private final static String OAI_RECORD = "record";
    private final static String OAI_METADATA = "metadata";
    private final static String OAI_IDENTIFIER = "identifier";

    private final static String MARCXML_DATAFIELD = "datafield";
    private final static String MARCXML_ATTR_TAG = "tag";
    private final static String MARCXML_ATTR_TAG_AVA = "AVA";
    private final static String MARCXML_SUBFIELD = "subfield";
    private final static String MARCXML_SUBFIELD_CODE_AVA = "e";
    private final static String MARCXML_SUBFIELD_CODE = "code";
    private final static String MARCXML_AVAILABLE_VALUE = "available";
    private final static String MARCXML_AVAILABLE_ERROR = "error";
    private final static String MARCXML_AVAILABLE_ERROR_MESSAGE = "error_message";

    private final static String XSERVER_LOGIN_ERROR = "login";
    private final static String XSERVER_LOGIN_APACHE_ERROR = "html";
    private final static String XSERVER_CIRCSTATUS_ERROR_ROOT = "circ-status";
    private final static String XSERVER_CIRCSTATUS_ERROR_FIELD = "error";
    private final static String XSERVER_LOGIN_ERROR_ERRORFIELD = "error";

    private final static int AVAILABILITY_STATE_GREEN = 0;
    private final static int AVAILABILITY_STATE_RED = 1;
    private final static int AVAILABILITY_STATE_UNKNOWN = 2;
    private final static int AVAILABILITY_STATE_ERROR = 3;

    private final static Logger availLog = Logger.getLogger("swissbibavail");

    public CirculationStateResponse requestCircultation(int type) throws XServiceException {
        return requestCircultation(type, null);
    }

    public CirculationStateResponse requestCircultation(int type, String idls) throws XServiceException {
        CirculationStateResponse response = null;
        String xServerResponse = null;

        String alephXRequest = getLibraryProperties().getUrlsystem() +
            String.format(getLibraryProperties().getCircQueryParameter(),XServiceUtilities.getStringArrayAsString(getDocItems()))
                + getLibraryProperties().getAnonymusUser();

        try {
            xServerResponse = this.doAlephXRequest(alephXRequest);

            //ToDo better check of response using this op-code -> it seems Aleph knows a lot of errors...
            this.requestURL = alephXRequest;
            //this.checkResponseFromSystem(xServerResponse);

            response = this.parseGetCircStatusStax(xServerResponse, type);
            response.setRequestedURL(alephXRequest);
        }
        catch (XMLStreamException stE){
            availLog.warn("XMLStreamException while accessing remote library system: ", stE);
            throw new XServiceException("XMLStreamException while accessing remote library system: " + stE.getMessage());
        }
        catch (IOException ioE) {
            availLog.warn("IOException while accessing remote library system: ", ioE);
            throw new XServiceException("IOException while accessing remote library system: " + ioE.getMessage());
        } catch (Throwable thE) {
            availLog.warn("Throwable Exception while accessing remote library system: ", thE);
            throw new XServiceException("Throwable Exception while accessing remote library system: " + thE.getMessage());
        }

        if (type == LibrarySystem.AVAILABILITY_REQUEST_BY_BARCODE) {
            // just leave the items in its given state...
        } else if (type == LibrarySystem.AVAILABILITY_REQUEST_BY_LIBRARYCODE) {
            // go through all items of the response you got back from the xmlparser; get best loanstatus for each sub-library:
            org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem[] items = response.getItemList();
            String libCode;
            HashMap<String, Integer> libBestAvail = new HashMap<String, Integer>();
            for (org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem item : items) {
                libCode = item.getSublibrary();
                int availabilityState = getAvailabilityState(item, idls);
                if (libBestAvail.get(libCode) == null || libBestAvail.get(libCode) > availabilityState) libBestAvail.put(libCode, availabilityState);
            }

            // create new response-items:
            response.clearItemList();
            for (HashMap.Entry<String, Integer> entry : libBestAvail.entrySet()) {
                CirculationStateItem item = new CirculationStateItem();
                item.setSubLibraryAvailability(entry.getKey(), entry.getValue());
                response.setItemList(item);
            }
        }

        if (type == LibrarySystem.AVAILABILITY_REQUEST_BY_BARCODE) response.formatItemsResponse(this.getInstitution(),this.getLanguage());

        return response;
    }

    public void checkResponseFromSystem(String xServerResponse) throws XServiceException {
        try {
            ByteArrayInputStream bAxServerResponse = new ByteArrayInputStream(xServerResponse.getBytes("UTF-8"));
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

            AlephErrorStreamFilter aFilter = new AlephErrorStreamFilter(this.getDocItems()[0]);

            XMLStreamReader p = inputFactory.createFilteredReader(inputFactory.createXMLStreamReader
                    (new StreamSource(bAxServerResponse)),aFilter) ;

            while(p.hasNext() )
            {   try {
                    p.next();

                } catch (Exception ex){
                    availLog.info("AlephLibrarySystem.checkResponseFromSystem: Exception error: ", ex);
                } catch (Throwable thEx) {
                    availLog.info("AlephLibrarySystem.checkResponseFromSystem: Throwable error: ", thEx);
                }
            }

        } catch (UnsupportedEncodingException useE){
            StringBuilder sB  = new StringBuilder();
            for (StackTraceElement sE : useE.getStackTrace() ) {
                sB.append(sE.getMethodName()).append("\n");
                sB.append(sE.getClassName()).append("\n");
                sB.append(sE.getFileName()).append("\n");
                sB.append(sE.getLineNumber()).append("\n");
            }
            throw new XServiceException(sB.toString());
        } catch (XMLStreamException xmlSE){
            StringBuilder sB  = new StringBuilder();
            for (StackTraceElement sE : xmlSE.getStackTrace() ) {
                sB.append(sE.getMethodName()).append("\n");
                sB.append(sE.getClassName()).append("\n");
                sB.append(sE.getFileName()).append("\n");
                sB.append(sE.getLineNumber()).append("\n");
            }
            throw new XServiceException(sB.toString());
        }
    }

    private String doAlephXRequest(String requestedURL) throws IOException {
        String response = "";
        this.requestURL = requestURL;
        availLog.info("URL library system: " + requestedURL);
        HTTPConnectionHandling connectionHandling = null;

        if (null != this.getProxyServer() && this.getProxyServer().length() > 0) {
            availLog.debug("general proxy is defined in web.xml: " + this.getProxyServer());
            connectionHandling = new HTTPConnectionHandling(this.getProxyServer());
        } else {
            availLog.debug("no general proxy in web.xml defined");
            connectionHandling = new HTTPConnectionHandling();
        }

        availLog.debug("proxy definition for individual system: " + this.getLibraryProperties().isUseProxy() + " -> both must be true to use proxy server");
        HttpURLConnection connection = connectionHandling.getHTTPConnection(requestedURL,
                                                                            this.getLibraryProperties().isUseProxy());

        InputStream is = (InputStream) connection.getContent();
        response = new Scanner( is ).useDelimiter( "\\Z" ).next();

        availLog.debug("\nresponse from system");
        availLog.debug(response);

        if ( is != null )
            is.close();
        return response;
    }

    private CirculationStateResponse parseGetCircStatusStax(String xServerResponse, int type) throws UnsupportedEncodingException,
                                                                                            XMLStreamException, Exception
                                                                                                        {
        ByteArrayInputStream bAxServerResponse = new ByteArrayInputStream(xServerResponse.getBytes("UTF-8"));
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

        AlephStreamFilter aFilter;
        if (type == AVAILABILITY_REQUEST_BY_BARCODE) {
            aFilter = new AvailabilityFilter(this.getDocItems()[0],this.getBarcode(),this.getInstitution() );
        } else if (type == AVAILABILITY_REQUEST_BY_LIBRARYCODE) {
            aFilter = new AvailabilityFilterByLibraryNetwork(this.getDocItems()[0],this.getInstitution() );
        }
        else throw new Exception("unsupported availability request type");

        XMLStreamReader p = inputFactory.createFilteredReader(inputFactory.createXMLStreamReader
                (new StreamSource(bAxServerResponse)),aFilter) ;

        while(p.hasNext() )
        {   try {
                p.next();
            } catch (Exception ex){
                availLog.info("AlephLibrarySystem.parseGetCircStatusStax: Exception error: ", ex);
                p.next();
            } catch (Throwable thEx) {
                availLog.info("AlephLibrarySystem.parseGetCircStatusStax: Throwable error: ", thEx);
                p.next();
            }
        }
        return aFilter.getCircStateResponse();
    }

    private boolean isNullOrEmpty(String s) {return s == null || s.isEmpty();}

    private int getAvailabilityState(CirculationStateItem item, String idls) {
        int availabilityState = AVAILABILITY_STATE_UNKNOWN;
        String sublibrary = item.getSublibrary();
        String loanState = item.getLoanState();
        String dueDate = item.getDueDate();

        switch (idls) {
            case "DSV01":
                if (isNullOrEmpty(dueDate) && loanState.matches("(?i)Loan|short loan \\(14 days\\)|(.*)Fernleihe(.*)|short loan \\(7 days\\)|short loan \\(3 days\\)|short loan \\(1 day\\)|one day loan|(.*)Reading Room(.*)|(.*)Use on-site(.*)|(.*)Special Reading Room(.*)|(.*)Online(.*)|(.*)Photocopy(.*)")) {
                    availabilityState = AVAILABILITY_STATE_GREEN;
                } else if (!isNullOrEmpty(dueDate) || loanState.matches("(?i)(.*)Missing(.*)|(.*)Removed(.*)|(.*)Not available(.*)|(.*)Cancelled(.*)|(.*)On Repair(.*)|(.*)Binding(.*)|(.*)Archive copy, no loan(.*)|(.*)Relocation UB(.*)|(.*)Exhibition(.*)")) {
                    availabilityState = AVAILABILITY_STATE_RED;
                }
                break;
            case "HSB01":
                  if (isNullOrEmpty(dueDate) && loanState.matches("(?i)loan(.*)|ausleihbar(.*)")) {
                    availabilityState = AVAILABILITY_STATE_GREEN;
                  } else if (!isNullOrEmpty(dueDate) || loanState.matches("(?i)(.*)missing(.*)|(.*)removed(.*)|(.*)vermisst(.*)")) {
                    availabilityState = AVAILABILITY_STATE_RED;
                }
                break;
            case "SBT01":
                if (isNullOrEmpty(dueDate) && loanState.matches("(?i)(.*)prestito(.*)")) {
                    availabilityState = AVAILABILITY_STATE_GREEN;
                } else if (!isNullOrEmpty(dueDate) || loanState.matches("(?i)(.*)missing(.*)|(.*)removed(.*)|(.*)vermisst(.*)")) {
                    availabilityState = AVAILABILITY_STATE_RED;
                }
                break;
            case "ILU01":
                if ((isNullOrEmpty(dueDate) || dueDate.matches("(?i)(.*)On Shelf(.*)")) && loanState.matches("(.*)heimausleihe(.*)")) {
                    availabilityState = AVAILABILITY_STATE_GREEN;
                } else if (!isNullOrEmpty(dueDate) || loanState.matches("(?i)(.*)missing(.*)|(.*)removed(.*)|(.*)vermisst(.*)")) {
                    availabilityState = AVAILABILITY_STATE_RED;
                }
                break;
            case "EBI01":
                if (isNullOrEmpty(dueDate) && loanState.matches("(?i)loan(.*)|heimausleihe(.*)|(.*)days(.*)|(.*)Online(.*)")) {
                    availabilityState = AVAILABILITY_STATE_GREEN;
                } else if (!isNullOrEmpty(dueDate) || loanState.matches("(?i)(.*)missing(.*)|(.*)removed(.*)|(.*)vermisst(.*)")) {
                    availabilityState = AVAILABILITY_STATE_RED;
                }
                break;
            case "SGB01":
                if (isNullOrEmpty(dueDate) && loanState.matches("(?i)(.*)ausleihbar(.*)")) {
                    availabilityState = AVAILABILITY_STATE_GREEN;
                } else if (!isNullOrEmpty(dueDate) || loanState.matches("(?i)(.*)missing(.*)|(.*)removed(.*)|(.*)vermisst(.*)")) {
                    availabilityState = AVAILABILITY_STATE_RED;
                }
                break;
            case "BGR01":
                if (isNullOrEmpty(dueDate) && loanState.matches("(?i)(.*)ausleihbar(.*)|(.*)kurzausleihe(.*)|(.*)ma ausleihbar(.*)|(.*)ma kurzausleihe(.*)|(.*)fh ausleihbar(.*)|(.*)fh ausleihbar 1(.*)|(.*)fh ausleihbar 2(.*)|(.*)fh kurzausleihe(.*)|(.*)fh kurzausleihe 1(.*)|(.*)fh kurzausleihe 2(.*)")) {
                    availabilityState = AVAILABILITY_STATE_GREEN;
                } else if (!isNullOrEmpty(dueDate) || loanState.matches("(?i)(.*)missing(.*)|(.*)removed(.*)|(.*)vermisst(.*)")) {
                    availabilityState = AVAILABILITY_STATE_RED;
                }
                break;
            case "ABN01":
                if (isNullOrEmpty(dueDate) && loanState.matches("(?i)(.*)4 Wochen(.*)|(.*)1 Monat(.*)|(.*)14 Tage(.*)")) {
                    availabilityState = AVAILABILITY_STATE_GREEN;
                } else if (!isNullOrEmpty(dueDate) || loanState.matches("(?i)(.*)missing(.*)|(.*)removed(.*)|(.*)vermisst(.*)")) {
                    availabilityState = AVAILABILITY_STATE_RED;
                }
                break;
            default:
                availabilityState = AVAILABILITY_STATE_ERROR;
        }
        return availabilityState;
    }

    private String doAlephTestrequest() {
        String[] barcodes = {"DSVN1043418","0UBU0152192"};
        this.setBarcode(barcodes);
        return "<circ-status> <item-data><z30-description/><loan-status>Consult LibInfo</loan-status><due-date/><due-hour/><sub-library>Bern UB Romanistik</sub-library><collection>Romanische Philologie Freihandbereich</collection><location>RO II J 580</location><pages/><no-requests/><location-2/><barcode>DSVN1043418</barcode><opac-note/></item-data><item-data><z30-description/><loan-status>Loan</loan-status><due-date/><due-hour/><sub-library>Bern UB ZB</sub-library><collection>Magazin (U1)</collection><location>ZB Rom var 339</location><pages/><no-requests/><location-2/><barcode>000846409</barcode><opac-note/></item-data><item-data><z30-description/><loan-status>Loan</loan-status><due-date/><due-hour/><sub-library>Basel Frz. Sprach- &amp; Lit-Wiss.</sub-library><collection>Freihandbereich</collection><location>FRA 1 Fla 40.334</location><pages/><no-requests/><location-2/><barcode>DSVN3363045</barcode><opac-note/></item-data><item-data><z30-description/><loan-status>Loan</loan-status><due-date/><due-hour/><sub-library>Basel UB</sub-library><collection>Magazin</collection><location>Aoo 3253</location><pages/><no-requests/><location-2/><barcode>0UBU0152192</barcode><opac-note/></item-data><session-id>3JDSRTEPPTJSU9R7471TKFRY6IV8PBRFMUAPBYHNE3573SPKPD</session-id></circ-status>";
    }

}
