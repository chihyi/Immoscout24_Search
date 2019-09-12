//package de.immobilienscout24.example.consumer.webapp.tester;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import sun.jvm.hotspot.debugger.posix.elf.ELFException;

public class OauthTesterIS24 { public static void main(String[] args) throws Exception{

    OAuthConsumer consumer = new DefaultOAuthConsumer("Streitel-Immobilien-SucheKey", "GKYAbMEDamTVue0P");
    OAuthProvider provider = new DefaultOAuthProvider("https://rest.immobilienscout24.de/restapi/security/oauth/request_token", "https://rest.immobilienscout24.de/restapi/security/oauth/access_token", "https://rest.immobilienscout24.de/restapi/security/oauth/confirm_access");

    System.out.println("Fetching request token...");

    String authUrl = provider.retrieveRequestToken(consumer, "http://www.google.de");

    String requestToken = consumer.getToken();

    String requestTokenSecret = consumer.getTokenSecret();

    System.out.println("Request token: " + requestToken);

    System.out.println("Token secret: " + requestTokenSecret);

    System.out.println("Now visit:\n" + authUrl + "\n... and grant this app authorization");

    System.out.println("Enter the verification code and hit ENTER when you're done:");

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    String verificationCode = br.readLine();

    System.out.println("Fetching access token...");


    provider.retrieveAccessToken(consumer, verificationCode.trim());

    String accessToken = consumer.getToken();

    String accessTokenSecret = consumer.getTokenSecret();

    System.out.println("Access token: " + accessToken);

    System.out.println("Token secret: " + accessTokenSecret);

    System.out.println("first call"); requestObjectApi(consumer);

    System.out.println("second call"); requestObjectApi(consumer);

    System.out.println("third call"); OAuthConsumer consumer2 = new DefaultOAuthConsumer("Streitel-Immobilien-SucheKey", "GKYAbMEDamTVue0P");

    consumer2.setTokenWithSecret(accessToken, accessTokenSecret);

    requestObjectApi(consumer2);
    }



    private static void requestObjectApi(OAuthConsumer consumer) throws MalformedURLException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, UnsupportedEncodingException {

        System.out.println("#################################################################################################");

        URL url = new URL("https://rest.immobilienscout24.de/restapi/api/search/v1.0/search/region?realestatetype=apartmentbuy&geocodes=1276003");

        HttpURLConnection apiRequest = (HttpURLConnection) url.openConnection();
        apiRequest.setRequestProperty("Accept","application/json");

        consumer.sign(apiRequest);

        System.out.println("Sending request...");

        apiRequest.connect();

        System.out.println("Expiration " + apiRequest.getExpiration());

        System.out.println("Timeout " + apiRequest.getConnectTimeout());

        System.out.println("URL " + url);

        System.out.println("Method " + apiRequest.getRequestMethod());

        System.out.println("Response: " + apiRequest.getResponseCode() + " " + apiRequest.getResponseMessage());

        InputStream inputStream = apiRequest.getInputStream();
        String response = getStringFromStream(inputStream);
        System.out.println(response);

        /*try {

            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputStream);
            System.out.println("Root element:" + document.getRootElement().getName());
            //Element rootElement = document.getRootElement();

            //List<Element> immobilienList = rootElement.getChildren();
            System.out.println("#################################################################################################");
            
            //Element immobilien = immobilienList.get(0);
            //System.out.println("\nCurrent Element:" + immobilien.getName());
            //System.out.println("Immobilien Contact Salutation: "+immobilien.getChild("salutation").getText());

           /* for (int tmp = 0; tmp < immobilienList.size(); tmp++){
                Element immobilien = immobilienList.get(tmp);
                System.out.println("\nCurrent Element:" + immobilien.getName());
                Attribute attribute = immobilien.getAttribute("salutation");
                System.out.println("Immobilien Contact Salutation: "+attribute.getValue());
                System.out.println("First Name: "+ immobilien.getChild("firstname").getText());
            }

        }
        catch (JDOMException e){
            e.printStackTrace();
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }

        System.out.println("#################################################################################################");
            */
    }


    private static String getStringFromStream(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[2048];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                int counter;
                while ((counter = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, counter);
                }
            } finally {
                inputStream.close();
            }
            return writer.toString();
        } else {
            return "No Contents";
        }
    }



}