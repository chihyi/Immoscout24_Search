//package de.immobilienscout24.example.consumer.webapp.tester;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

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
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import sun.jvm.hotspot.debugger.posix.elf.ELFException;

import java.text.SimpleDateFormat;
import java.util.Date;


public class OauthTesterIS24 {

    public static void main(String[] args) throws Exception {

        //OauthGetTokenWithSecret();
        OAuthConsumer consumer = new DefaultOAuthConsumer("Streitel-Immobilien-SucheKey", "GKYAbMEDamTVue0P");
        consumer.setTokenWithSecret("1119e9e5-d4e8-4838-be1e-37eb63034627", "tffH/7toC6rmL/Ho4f3SvdF6IJ/+kLKwE/4tJsiKMEbfSyYeTNAEiU/NMrS/i4jInn+uSXWFFsgfftQEWimPxaOHrG3HIgjphEdxSynUaIo=");
        searchNewInterestingObject(consumer);
        //debugFunc(consumer);
    }

    private static void debugFunc(OAuthConsumer consumer) throws OAuthExpectationFailedException, OAuthCommunicationException, OAuthMessageSignerException, IOException, ParseException {
        String datetimeString1 = "2019-09-17T11:15:41.000+02:00";
        String datetimeString2 = "2019-09-18T11:15:41.000+02:00";
        Date date1=new SimpleDateFormat("yyyy-MM-dd").parse(datetimeString1);
        Date date2=new SimpleDateFormat("yyyy-MM-dd").parse(datetimeString2);
        if (date1.compareTo(date2) > 0) {
            System.out.println("Date1 is after Date2");
        } else if (date1.compareTo(date2) < 0) {
            System.out.println("Date1 is before Date2");
        } else if (date1.compareTo(date2) == 0) {
            System.out.println("Date1 is equal to Date2");
        } else {
            System.out.println("How to get here?");
        }

    }

    private static void searchNewInterestingObject(OAuthConsumer consumer) throws Exception {

        System.out.println("#################################################################################################");

        // Read city_rent.csv to get to be searched city GeoCodeId and rent
        ICsvMapReader mapReader = null;
        try {
            mapReader = new CsvMapReader(new FileReader("city_rent.csv"), CsvPreference.STANDARD_PREFERENCE);

            // the header columns are used as the keys to the Map
            final String[] header = mapReader.getHeader(true);
            final CellProcessor[] processors = getProcessors();

            Map<String, Object> customerMap;
            int counter = 0;
            while ((customerMap = mapReader.read(header, processors)) != null) {
                if (customerMap.get("GeoCodeId") != null) {
                    counter++;

                    System.out.println(customerMap.get("Stadt") + ", " + customerMap.get("Stadtteil") + ", " + customerMap.get("GeoCodeId"));

                    double rent = (double) customerMap.get("Mietpreise");

                    String urlCityString = "https://rest.immobilienscout24.de/restapi/api/search/v1.0/search/region?realestatetype=apartmentbuy&geocodes=" + customerMap.get("GeoCodeId") + "&price=0-150000&livingspace=50-1000";
                    String responseString = getResponseString(urlCityString, consumer);

                    Gson gson = new Gson();
                    JsonObject responseJson = gson.fromJson(responseString, JsonObject.class);

                    int numberOfHits = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonObject("paging").getAsJsonPrimitive("numberOfHits").getAsInt();
                    //System.out.println("number of Hits: " + numberOfHits);
                    int pageSize = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonObject("paging").getAsJsonPrimitive("pageSize").getAsInt();
                    //System.out.println("pageSize: " + pageSize);
                    int numberOfPages = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonObject("paging").getAsJsonPrimitive("numberOfPages").getAsInt();
                    //
                    // System.out.println("numberOfPages: " + numberOfPages);

                    // Search first page
                    getInterestingObjectFromOnePage(consumer, responseJson, pageSize, numberOfHits, rent);

                    // Search pages from 2. to the last
                    for (int i = 2; i <= numberOfPages; i++) {

                        //System.out.println("page number: " + i);
                        String urlString = urlCityString + "&pagenumber=" + String.valueOf(i);

                        responseJson = gson.fromJson(getResponseString(urlString, consumer), JsonObject.class);
                        pageSize = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonObject("paging").getAsJsonPrimitive("pageSize").getAsInt();

                        getInterestingObjectFromOnePage(consumer, responseJson, pageSize, numberOfHits, rent);
                    }
                }
            }
        } finally {
            if (mapReader != null) {
                mapReader.close();
            }
        }


    }

