package org.swissbib.extern.xSwissBib;



import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: swissbib
 * Date: Nov 30, 2010
 * Time: 2:40:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageTransformer extends HttpServlet {

    //Test um ein Adam-Bild abzurufen
    //dies noch mit den sourcen auf chbtptst, die nicht mehr den hier abgeänderten für posters entsprechen.
    //http://chbtptst.oclcpica.org/TouchPoint/ImageTransformer?imagePath=http://www.idsluzern.ch/images/sosa/01/710.JPG&scale=1.0
    private String sPostersURL="http://ccsa.admin.ch/cgi-bin/hi-res/get_image.cgi?x=800&y=500&res=2&width=20&height=1000&filename=/data/dbadmin/htdocs/hi-res/images/sid/%s.sid";
    private final static Logger transformerLog = Logger.getLogger(ImageTransformer.class);

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        boolean bPosters = false;

        transformerLog.debug("start of processing transformer request");

        String urlToImage = request.getParameter("imagePath");

        if (request.getParameter("posterImage") != null) {

            bPosters = true;

            String tempArray[] = urlToImage.split("\\.");

            if (tempArray.length == 2) {
                urlToImage = String.format(sPostersURL,tempArray[0]);
                //System.out.println(urlToImage);
            } else {
                urlToImage = sPostersURL;
            }
        }
        else {
            String escapedImagePath = request.getParameter("escaped");
            if (Boolean.valueOf(escapedImagePath)) {
                Pattern replacePattern = Pattern.compile("ESCAPED");
                Matcher m = replacePattern.matcher(urlToImage);
                urlToImage = m.replaceAll("&");
            }
        }

        //System.out.println("imagePath: " + urlToImage);
        try {


            double scale = 0.8;
            if (!bPosters){
                try {
                    scale = Double.valueOf(request.getParameter("scale"));
                } catch (Exception numberExcep)  {
                    scale = 0.66;
                    numberExcep.printStackTrace();
                }
            }
            //we need a quick solution to suppress delivery of large pictures because of
            //law restrictions
            scale = 0.1;
            //urlToImage = urlToImage + ".jpg";

            transformerLog.debug("got request: " + urlToImage);

            URL url = new URL(urlToImage);

            //Iterator readers = ImageIO.getImageReadersByFormatName("dicom");
            //GIFImageReader reader = (GIFImageReader)readers.next();
            //BufferedImage bufI = reader.read();

            BufferedImage bufI = ImageIO.read(url);
            //String [] s = ImageIO.getReaderFormatNames();

            int width =   (int)(bufI.getWidth() * scale );
            int height = (int)(bufI.getHeight() * scale);

            Image img =  bufI.getScaledInstance(width,height,BufferedImage.TYPE_INT_ARGB);
            BufferedImage newImg = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);

            Graphics g = newImg.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();

            response.setContentType("image/jpeg");

            ImageIO.write(newImg,"jpeg",response.getOutputStream());
            //ImageIO.write(newImg,"jpeg",new File("/home/swissbib/newImage.jpg"));

        } catch (MalformedURLException urlEx) {
            transformerLog.debug(urlEx);
            urlEx.printStackTrace();
        } catch (IOException ioExc) {
            transformerLog.debug(ioExc);
            ioExc.printStackTrace();
        } catch (Exception ex) {
            transformerLog.debug(ex);
            ex.printStackTrace();
        } catch (Throwable thr) {
            transformerLog.debug(thr);
            thr.printStackTrace();
        }

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }
}
