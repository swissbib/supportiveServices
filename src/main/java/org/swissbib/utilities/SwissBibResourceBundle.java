package org.swissbib.utilities;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 06.11.2009
 * Time: 10:41:46
 */
public class SwissBibResourceBundle {

    private static final String defaultResourceBundleFile = "resources/swissbib/swissbib";
    private static final String defaultSystemResourceBundleFile = "resources/swissbib/swissbib-config";

    private static SwissBibResourceBundle sRB = null;
    private ResourceBundle resourceBundle = null;
    private ResourceBundle systemResourceBundle = null;
    private final static Logger taglibLogger = Logger.getLogger("swissbibtaglib");


    private SwissBibResourceBundle(){

    }

    public static SwissBibResourceBundle instance() {
        if (null == sRB) {
            sRB = new SwissBibResourceBundle();
            sRB.loadResourceBundle();
            //sRB.loadSystemResourceBundle();
        }
        return sRB;
    }

    public String getValue(String key) {

        return this.resourceBundle.getString(key);
    }

    /*
    public String getSystemValue(String key) {

        return this.systemResourceBundle.getString(key);
    }
    */

    private synchronized void loadResourceBundle(){

        if (resourceBundle == null) {
            try {
                ResourceBundle defaultSwissBibProperties = ResourceBundle.getBundle(SwissBibResourceBundle.defaultResourceBundleFile);
                for(String key :  defaultSwissBibProperties.keySet())
                {
                    taglibLogger.debug("SwissBibResourceBundle.loadResourceBundle: loaded key" + defaultSwissBibProperties.getString(key));
                }
                this.resourceBundle = defaultSwissBibProperties;
            } catch (Throwable thr) {
                taglibLogger.debug("SwissBibResourceBundle.loadResourceBundle: Error Throwable", thr);
            }
        }

    }


    /*
    private synchronized void loadSystemResourceBundle(){

        if (systemResourceBundle == null) {
            try {
                ResourceBundle defaultSystemProperties = ResourceBundle.getBundle(SwissBibResourceBundle.defaultSystemResourceBundleFile);
                for(String key :  defaultSystemProperties.keySet())
                {
                    taglibLogger.debug("SwissBibResourceBundle.loadSystemResourceBundle: loaded key " + defaultSystemProperties.getString(key));
                }
                this.systemResourceBundle = defaultSystemProperties;
            } catch (Throwable thr) {
                taglibLogger.debug("SwissBibResourceBundle.loadSystemResourceBundle: Error Throwable", thr);
            }
        }

    }
    */


}
