//package de.immobilienscout24.example.consumer.webapp.tester;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.Gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import sun.jvm.hotspot.debugger.posix.elf.ELFException;

public class OauthTesterIS24 {

    public static void main(String[] args) throws Exception{

    //OauthGetTokenWithSecret();
    OAuthConsumer consumer = new DefaultOAuthConsumer("Streitel-Immobilien-SucheKey","GKYAbMEDamTVue0P");
    consumer.setTokenWithSecret("1119e9e5-d4e8-4838-be1e-37eb63034627", "tffH/7toC6rmL/Ho4f3SvdF6IJ/+kLKwE/4tJsiKMEbfSyYeTNAEiU/NMrS/i4jInn+uSXWFFsgfftQEWimPxaOHrG3HIgjphEdxSynUaIo=");
    searchNewInterestingObject(consumer);
    }

    private static void searchNewInterestingObject(OAuthConsumer consumer) throws IOException, OAuthCommunicationException, OAuthMessageSignerException, OAuthExpectationFailedException{

        System.out.println("#################################################################################################");

        double rentBerlin = 8.4;


        String urlBerlinString = "https://rest.immobilienscout24.de/restapi/api/search/v1.0/search/region?realestatetype=apartmentbuy&geocodes=1276003&price=0-150000&livingspace=50-1000";
        String responseString = getResponseString(urlBerlinString,consumer);

        Gson gson = new Gson();
        JsonObject responseJson = gson.fromJson(responseString, JsonObject.class);

        int numberOfHits = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonObject("paging").getAsJsonPrimitive("numberOfHits").getAsInt();
        System.out.println("number of Hits: " + numberOfHits);
        int pageSize = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonObject("paging").getAsJsonPrimitive("pageSize").getAsInt();
        System.out.println("pageSize: " + pageSize);
        int numberOfPages = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonObject("paging").getAsJsonPrimitive("numberOfPages").getAsInt();
        System.out.println("numberOfPages: " + numberOfPages);

        // Search first page
        getInterestingObjectFromOnePage(consumer, responseJson, pageSize, rentBerlin);

        // Search pages from 2. to the last
        for (int i = 2; i <= numberOfPages; i++){

            System.out.println("page number: " + i);
            String urlString = urlBerlinString + "&pagenumber=" + String.valueOf(i);

            responseJson = gson.fromJson(getResponseString(urlString,consumer), JsonObject.class);
            pageSize = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonObject("paging").getAsJsonPrimitive("pageSize").getAsInt();

            getInterestingObjectFromOnePage(consumer, responseJson, pageSize, rentBerlin);
        }

    }

    private static void getInterestingObjectFromOnePage(OAuthConsumer consumer, JsonObject responseJson, int pageSize, double rent) throws OAuthExpectationFailedException, OAuthCommunicationException, OAuthMessageSignerException, IOException {

        JsonArray resultlistEntry = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonArray("resultlistEntries").get(0).getAsJsonObject().getAsJsonArray("resultlistEntry");
        for (int i = 0; i < pageSize; i++) {
            JsonObject realEstate = resultlistEntry.get(i).getAsJsonObject().getAsJsonObject("resultlist.realEstate");
            int price = realEstate.getAsJsonObject("price").getAsJsonPrimitive("value").getAsInt();
            int livingSpace = realEstate.getAsJsonPrimitive("livingSpace").getAsInt();
            double rentalYield = 12 * livingSpace * rent / price;
            if (rentalYield > 0.06) {
                int realEstateId = resultlistEntry.get(i).getAsJsonObject().getAsJsonPrimitive("realEstateId").getAsInt();
                String realEstateUrl = "https://www.immobilienscout24.de/expose/" + String.valueOf(realEstateId);
                System.out.println(realEstateUrl);
            }
        }

    }

    private static String getResponseString(String urlString, OAuthConsumer consumer) throws IOException, OAuthCommunicationException, OAuthExpectationFailedException, OAuthMessageSignerException {
        URL urlBerlin = new URL(urlString);

        HttpURLConnection apiRequest = (HttpURLConnection) urlBerlin.openConnection();
        apiRequest.setRequestProperty("Accept", "application/json");

        consumer.sign(apiRequest);

        apiRequest.connect();

        InputStream inputStream = apiRequest.getInputStream();
        return getStringFromStream(inputStream);
    }

    private static void OauthGetTokenWithSecret() throws OAuthCommunicationException, OAuthExpectationFailedException, OAuthNotAuthorizedException, OAuthMessageSignerException, IOException {

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

        System.out.println("call: ");

        System.out.println(getResponseString("https://rest.immobilienscout24.de/restapi/api/search/v1.0/search/region?realestatetype=apartmentbuy&geocodes=1276003",consumer));
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