package org.swissbib.extern.xSwissBib.services;

import java.util.Enumeration;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: swissbib
 * Date: 3/16/12
 * Time: 10:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceDescription {



    /*<service>
    <servicename>AvailabilityRequest</servicename>
    <default>TRUE</default>
    <contextname>TouchPoint</contextname>
    </service>
    */
    
    
    private String serviceName = null;
    private boolean defaultService = false;
    private String contextName = null;
    private String targetServer = null;
    
    
    public ServiceDescription(String servicename,
                              boolean defaultService,
                              String contextName,
                              String targetServer) {
        
        
        this.serviceName = servicename;
        this.defaultService = defaultService;
        this.contextName = contextName;
        this.targetServer = targetServer;
        
    }


    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isDefaultService() {
        return defaultService;
    }

    public void setDefaultService(boolean defaultService) {
        this.defaultService = defaultService;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }
    
    
    public String buildServiceRequest (HashMap<String,String> queryParameter) {

        StringBuilder redirectURL = new StringBuilder();

        redirectURL.append("http://").append(this.getTargetServer()).append("/").
                append(this.getContextName()).append("/").append(this.getServiceName()).append("?");

        boolean first = true;
        for (String key : queryParameter.keySet()) {
            if (!key.equalsIgnoreCase("servicename")) {
                if (first) {
                    redirectURL.append(key).append("=").append(queryParameter.get(key));
                    first = false;
                } else {
                    redirectURL.append("&");
                    redirectURL.append(key).append("=").append(queryParameter.get(key));
                }
            }

        }
        return redirectURL.toString();
    }


    public String buildServiceRequest (String queryParameter) {

        StringBuilder redirectURL = new StringBuilder();

        redirectURL.append("http://").append(this.getTargetServer()).append("/").
                append(this.getContextName()).append("/").append(this.getServiceName()).append("?").append(queryParameter);

        return redirectURL.toString();
    }


}
