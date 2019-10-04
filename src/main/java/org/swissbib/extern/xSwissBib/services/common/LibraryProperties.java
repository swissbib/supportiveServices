package org.swissbib.extern.xSwissBib.services.common;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 13.07.2009
 * Time: 19:40:25
 */
public class LibraryProperties {

    private String idsystem;
    private String urlsystem;
    private String systemtype;
    private String circResponseType = "org.swissbib.extern.xSwissBib.services.circulation.CirculationResponseFormatter";
    private String anonymusUser;
    private String circQueryParameter;
    private String apiKey;
    private boolean useProxy = true;


    public LibraryProperties(String idSystem,
                             String urlSystem,
                             String systemtype,
                             String anonymusUser,
                             String circQueryParameter,
                             String apiKey,
                             boolean  useProxy) {


        this.idsystem = idSystem;
        this.urlsystem = urlSystem;
        this.systemtype = systemtype;
        this.anonymusUser = anonymusUser;
        this.circQueryParameter = circQueryParameter;
        this.apiKey = apiKey;
        this.useProxy = useProxy;
    }

    public String getIdsystem() {
        return idsystem;
    }

    public void setIdsystem(String idsystem) {



        this.idsystem = idsystem;
    }

    public String getUrlsystem() {
        return urlsystem;
    }

    public void setUrlsystem(String urlsystem) {
        this.urlsystem = urlsystem;
    }

    public String getSystemtype() {
        return systemtype;
    }

    public void setSystemtype(String systemtype) {
        this.systemtype = systemtype;
    }

    public String getAnonymusUser() {

        if (!anonymusUser.equalsIgnoreCase("true"))
            return anonymusUser;

        else
            return "";

    }

    public void setAnonymusUser(String anonymusUser) {
        this.anonymusUser = anonymusUser;
    }

    public String getCircResponseType() {
        return circResponseType;
    }

    public void setCircResponseType(String circResponseType) {
        this.circResponseType = circResponseType;
    }

    public String getCircQueryParameter() {
        return circQueryParameter;
    }

    public void setCircQueryParameter(String circQueryParameter) {
        this.circQueryParameter = circQueryParameter;
    }

    public String getProperties(){
        return  "\nidsystem=" + idsystem + " / urlSystem=" + urlsystem + " / systemtype=" + systemtype + 
                " / anonymusUser=" + anonymusUser + " / responsetype=" + circResponseType + " / queryParameter=" + circQueryParameter +
                "apiKey= " + apiKey;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
