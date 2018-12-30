package org.swissbib.extern.xSwissBib.services.forwarder;

import com.google.common.net.UrlEscapers;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [...description of the type ...]
 * <p/>
 * <p/>
 * <p/>
 * Copyright (C) project swissbib, University Library Basel, Switzerland
 * http://www.swissbib.org  / http://www.swissbib.ch / http://www.ub.unibas.ch
 * <p/>
 * Date: 2/12/14
 * Time: 2:07 PM
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
public class ProtocolWrapper  extends HttpServlet {

    private final static Logger protocolWrapperLog = Logger.getLogger(ProtocolWrapper.class);
    private String ESCAPED_PATTERN = null;


    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        protocolWrapperLog.info("getting request: " + httpServletRequest.getQueryString());
        //String queryString = URLDecoder.decode(httpServletRequest.getQueryString(), "UTF-8");
        //String queryString1 = URLDecoder.decode(httpServletRequest.getParameter("targetURL"), "UTF-8");

        //String targetURL = URLEncoder.encode(httpServletRequest.getParameter("targetURL"),StandardCharsets.ISO_8859_1.toString());
        String targetURL = httpServletRequest.getParameter("targetURL");

        if (null != targetURL) {

            if (null != this.ESCAPED_PATTERN) {
                Pattern  pEscapedURL = Pattern.compile(this.ESCAPED_PATTERN);
                Matcher m = pEscapedURL.matcher(targetURL);
                if (m.find()) {
                    targetURL = m.replaceAll("&");
                    protocolWrapperLog.debug("targetURL after replacement: " + targetURL);
                }
            }

        } else {
            //in case we don't get an URL to be wrapped src will be assigned to the loaded page
            targetURL = "#";
        }

        //String prefix = "http://boris.unibe.ch/59490/1/";
        //String test = "TRANS KlÃ¶ti  Map Collecting in Switzerland_ENGSJT Rev 061013_TK SJT-1.pdf";
        //String queryS = "TRANS Klöti  Map Collecting in Switzerland_ENGSJT Rev 061013_TK SJT-1.pdf";

        //test = URLEncoder.encode(test, "UTF-8");
        protocolWrapperLog.info("target URL: " + targetURL);

        //targetURL = prefix + test;
        //StandardCharsets.ISO_8859_1
        //byte[] encoded = targetURL.getBytes(StandardCharsets.ISO_8859_1);
        //String iso_8859 =  new String(encoded);


        //String test5 = URLEncoder.encode(queryS, StandardCharsets.ISO_8859_1.toString());
        targetURL = UrlEscapers.urlFragmentEscaper().escape(targetURL);
        //targetURL = prefix + test5;

        httpServletResponse.sendRedirect(targetURL);

    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }


    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

        this.ESCAPED_PATTERN  = servletConfig.getInitParameter("ESCAPED_PATTERN");



    }





}
