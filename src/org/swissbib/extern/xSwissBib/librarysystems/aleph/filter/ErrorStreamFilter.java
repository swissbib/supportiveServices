package org.swissbib.extern.xSwissBib.librarysystems.aleph.filter;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamReader;


/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 21.12.2009
 * Time: 11:28:06
 */
public class ErrorStreamFilter implements StreamFilter {

    protected String sysnumber = null;

    public boolean accept(XMLStreamReader reader) {
        return true;  //derivates of this class has to implement the specific parser logic
                       //in case of error true will be returned
                    //so - if no error occurs the specific implementation class has to return false for all the nodes
    }

    public ErrorStreamFilter(String sysNumber) {
        this.sysnumber = sysNumber;

    }
}
