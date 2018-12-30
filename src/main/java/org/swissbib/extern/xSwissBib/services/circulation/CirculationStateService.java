package org.swissbib.extern.xSwissBib.services.circulation;

import org.apache.log4j.Logger;
import org.swissbib.extern.xSwissBib.librarysystems.LibrarySystem;
import org.swissbib.extern.xSwissBib.services.circulation.responsemodel.Institution;
import org.swissbib.extern.xSwissBib.services.common.LibraryProperties;
import org.swissbib.extern.xSwissBib.services.common.XServiceException;
import org.swissbib.extern.xSwissBib.services.common.XServiceUtilities;

import java.util.HashMap;


/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 25.07.2009
 * Time: 14:35:43
 */
public class CirculationStateService {
    private HashMap<String,LibraryProperties> libraryProperties;
    private HashMap<String,Institution> mapInstitutions;
    private String proxyServer = null;
    private final static Logger availLog = Logger.getLogger("swissbibavail");

    public CirculationStateResponse getCirculationStatus(String sysnumber, String[] barcode, String idls, boolean debug, String lang) throws XServiceException {

        availLog.debug(new StringBuffer(3).append("getCirculationsStatus requested: barcodes: ").append(XServiceUtilities.getStringArrayAsString(barcode)).
                append(" / sysnumber: ").append(sysnumber).append(" / idls: ").append(idls).toString());

        LibraryProperties lP = XServiceUtilities.getCurrentLibraryProperties(idls,libraryProperties);

        if (null == sysnumber || null == barcode) {
            availLog.warn("Please use proper parameter syntax: ?sysnumber=[xx]&barcode=[barcode of the requested item]idls=[label bibliographic database e.g. DSV01]");
            throw new XServiceException("Please use proper parameter syntax: ?sysnumber=[xx]&barcode=[barcode of the requested item]idls=[label bibliographic database e.g. DSV01]");
        }

        LibrarySystem librarySystem;

        try {
            librarySystem = (LibrarySystem)  Class.forName(lP.getSystemtype()).newInstance();
        }
        catch (ClassNotFoundException cnF) {
            throw new XServiceException("could not load LibrarySystem: " + lP.getSystemtype() + cnF.getMessage());
        }
        catch (InstantiationException iE) {
            throw new XServiceException("could not instantiate  LibrarySystem: " + lP.getSystemtype() +  iE.getMessage());
        }
        catch (IllegalAccessException iA) {
            throw new XServiceException("Ilegal access instantiating  LibrarySystem: " + lP.getSystemtype() +  iA.getMessage());
        }

        librarySystem.setDocItems(new String[] {sysnumber});
        librarySystem.setBarcode(barcode);
        librarySystem.setLibraryProperties(lP);
        librarySystem.setDebug(debug);
        librarySystem.setLanguage(lang);
        librarySystem.setProxyServer(this.proxyServer);
        librarySystem.setInstitution(mapInstitutions.get(lP.getIdsystem()));

        return librarySystem.requestCircultation(LibrarySystem.AVAILABILITY_REQUEST_BY_BARCODE);
    }

    public CirculationStateResponse getCirculationStatusByLibraryNetwork(String sysnumber, String idls, boolean debug, String lang) throws XServiceException {

        availLog.debug(new StringBuffer(3).append("getCirculationsStatus requested: sysnumber: ").append(sysnumber).
                append(" / idls: ").append(idls).toString());

        LibraryProperties lP = XServiceUtilities.getCurrentLibraryProperties(idls,libraryProperties);

        if (null == sysnumber) {
            availLog.warn("Please use proper parameter syntax: ?sysnumber=[xx]&idls=[label bibliographic database e.g. DSV01]");
            throw new XServiceException("Please use proper parameter syntax: ?sysnumber=[xx]&idls=[label bibliographic database e.g. DSV01]");
        }

        LibrarySystem librarySystem;

        try {
            librarySystem = (LibrarySystem)  Class.forName(lP.getSystemtype()).newInstance();
        }
        catch (ClassNotFoundException cnF) {
            throw new XServiceException("could not load LibrarySystem: " + lP.getSystemtype() + cnF.getMessage());
        }
        catch (InstantiationException iE) {
            throw new XServiceException("could not instantiate  LibrarySystem: " + lP.getSystemtype() +  iE.getMessage());
        }
        catch (IllegalAccessException iA) {
            throw new XServiceException("Ilegal access instantiating  LibrarySystem: " + lP.getSystemtype() +  iA.getMessage());
        }

        librarySystem.setDocItems(new String[] {sysnumber});
        librarySystem.setLibraryProperties(lP);
        librarySystem.setDebug(debug);
        librarySystem.setLanguage(lang);
        librarySystem.setProxyServer(this.proxyServer);
        librarySystem.setInstitution(mapInstitutions.get(lP.getIdsystem()));

        return librarySystem.requestCircultation(LibrarySystem.AVAILABILITY_REQUEST_BY_LIBRARYCODE);
    }

    public CirculationStateService (HashMap<String,LibraryProperties> libraryProperties,
                                    HashMap<String,Institution> mapInstitutions,
                                    String proxyServer) {
        this.libraryProperties = libraryProperties;
        this.mapInstitutions = mapInstitutions;
        this.proxyServer = proxyServer;
    }

}
