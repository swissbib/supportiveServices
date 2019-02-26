package org.swissbib.extern.xSwissBib.librarysystems;

import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateResponse;
import org.swissbib.extern.xSwissBib.services.circulation.responsemodel.Institution;
import org.swissbib.extern.xSwissBib.services.common.LibraryProperties;
import org.swissbib.extern.xSwissBib.services.common.XServiceException;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 13.07.2009
 * Time: 22:13:13
 */
public abstract class LibrarySystem {
    public final static int AVAILABILITY_REQUEST_BY_BARCODE = 0;
    public final static int AVAILABILITY_REQUEST_BY_LIBRARYCODE = 1;

    private LibraryProperties libraryProperties = null;
    private String[] docItems = null;
    private String[] barcodes = null;
    private boolean debug  = false;
    private String language = "de";
    private String proxyServer = null;

    private Institution institution;

    //private HashMap<String,String> barcodeMap;

    protected String requestURL;


    public LibraryProperties getLibraryProperties() {
        return libraryProperties;
    }

    public void setLibraryProperties(LibraryProperties libraryProperties) {
        this.libraryProperties = libraryProperties;
    }

    public String[] getDocItems() {
        return docItems;
    }

    public void setDocItems(String[] docItems) {
        this.docItems = docItems;
    }

    public String[] getBarcode() {
        return barcodes;
    }

    protected boolean isBarcodeRequested(String barcode) {
        boolean isRequested = false;
        for (String tempB : barcodes) {
            if (tempB.equals(barcode)) {
                isRequested = true;
                break;
            }
        }
        return isRequested;
    }

    public void setBarcode(String[] barcode) {
        this.barcodes = barcode;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getProxyServer() {
        return proxyServer;
    }

    public void setProxyServer(String proxyServer) {
        this.proxyServer = proxyServer;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public abstract CirculationStateResponse requestCircultation(int type, String idls) throws XServiceException;

    public abstract CirculationStateResponse requestCircultation(int type) throws XServiceException;



    public abstract void checkResponseFromSystem(String xServerResponse) throws Exception;


}
