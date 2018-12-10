package org.swissbib.extern.xSwissBib.librarysystems.aleph.filter;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamReader;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateResponse;

/**
 * Created by Project SwissBib, www.swissbib.org.
 * Author: Günter Hipler
 * Date: 18.12.2009
 * Time: 12:17:02
 */
public class AlephStreamFilter implements StreamFilter {

    protected String sysnumber = null;

    public boolean accept(XMLStreamReader reader) {
        return false;  //derivates of this class has to implement the specific parser logic
    }

    public AlephStreamFilter(String sysNumber) {
        this.sysnumber = sysNumber;
    }

    public CirculationStateResponse getCircStateResponse() {
        return null;
    }

    
}
