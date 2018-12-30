package org.swissbib.extern.xSwissBib.services.common;

import org.swissbib.extern.xSwissBib.services.common.LibraryProperties;

import java.util.HashMap;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 13.07.2009
 * Time: 21:48:33
 */
public class XServiceUtilities {

    public static LibraryProperties getCurrentLibraryProperties(String idls, HashMap<String,LibraryProperties> lP ) throws XServiceException{

        //Todo: Find a better way for casting objects and type evaluation!!
        //Object lP =   mc.getAxisService().getParameter(XServiceConstants.LIBRARY_PROPERTIES).getValue();

        if (null == idls) {
            throw new XServiceException("Please use proper parameter syntax: ?docids=[xx]&idls=[label bibliographic database e.g. HSD01]");
        }
        else if (null == lP || ! lP.getClass().getName().equals(HashMap.class.getName())) {

            throw new XServiceException("Didn't find HashMap with library properties");
        } else if(null == (LibraryProperties)((HashMap)lP).get(idls)) {
            throw new XServiceException("library system with id: " + idls + " not properly configured");
        }

        return (LibraryProperties)((HashMap)lP).get(idls);
    }

    public static String getStringArrayAsString(String [] tArray) {
        String sArrayToString = "";

        for(String t: tArray) {
            sArrayToString += t + ",";
        }

        if (sArrayToString.lastIndexOf(",") > -1) {
            sArrayToString = sArrayToString.substring(0,sArrayToString.lastIndexOf(",") );
        }
        return sArrayToString;
    }


}
