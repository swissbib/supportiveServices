package org.swissbib.extern.xSwissBib.services.circulation.responsemodel;

import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem;

import java.util.HashMap;

/**
 * [...description of the type ...]
 * <p/>
 * <p/>
 * <p/>
 * Copyright (C) project swissbib, University Library Basel, Switzerland
 * http://www.swissbib.org  / http://www.swissbib.ch / http://www.ub.unibas.ch
 * <p/>
 * Date: 9/24/13
 * Time: 6:05 PM
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * <p/>
 * license:  http://opensource.org/licenses/gpl-2.0.php GNU General Public License
 *
 * @author Guenter Hipler  <guenter.hipler@unibas.ch>
 * @link http://www.swissbib.org
 * @link https://github.com/swissbib/xml2SearchDoc
 */
public class Institution {

    private String name;

    private HashMap<String,AvailabilityStatus> availabilityStati = new HashMap<String, AvailabilityStatus>();
    private AvailabilityStatus fallBackStatus = null;

    public Institution(String name) {

        this.name = name;

    }

    public void addAvailabilityStatus (AvailabilityStatus status) {

        if (status.getName().equalsIgnoreCase("lookOnSite")) {
            this.fallBackStatus = status;
        } else {
            availabilityStati.put(status.getName(),status);
        }

    }

    public HashMap<String,AvailabilityStatus> getAvailabilityStati ()   {

        return this.availabilityStati;

    }

    public String getName() {
        return name;
    }

    public void formatItem (CirculationStateItem item, String language) {

        boolean fallback = true;

        for (AvailabilityStatus status: availabilityStati.values()) {

            if (status.match(item, language) ) {
                fallback = false;
                break;
            }

        }

        if (fallback) {
            fallBackStatus.match(item, language);
        }

    }
}
