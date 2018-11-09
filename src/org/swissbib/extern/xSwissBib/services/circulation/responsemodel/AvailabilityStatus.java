package org.swissbib.extern.xSwissBib.services.circulation.responsemodel;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.*;

/**
 * [...description of the type ...]
 * <p/>
 * <p/>
 * <p/>
 * Copyright (C) project swissbib, University Library Basel, Switzerland
 * http://www.swissbib.org  / http://www.swissbib.ch / http://www.ub.unibas.ch
 * <p/>
 * Date: 9/24/13
 * Time: 6:19 PM
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
public class AvailabilityStatus {

    private static final  String lendable_borrowed = "lendable_borrowed";
    private static final  String lendable_available = "lendable_available";


    protected static Pattern pDueDateLong = Pattern.compile("([0-9]{2}/[0-9]{2}/[0-9]{4,4})");
    protected static Pattern pDueDateShort = Pattern.compile("([0-9]{2}/[0-9]{2}/[0-9]{2,2})");
    protected static Pattern pDueDateCustomized = Pattern.compile("On Hold##Requested");
    protected static Pattern pisItemLost = Pattern.compile("(missing)|(removed)|(vermisst)");
    protected static Pattern pisItemNotBorrowed = Pattern.compile("(no loan)|(nicht ausleihbar)");
    protected static Pattern pnR = Pattern.compile("(\\d+)");

    protected static Pattern pDueDateEscape = Pattern.compile("\\\\#\\\\#");


    private final static Logger availLog = Logger.getLogger("swissbibavail");



    private String mapField;
    private HashSet<String> mapValuesAsString = new HashSet<String>();
    private HashSet<Pattern> mapValuesAsPattern = new LinkedHashSet<Pattern>();

    private String additionalFieldConjunction;

    private HashMap<String,String> additionalFields = new HashMap<String, String>();

    private String name;

    public AvailabilityStatus(String name) {

        this.name = name;


    }

    public String getMapField() {
        return mapField;
    }

    public void setMapField(String mapField) {
        this.mapField = mapField;
    }

    public HashSet<String> getMapValues() {
        return mapValuesAsString;
    }

    public void setMapValues(HashSet<String> mapValues) {
        this.mapValuesAsString = mapValues;
    }

    public void setMapValues(String mapValues) {

        this.mapValuesAsString.addAll(Arrays.asList(mapValues.split("##")));

        for (String mapValue : this.mapValuesAsString) {

            this.mapValuesAsPattern.add(Pattern.compile(mapValue, Pattern.CASE_INSENSITIVE));

        }




    }



    public String getAdditionalFieldConjunction() {
        return additionalFieldConjunction;
    }

    public void setAdditionalFieldConjunction(String additionalFieldConjunction) {
        this.additionalFieldConjunction = additionalFieldConjunction;
    }

    public HashMap<String, String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(HashMap<String, String> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public void setAdditionalField(String addFieldName, String addFieldValue) {
        this.additionalFields.put(addFieldName.toLowerCase(),addFieldValue.toLowerCase());
    }


    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public boolean match (CirculationStateItem item, String language) {
        boolean matched  = false;

        //try {
            //Class cls = Class.forName("org.swissbib.extern.xSwissBib.services.circulation.CirculationStateItem");
            //Method method = cls.getDeclaredMethod(this.mapField);
            //String definedField =  (String) method.invoke(item);

            JSONObject statusInformation= new JSONObject();
            //todo: use Patters

            boolean contains = false;

            for (Pattern p: this.mapValuesAsPattern) {
                Matcher matchedValue = p.matcher(item.getLoanState().trim());
                if (matchedValue.find() ){
                    contains = true;
                    break;
                }

            }

            //if (mapValuesAsString.contains(item.getLoanState().trim().toLowerCase())) {
        if (contains) {
            matched = true;


            if (this.name.equalsIgnoreCase("lendable")  ) {

                if ( this.additionalFields.size() > 0 ) {
                    //at the moment we don't care about the idea to define additional fields in configuration
                    //we assume to evaluate fix the three fields in Aleph
                    String duedate = item.getDueDate() != null ? this.prepareDueDate( item.getDueDate().trim()) : "";
                    String duehour = item.getDueHour() != null ? item.getDueHour().trim() : "";
                    String norequests = item.getNumberRequests() != null ? item.getNumberRequests().trim() : "";

                    if (this.hasDueDate(duedate)) {
                        duedate = this.formatDueDate(duedate,language);
                        norequests = this.numberRequests(norequests);



                        statusInformation.put("statusfield",AvailabilityStatus.lendable_borrowed);

                        JSONObject borrowingInformation = new JSONObject();
                        //todo: better format
                        borrowingInformation.put("due_date",duedate);
                        borrowingInformation.put("due_hour",duehour);
                        //if zero then clients will present 0
                        borrowingInformation.put("no_requests",(norequests.equalsIgnoreCase("0")) ? 0 : norequests);
                        statusInformation.put("borrowingInformation",borrowingInformation);

                    } else {
                        //we need this case for onhold##requested where we don't have a due date

                        statusInformation.put("statusfield",AvailabilityStatus.lendable_available);

                    }



                } else {
                    //shouldn't be the case - but anyway
                    statusInformation.put("statusfield",AvailabilityStatus.lendable_available);

                }


            } else {

                String duedate = item.getDueDate() != null ? item.getDueDate().trim() : "";
                String duehour = item.getDueHour() != null ? item.getDueHour().trim() : "";
                String norequests = item.getNumberRequests() != null ? item.getNumberRequests().trim() : "";

                if (this.hasDueDate(duedate)) {

                    duedate = this.formatDueDate(duedate,language);
                    norequests = this.numberRequests(norequests);



                    statusInformation.put("statusfield",this.name);

                    JSONObject borrowingInformation = new JSONObject();
                    //todo: better format
                    borrowingInformation.put("due_date",duedate);
                    borrowingInformation.put("due_hour",duehour);
                    //if zero then clients will present 0
                    borrowingInformation.put("no_requests",(norequests.equalsIgnoreCase("0")) ? 0 : norequests);
                    statusInformation.put("borrowingInformation",borrowingInformation);

                } else {

                     statusInformation.put("statusfield",this.name);
                }

            }


            item.setItemStatusInfomation(statusInformation);

        } else if (this.name.equalsIgnoreCase("lookOnSite")) {
            //we have the fallback
            statusInformation.put("statusfield",this.name);
            item.setItemStatusInfomation(statusInformation);

        }


        return matched;

    }


    protected boolean hasDueDate(String stringToAnalyze) {

        boolean hasDateInTagField = AvailabilityStatus.pDueDateLong.matcher(stringToAnalyze).find() ||
                AvailabilityStatus.pDueDateShort.matcher(stringToAnalyze).find(); //||
                //AvailabilityStatus.pDueDateCustomized.matcher(stringToAnalyze).find();

        if (!hasDateInTagField) {
            //search for a defined value in dueDate


            hasDateInTagField = searchForDefinedDueDate(stringToAnalyze) != null;



        }

        return hasDateInTagField;



    }


    private HashMap<String, String> searchForDefinedDueDate(String currentDueDate)  {

        HashMap<String,String> searchedValues = null;


        if (this.getAdditionalFields().containsKey("due-date") && this.getAdditionalFields().get("due-date") != null &&
                ! this.getAdditionalFields().get("due-date").equalsIgnoreCase("")) {


            String[] definedDueDates =  this.getAdditionalFields().get("due-date").split("##");

            for (String definedDate: definedDueDates) {
                Pattern pTemp = Pattern.compile ("(.*?)\\{(.*?)\\}");

                Matcher m = pTemp.matcher(definedDate);
                if (m.find()) {


                    String definedPattern = m.group(1);
                    String definedValuesForPattern = m.group(2);
                    if (definedPattern != null && !definedPattern.equalsIgnoreCase("") &&
                            this.prepareDueDate(definedPattern).equalsIgnoreCase(currentDueDate)) {
                        searchedValues = new HashMap<String, String>();
                        searchedValues.put(definedPattern,definedValuesForPattern);
                        break;

                    }


                }


            }

        }

        return searchedValues;

    }


    protected String formatDueDate(String stringToFormat, String language){

        StringBuilder display = new StringBuilder();

        Matcher m =  AvailabilityStatus.pDueDateLong.matcher(stringToFormat);

        if (AvailabilityStatus.pDueDateLong.matcher(stringToFormat).find()){
            String dueDate = m.group();
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            try {
                Date t = formatter.parse(dueDate);

                DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, this.getLocal(language));
                display.append(StringEscapeUtils.escapeHtml(df.format(t)));

            } catch (ParseException pE) {
                availLog.info("CirculationResponseFormatterformatDueDate: ParseException: ", pE);
            }

        }else {
            m = AvailabilityStatus.pDueDateShort.matcher(stringToFormat);
            if (m.find()) {

                String dueDate = m.group();
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
                try {
                    Date t = formatter.parse(dueDate);
                    DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, this.getLocal(language));
                    display.append(StringEscapeUtils.escapeHtml(df.format(t)));

                } catch (ParseException pE) {
                    availLog.info("CirculationResponseFormatterformatDueDate: ParseException: ", pE);
                }
            } else {

                //due date doesn't contain any information which could be defined as date
                //for duch a csae we are going to use the predefined in xservceAvailability

                //Rest in format string
                //testen: http://localhost/vufind/Record/281425906?lng=de

                HashMap <String, String> values =  searchForDefinedDueDate(stringToFormat);

                if (null != values ) {

                    String languagesToUse = "";

                    Iterator<String> definedValue = values.values().iterator();
                    while (definedValue.hasNext())  {
                        languagesToUse = definedValue.next();
                    }


                    String[] languages = languagesToUse.split("#");

                    String fallback = null;

                    String definedString = null;

                    for (String tLanguage: languages) {

                        String [] languageValue = tLanguage.split("!");



                        if (languageValue.length == 2) {

                            if (languageValue[0].equalsIgnoreCase("de")) {

                                fallback = languageValue[1];
                            }

                            if (languageValue[0].equalsIgnoreCase(language)) {
                                definedString = languageValue[1];
                                break;
                            }

                        }


                    }

                    if (null != definedString) {
                        display.append(definedString);
                    } else if (null != fallback) {
                        display.append(fallback);
                    } else {
                        display.append(stringToFormat);

                    }



                } else {

                    //it up to the client to handle the xServerResponse value in dueDate
                    display.append(stringToFormat);
                }

            }
        }

        return display.toString();

    }

    protected Locale getLocal(String language) {
        Locale l = null;

        if (language.equalsIgnoreCase("fr")) {
            l = Locale.FRENCH;
        }else if (language.equalsIgnoreCase("it")){
            l = Locale.ITALIAN;
        }else if (language.equalsIgnoreCase("en")){
            l = Locale.ENGLISH;
        } else {
            l = Locale.GERMAN;
        }
        return l;
    }

    protected String numberRequests(String response) {
        String count = "0";

        Matcher numbR = pnR.matcher(response);
        if (numbR.find() ){
            count = numbR.group(0);
        }

        return count;
    }


    private String prepareDueDate(String rawDueDate)  {

        return  pDueDateEscape.matcher(rawDueDate).replaceAll("##");

    }


}