    private static void getInterestingObjectFromOnePage(OAuthConsumer consumer, JsonObject responseJson, int pageSize, int numberOfHits, double rent) throws OAuthExpectationFailedException, OAuthCommunicationException, OAuthMessageSignerException, IOException, ParseException {

        double rentalYieldThreshold = 0.06, rentalYield;
        int price, livingSpace, realEstateId;
        JsonObject realEstate;
        String creationDateString, houseNumber = "";
        Date creationDate;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date creationDateSThreshold = sdf.parse("2019-09-10");

        if (numberOfHits == 1) {
            realEstate = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonArray("resultlistEntries").get(0).getAsJsonObject().getAsJsonObject("resultlistEntry");
            price = realEstate.getAsJsonObject("resultlist.realEstate").getAsJsonObject("price").getAsJsonPrimitive("value").getAsInt();
            livingSpace = realEstate.getAsJsonObject("resultlist.realEstate").getAsJsonPrimitive("livingSpace").getAsInt();

            creationDateString = realEstate.getAsJsonPrimitive("@creation").getAsString();
            creationDate = new SimpleDateFormat("yyyy-MM-dd").parse(creationDateString);

            if (realEstate.getAsJsonObject("resultlist.realEstate").getAsJsonObject("address").has("houseNumber")){
                houseNumber = realEstate.getAsJsonObject("resultlist.realEstate").getAsJsonObject("address").getAsJsonPrimitive("houseNumber").getAsString();
            }

            rentalYield = calcRentalYield(livingSpace, price, rent);

            if (rentalYield > rentalYieldThreshold && creationDate.compareTo(creationDateSThreshold) > 0 && !houseNumber.contains("XXXX") && price > 1000) {
                realEstateId = realEstate.getAsJsonPrimitive("realEstateId").getAsInt();
                String realEstateUrl = "https://www.immobilienscout24.de/expose/" + String.valueOf(realEstateId);
                System.out.println(realEstateUrl + "    " + creationDate);
            }
        } else if (numberOfHits > 1) {
            JsonArray resultlistEntry = responseJson.getAsJsonObject("resultlist.resultlist").getAsJsonArray("resultlistEntries").get(0).getAsJsonObject().getAsJsonArray("resultlistEntry");
            for (int i = 0; i < pageSize; i++) {
                realEstate = resultlistEntry.get(i).getAsJsonObject().getAsJsonObject("resultlist.realEstate");
                price = realEstate.getAsJsonObject("price").getAsJsonPrimitive("value").getAsInt();
                livingSpace = realEstate.getAsJsonPrimitive("livingSpace").getAsInt();

                creationDateString = resultlistEntry.get(i).getAsJsonObject().getAsJsonPrimitive("@creation").getAsString();
                creationDate = new SimpleDateFormat("yyyy-MM-dd").parse(creationDateString);

                if (realEstate.getAsJsonObject("address").has("houseNumber")){
                    houseNumber = realEstate.getAsJsonObject("address").getAsJsonPrimitive("houseNumber").getAsString();
                }

                rentalYield = calcRentalYield(livingSpace, price, rent);

                if (rentalYield > rentalYieldThreshold && creationDate.compareTo(creationDateSThreshold) > 0 && !houseNumber.contains("XXXX") && price > 1000) {
                    realEstateId = resultlistEntry.get(i).getAsJsonObject().getAsJsonPrimitive("realEstateId").getAsInt();
                    String realEstateUrl = "https://www.immobilienscout24.de/expose/" + String.valueOf(realEstateId);
                    System.out.println(realEstateUrl + "    " + creationDate);

                }
            }
        }

    }

    private static double calcRentalYield(int livingSpace, int price, double rent) {
        double rentalYield = 12 * livingSpace * rent / price;
        return rentalYield;
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

        System.out.println(getResponseString("https://rest.immobilienscout24.de/restapi/api/search/v1.0/search/region?realestatetype=apartmentbuy&geocodes=1276003", consumer));
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


    private static CellProcessor[] getProcessors() {

        final CellProcessor[] processors = new CellProcessor[]{
                new NotNull(), // city
                new NotNull(), // city quarter
                new Optional(new ParseLong()), // GeoCodeId
                new NotNull(), // price
                new NotNull(new ParseDouble()),  // rent
                new NotNull(), // rent yield
                new Optional(), // > 6%
                new Optional(), // comments
        };

        return processors;

    }

}
