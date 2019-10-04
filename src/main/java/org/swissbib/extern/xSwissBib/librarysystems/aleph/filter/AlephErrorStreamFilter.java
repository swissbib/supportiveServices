package org.swissbib.extern.xSwissBib.librarysystems.aleph.filter;

import org.apache.log4j.Logger;
import org.swissbib.extern.xSwissBib.services.common.XServiceException;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 21.12.2009
 * Time: 11:36:21
 */
public class AlephErrorStreamFilter  extends ErrorStreamFilter{

    private boolean rootCircStateElement = false;
    private boolean loginElement = false;
    private XServiceException xServiceException;
    private final static Logger availLog = Logger.getLogger("swissbibavail");


    public AlephErrorStreamFilter(String sysnumber) {
        super(sysnumber);

    }

    public XServiceException getXServiceException() {
        return xServiceException;
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
                case XMLStreamConstants.END_DOCUMENT:
                case XMLStreamConstants.END_ELEMENT:
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    String name = reader.getLocalName();
                    if (name.equalsIgnoreCase("circ-status")) {
                        //we got the correct rootElement within the XMLResponse
                        this.rootCircStateElement = true;
                        break;
                    } else if (name.equalsIgnoreCase("login")) {
                        this.loginElement = true;
                    } else if (name.equalsIgnoreCase("error") && this.rootCircStateElement) {
                        //in this combination an application specific error occured e.g. a document with the requested sysnumber wasn't found
                        /*
                            <circ-status>
                            <error>
                            Document: 7363763al doesn't exist in library: DSV51. Make sure you insert BIB library.
                            </error>
                            <session-id>Q4Y7YQHSGAY9LI5AQPU5HB7AI11D1Q5F2YD3SNP7NT1IMTXPS2</session-id>
                            </circ-status>
                         */
                        this.xServiceException = new XServiceException("Service Error: " + reader.getElementText());
                        toParse = true;
                    } else if (name.equalsIgnoreCase("error") && this.loginElement) {

                        this.xServiceException = new XServiceException("Login Error: " + reader.getElementText());
                        toParse = true;
                    } else if (name.equalsIgnoreCase("error")) {
                        this.xServiceException = new XServiceException("Error: " + reader.getElementText());
                        toParse = true;
                    break;
                }
            }
        } catch (XMLStreamException sEx){
            this.xServiceException = new XServiceException(sEx.getMessage());
            availLog.info("AlephErrorStreamFilter.accept: XMLStreamException: ", sEx);
            toParse = true;
        }
        catch (Exception ex ) {
            this.xServiceException = new XServiceException(ex.getMessage());
            availLog.info("AlephErrorStreamFilter.accept: Exception: ", ex);
            toParse = true;
        }

        catch (Throwable th) {
            this.xServiceException = new XServiceException(th.getMessage());
            availLog.info("AlephErrorStreamFilter.accept: Throwable: ", th);
            toParse = true;
        }


        return toParse;  //To change body of implemented methods use File | Settings | File Templates.

    }
}
