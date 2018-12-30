package org.swissbib.extern.xSwissBib;



import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
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

    private static ArrayList<AllowedCover> coversToMatch = new ArrayList<AllowedCover>();

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


            double scale = 0.2;
            if (!bPosters){
                try {
                    scale = Double.valueOf(request.getParameter("scale"));
                } catch (Exception numberExcep)  {
                    scale = 0.2;
                    numberExcep.printStackTrace();
                }
            }
            //we need a quick solution to suppress delivery of large pictures because of
            //law restrictions
            boolean allowedCover = false;
            for (AllowedCover aC: coversToMatch) {
                if (aC.urlRegEx.matcher(urlToImage).find()) {
                    allowedCover = true;
                    try {
                        scale = Double.valueOf(aC.scale);

                    } catch (Exception ex) {
                        transformerLog.error("error using scale from white list", ex);
                        scale = 0.2;
                        allowedCover = false;
                    }
                    break;
                }
            }
            scale = allowedCover || scale <= 0.2 ? scale : 0.2;
            //urlToImage = urlToImage + ".jpg";

            transformerLog.debug("got request: " + urlToImage);

            InputStream is = performRequest(urlToImage);

            //URL url = new URL(is);

            //Iterator readers = ImageIO.getImageReadersByFormatName("dicom");
            //GIFImageReader reader = (GIFImageReader)readers.next();
            //BufferedImage bufI = reader.read();

            BufferedImage bufI = ImageIO.read(is);
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

    @Override
    public void init(ServletConfig config) throws ServletException {

        String whiteListWithIconSize = config.getInitParameter("whiteListScaleIcons");

        InputStream whiteListStream = ImageTransformer.class.getClassLoader().getResourceAsStream(whiteListWithIconSize);
        if (null != whiteListStream) {
            Properties coverWhiteList  = new Properties();
            try {
                coverWhiteList.load(whiteListStream);
                Iterator<String> propertyIterator = coverWhiteList.stringPropertyNames().iterator();
                while (propertyIterator.hasNext()) {
                    AllowedCover tC = new AllowedCover();
                    tC.coverURL = propertyIterator.next();
                    tC.scale = coverWhiteList.getProperty(tC.coverURL);
                    tC.urlRegEx = Pattern.compile(tC.coverURL);
                    coversToMatch.add(tC);

                }
            } catch (IOException ioException ) {
                transformerLog.error("error loading cover properties", ioException);
            }
        }
        super.init(config);
    }

    private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    private InputStream performRequest(String urlToImage) {

        //testlink for redirect to https
        //https://externalservices.swissbib.ch/services/ImageTransformer?imagePath=http://www.e-rara.ch/titlepage/doi/10.3931/e-rara-26903/128&scale=1
        //http://localhost:8080/services/ImageTransformer?imagePath=http://biblio.unibe.ch/adam/karten/ZB_Kart_413_11.jpg&scale=1
        //http://localhost:8080/services/ImageTransformer?imagePath=http://www.e-rara.ch/titlepage/doi/10.3931/e-rara-26903/128&scale=1
        //https://stackoverflow.com/questions/1201048/allowing-java-to-use-an-untrusted-certificate-for-ssl-https-connection
        URL resourceUrl, base, next;
        HttpURLConnection conn = null;
        String location;

        //try {
        //    SSLContext ctx = SSLContext.getInstance("TLS");
        //    ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
        //    SSLContext.setDefault(ctx);
        //} catch (NoSuchAlgorithmException | KeyManagementException exc) {
        //    exc.printStackTrace();
        //}

        //it's really a qick hack to support redirects
        //haven't had enough time to make it proper and tested

        try {
            while (true) {
                resourceUrl = new URL(urlToImage);
                conn = (HttpURLConnection) resourceUrl.openConnection();

                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setInstanceFollowRedirects(false);   // Make the logic below easier to detect redirections
                conn.setRequestProperty("User-Agent", "Mozilla/5.0...");

                switch (conn.getResponseCode()) {
                    case HttpURLConnection.HTTP_MOVED_PERM:
                    case HttpURLConnection.HTTP_MOVED_TEMP:
                        location = conn.getHeaderField("Location");
                        location = URLDecoder.decode(location, "UTF-8");
                        base = new URL(urlToImage);
                        next = new URL(base, location);  // Deal with relative URLs
                        urlToImage = next.toExternalForm();
                        continue;
                }

                break;
            }
        } catch (IOException exception) {

            exception.printStackTrace();
            conn = null;

        }

        InputStream ios = null;

        if (null != conn) {
            try {
                ios = conn.getInputStream();
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
            }
        }
        //todo use optional for this case
        return ios;
    }


}

class AllowedCover {
    public String coverURL;
    public Pattern urlRegEx;
    public String scale;
}
