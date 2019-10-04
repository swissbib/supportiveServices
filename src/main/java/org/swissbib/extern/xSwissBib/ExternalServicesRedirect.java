package org.swissbib.extern.xSwissBib;

import org.apache.log4j.Logger;
import org.swissbib.extern.xSwissBib.services.ServiceDescription;
import org.swissbib.utilities.web.HTTPConnectionHandling;
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
import java.io.*;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: swissbib
 * Date: 3/16/12
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExternalServicesRedirect extends HttpServlet {

    private static HashMap<String,ServiceDescription> serviceRegister = null;

    private final static Logger redirectLog = Logger.getLogger(ExternalServicesRedirect.class);



    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request,response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        try {

            redirectLog.debug("start processing redirect");


            //HashMap<String,String> parameterMap = (HashMap<String,String>) request.getParameterMap();

            Map parameterMap =  request.getParameterMap();

            //http://stackoverflow.com/questions/16163874/get-real-client-ip-in-a-servlet
            //http://www.javabeat.net/client-ip-address-servlet/
            //http://java-monitor.com/forum/showthread.php?t=43


            String serviceName;

            if (parameterMap.containsKey("reqServicename")) {

                Object oName = parameterMap.get("reqServicename");

                if (oName instanceof String[]) {
                    String[] tArray = (String[]) oName;
                    serviceName = tArray[0];

                } else {
                    serviceName = (String) oName;
                }

            } else {
                serviceName = "default";
            }


            //String serviceName = parameterMap.containsKey("servicename") ? (String) parameterMap.get("servicename") : "default";

            redirectLog.debug("service name: " + serviceName);

            ServiceDescription requestedService = null;

            if (serviceName.equalsIgnoreCase("default")) {

                for (ServiceDescription sD : serviceRegister.values()) {
                    if (sD.isDefaultService()) {
                        requestedService = sD;
                        break;
                    }
                }


            } else {
                for (ServiceDescription sD : serviceRegister.values()) {

                    if (sD.getServiceName().equalsIgnoreCase(serviceName)) {
                        requestedService = sD;
                        break;
                    }
                }

            }



            if (null != requestedService) {


                redirectLog.debug("resquested Service: " + requestedService);

                //response.sendRedirect(requestedService.buildServiceRequest(parameterMap));



                HTTPConnectionHandling connectionHandling = new HTTPConnectionHandling();

                String nURL =  requestedService.buildServiceRequest(request.getQueryString());
                HttpURLConnection connection = connectionHandling.getHTTPConnection(nURL,false);


                redirectLog.debug("got connection for: " + nURL);

                //URLImageSource is = (URLImageSource) connection.getContent();

                InputStream is =  connection.getInputStream();



                //new: disconnect
                connection.disconnect();
                //background: http://stackoverflow.com/questions/11056088/do-i-need-to-call-httpurlconnection-disconnect-after-finish-using-it

                redirectLog.debug("got connection for: " + nURL);
                redirectLog.debug("content type for remote service: " + connection.getContentType());

                //reader = conn.getInputStream();
                //int available = reader.available()

                //int availableBytes =  is.available();


                //byte[] data = new byte[availableBytes];
                //is.read(data,0,availableBytes);

                //InputStream is=request.getInputStream();

                //ImageIO.write(is)

                OutputStream os=response.getOutputStream();
                byte[] buf = new byte[1000];
                for (int nChunk = is.read(buf); nChunk!=-1; nChunk = is.read(buf))
                {
                    os.write(buf, 0, nChunk);
                }

                response.setContentType(connection.getContentType());



                //String forwardedResponse =  new Scanner( is ).useDelimiter( "\\Z" ).next();

                //redirectLog.debug("forwarded response: ");
                //redirectLog.debug(forwardedResponse);
                //response.setContentType(connection.getContentType());

                //response.getWriter().println(forwardedResponse);




            }  else {
                response.setContentType("text/html");

                PrintWriter out = response.getWriter( );

                StringBuilder errortext = new StringBuilder();
                errortext.append("<html>");
                errortext.append("<body>");
                errortext.append("<div>Didn't find any service for your request</div>");
                errortext.append("</body>");
                errortext.append("</html>");

                redirectLog.debug(errortext.toString());


                out.println(errortext);
                //out.println(StringEscapeUtils.escapeHtml(response));
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE );

        }
        } catch (Exception exception ) {

            redirectLog.debug(exception);

        }



        
        
        
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

        redirectLog.debug("init of Redirection Service started");


        String targetServer = servletConfig.getInitParameter("targetServer");

        redirectLog.debug("targetServer: " + targetServer);


        String serviceConfiguration = servletConfig.getInitParameter("serviceconfiguration");

        redirectLog.debug("serviceConfiguration: " + serviceConfiguration);


        InputStream in = servletConfig.getServletContext().getResourceAsStream(serviceConfiguration);

        DocumentBuilderFactory factory;
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder documentBuilder = null;


        try {
            documentBuilder = factory.newDocumentBuilder();
            Document document = documentBuilder.parse(in);
            Element docEl = document.getDocumentElement();

            HashMap<String,ServiceDescription> mapServiceregister  = new HashMap<String,ServiceDescription>(10);
            NodeList services = docEl.getElementsByTagName("service");

            for (int i = 0; i < services.getLength();i++) {
                Node service = services.item(i);
                NodeList serviceAttributes = service.getChildNodes();
                String servicename = null;
                boolean defaultService = false;
                String contextName = null;

                for (int j = 0; j < serviceAttributes.getLength();j++) {
                    Node attribute = serviceAttributes.item(j);
                    String localName = attribute.getNodeName();
                    if (localName.equals("servicename")) {
                        servicename =  attribute.getTextContent();
                    } else if (localName.equals("default")) {
                        defaultService = Boolean.valueOf( attribute.getTextContent());
                    } else if (localName.equals("contextname")) {
                        contextName = attribute.getTextContent();
                    }
                }


                ServiceDescription sD = new ServiceDescription(servicename,
                                                                defaultService,
                                                                contextName,
                                                                targetServer);
                redirectLog.debug("description od service: " + servicename + " was read");

                mapServiceregister.put(servicename,sD);
            }

            ExternalServicesRedirect.serviceRegister = mapServiceregister;




        } catch (ParserConfigurationException pE) {
            //todo: better logging
            pE.printStackTrace();
            

        }catch (IOException ioE) {
            //todo: better logging
            ioE.printStackTrace();
        }catch (SAXException sE)  {
            //todo: better logging
            sE.printStackTrace();
        }


        super.init(servletConfig);



    }
    
    
}
