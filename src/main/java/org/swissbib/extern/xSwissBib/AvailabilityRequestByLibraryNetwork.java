package org.swissbib.extern.xSwissBib;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateResponse;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateService;
import org.swissbib.extern.xSwissBib.services.circulation.responsemodel.AvailabilityStatus;
import org.swissbib.extern.xSwissBib.services.circulation.responsemodel.Institution;
import org.swissbib.extern.xSwissBib.services.common.LibraryProperties;
import org.swissbib.extern.xSwissBib.services.common.XServiceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Matthias Edel
 * Date: 22.08.2018
 */
public class AvailabilityRequestByLibraryNetwork extends HttpServlet{
    private static HashMap<String,LibraryProperties> libraryProperties;
    private static HashMap<String,Institution> mapInstitutions;
    private final static Logger availLog = Logger.getLogger("swissbibavail");
    private static String  proxyServer = null;

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        String requestURL =  httpServletRequest.getQueryString();

        availLog.debug("new request -> query String: " + httpServletRequest.getQueryString());
        Boolean debug = Boolean.valueOf(getInitParameter("DEBUG"));

        String sysnumber = httpServletRequest.getParameter("sysnumber");
        String idls = httpServletRequest.getParameter("idls");
        String language = httpServletRequest.getParameter("language") != null ? httpServletRequest.getParameter("language"): "en";

        String response = null;
        CirculationStateResponse circResponse = null;
        String catchedErrorMessage = null;
        JSONObject jsonResonse = null;

        try {
            CirculationStateService circService = new CirculationStateService(AvailabilityRequestByLibraryNetwork.libraryProperties,
                                                                                AvailabilityRequestByLibraryNetwork.mapInstitutions,
                                                                               AvailabilityRequestByLibraryNetwork.proxyServer);
            circResponse = circService.getCirculationStatusByLibraryNetwork(sysnumber,idls,debug,language);
            jsonResonse = createJsonResponse(circResponse,debug);

            availLog.debug("\nJSON Response for client");
            availLog.debug(jsonResonse.toJSONString());
        } catch (XServiceException xEx) {
            availLog.warn("Catching XServiceException while requesting getCirculationStatus", xEx);
            catchedErrorMessage = xEx.getMessage();
        } catch (Throwable th) {
            availLog.warn("Catching Throwable while requesting getCirculationStatus", th);
            catchedErrorMessage = th.getMessage();
        }
        finally {
            if (jsonResonse != null && catchedErrorMessage == null) {
                
                httpServletResponse.setContentType("application/json");
                PrintWriter out = httpServletResponse.getWriter();
                out.println(jsonResonse.toJSONString());

                if (circResponse.hasError()) {
                    availLog.error("Internal Server Error:\\nidls=" + idls + "\\nsysNr=" + sysnumber +
                            "\\n\\nError message:\\n" +circResponse.getErrorMessage() );
                    httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                }
            } else {

                    //todo: this is not correct
                    StringBuffer tempJSON = null;
                    if (jsonResonse != null) {
                        tempJSON = new StringBuffer();
                        tempJSON.append(response);
                    } else if (catchedErrorMessage != null) {
                        tempJSON = new StringBuffer();
                        tempJSON.append(catchedErrorMessage);
                    } else {
                        tempJSON = new StringBuffer();
                        tempJSON.append("{errorMsg:'error while trying to fetch online availabilty'}");
                    }

                    httpServletResponse.setContentType("text/json");
                    PrintWriter out = httpServletResponse.getWriter( );
                    out.println(response);
                    httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doGet(httpServletRequest,httpServletResponse);
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        String configLibrarySystems = servletConfig.getInitParameter("librarysystems");
        AvailabilityRequestByLibraryNetwork.proxyServer = servletConfig.getInitParameter("proxyAdress");

        String configXServerConfig = servletConfig.getInitParameter("xserverconfig");
        try {
            InputStream in = servletConfig.getServletContext().getResourceAsStream(configLibrarySystems);
            AvailabilityRequestByLibraryNetwork.libraryProperties = createLibraryProperties(in);
            in = servletConfig.getServletContext().getResourceAsStream(configXServerConfig);
            AvailabilityRequestByLibraryNetwork.mapInstitutions = createXServerConfig(in);

        } catch (ParserConfigurationException pE) {
            availLog.error("AvailabilityRequestByLibraryNetwork.init: ParserConfigurationException: ", pE);

        }catch (IOException ioE) {
            availLog.error("AvailabilityRequestByLibraryNetwork.init: IOException: ", ioE);
        }catch (SAXException sE)  {
            availLog.error("AvailabilityRequestByLibraryNetwork.init: SAXException: ", sE);
        }
        super.init(servletConfig); 
    }

    private HashMap<String,LibraryProperties> createLibraryProperties(InputStream in)
                                                                   throws ParserConfigurationException,
                                                                   SAXException,
                                                                   IOException    {
        DocumentBuilderFactory factory;
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder documentBuilder = null;

        documentBuilder = factory.newDocumentBuilder();
        Document document = documentBuilder.parse(in);
        Element docEl = document.getDocumentElement();

        HashMap<String,LibraryProperties> mapLibraryProperties  = new HashMap<String,LibraryProperties>(10);
        NodeList libraries = docEl.getElementsByTagName("library");

        for (int i = 0; i < libraries.getLength();i++) {
            Node library = libraries.item(i);
            NodeList libraryAttributes = library.getChildNodes();
            String idsystem = null;
            String urlsystem = null;
            String systemtype = null;
            String circresponsetype = null;
            String anonymusUser = null;
            String circQueryParameter = null;
            String apiKey = null;
            boolean useProxy = true;

            for (int j = 0; j < libraryAttributes.getLength();j++) {
                Node attribute = libraryAttributes.item(j);
                String localName = attribute.getNodeName();
                if (localName.equals("idsystem")) {
                    idsystem =  attribute.getTextContent();
                } else if (localName.equals("urlsystem")) {
                    urlsystem = attribute.getTextContent();
                } else if (localName.equals("systemtype")) {
                    systemtype = attribute.getTextContent();
                }
                else if (localName.equals("circresponsetype")) {
                    circresponsetype = attribute.getTextContent();
                }
                else if (localName.equals("anonymusUser")) {
                    anonymusUser = attribute.getTextContent();
                }
                else if (localName.equals("circQueryParameter")) {
                    circQueryParameter = attribute.getTextContent();
                }
                else if (localName.equals("apiKey")) {
                    apiKey = attribute.getTextContent();
                }
                else if (localName.equals("useProxy")) {
                    useProxy = Boolean.valueOf(attribute.getTextContent());
                }
            }

            LibraryProperties lp = new LibraryProperties(idsystem,urlsystem, systemtype,
                    anonymusUser,circQueryParameter,apiKey,useProxy);
            availLog.info("InitLibraryProperties" + lp.getProperties());

            lp.setCircResponseType(circresponsetype);
            mapLibraryProperties.put(idsystem,lp);
        }
        return mapLibraryProperties;
    }

    private HashMap<String,Institution> createXServerConfig(InputStream in)
            throws ParserConfigurationException,
            SAXException,
            IOException    {

        DocumentBuilderFactory factory;
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder documentBuilder = null;

        documentBuilder = factory.newDocumentBuilder();
        Document document = documentBuilder.parse(in);
        Element codedefinitions = document.getDocumentElement();
        HashMap<String,Institution> mapInstitutions  = new HashMap<String,Institution>();
        NodeList institutions = codedefinitions.getElementsByTagName("institution");

        for (int i = 0; i < institutions.getLength();i++) {
            String institutionName = institutions.item(i).getAttributes().getNamedItem("name").getTextContent();
            Institution institution = new Institution(institutionName);
            NodeList stati = institutions.item(i).getChildNodes();

            for (int j = 0; j < stati.getLength(); j++ ) {
                if (stati.item(j).getNodeType() == Node.ELEMENT_NODE ) {
                    Node status = stati.item(j);
                    String statusName = status.getNodeName();
                    AvailabilityStatus availStatus = new AvailabilityStatus(statusName);
                    NodeList statusAttributes = status.getChildNodes();
                    
                    for (int k=0; k < statusAttributes.getLength(); k++) {
                        String conjunction;
                        String mapField;
                        HashSet<String> mapValues = new HashSet<String>();
                        HashSet<String> additionalFieldsSet = new HashSet<String>();
                        Node statusAttr = statusAttributes.item(k);
                        if (statusAttr.getNodeType() == Node.ELEMENT_NODE ) {
                            String attributeLocalName = statusAttr.getNodeName();
                            if (attributeLocalName.equalsIgnoreCase("mapField")) {
                                mapField = statusAttr.getTextContent();
                                availStatus.setMapField(mapField);
                            } else if(attributeLocalName.equalsIgnoreCase("mapValues")) {
                                //availStatus.setMapValues(statusAttr.getTextContent().toLowerCase());
                                availStatus.setMapValues(statusAttr.getTextContent());
                            } else if (attributeLocalName.equalsIgnoreCase("additionalFields")) {
                                NodeList additionalFields = statusAttr.getChildNodes();
                                conjunction = statusAttr.getAttributes().getNamedItem("conjunction").getTextContent();
                                availStatus.setAdditionalFieldConjunction(conjunction);

                                for (int l=0; l<additionalFields.getLength();l++ ) {
                                    if (additionalFields.item(l).getNodeType() == Node.ELEMENT_NODE ) {
                                        String name = additionalFields.item(l).getNodeName();
                                        String fieldValue = additionalFields.item(l).getTextContent();
                                        availStatus.setAdditionalField(name,fieldValue);
                                    }
                                }
                            }
                        }
                    }
                    institution.addAvailabilityStatus(availStatus);
                }
            }
            mapInstitutions.put(institution.getName(),institution);
        }
        return mapInstitutions;
    }

    @SuppressWarnings("unchecked")
    private JSONObject createJsonResponse(CirculationStateResponse circResponse,
                                        Boolean debug) {
        //todo: Debugging
        JSONObject jsonResponse = new JSONObject();
        for (CirculationStateItem item : circResponse.getItemList()) {
            AbstractMap.SimpleEntry<String, Integer> subLibAvail = item.getSubLibraryAvailability();
            jsonResponse.put(subLibAvail.getKey(), subLibAvail.getValue().toString());
        }
        return jsonResponse;
    }
}
