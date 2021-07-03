package pacSat;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import ax25.KissFrame;
import common.Log;

public class SubmitTelem {
	
	public static final String SATNOGS_URL = "https://db.satnogs.org/api/telemetry/";
	String url;
	int noradId;
	String source; // receiving station callsign
	Date timestamp;
	String frame; // hex string of the data
	String locator = "longLat";
	double longitude;
	double latitude;
	float azimuth;
	float elevation;
	long fDown;
	
	SubmitTelem(String url, int noradId, String source, Date timestamp, double longitude, double latitude) {
		this.url = url;
		this.noradId = noradId;
		this.source = source;
		this.timestamp = timestamp;
		this.longitude = longitude;
		this.latitude = latitude;
	}
	
	public void setFrame(KissFrame kissFrame) {
		frame = kissFrame.toByteString();
	}
	
	public boolean send() throws Exception {

        HttpPost post = new HttpPost(url);

        // add request parameter, form parameters
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("noradID", ""+noradId));
        urlParameters.add(new BasicNameValuePair("source", source));
        
        TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String timestampAsISO = df.format(timestamp);
		System.err.println(timestampAsISO);
        urlParameters.add(new BasicNameValuePair("timestamp", timestampAsISO));

        urlParameters.add(new BasicNameValuePair("frame", frame));
        urlParameters.add(new BasicNameValuePair("locator", locator));
        
        DecimalFormat decimalFormat5 = new DecimalFormat();
        decimalFormat5.setMaximumFractionDigits(5);
        
        String longDir = "E";
        if (longitude < 0) {
        	longDir = "W";
        	longitude = longitude * -1;
        }
        String latDir = "N";
        if (latitude < 0) {
        	latDir = "S";
        	latitude = latitude * -1;
        }
        
        urlParameters.add(new BasicNameValuePair("longitude", decimalFormat5.format(longitude)+longDir));
        urlParameters.add(new BasicNameValuePair("latitude", decimalFormat5.format(latitude)+latDir));
        
        HttpEntity entity = new UrlEncodedFormEntity(urlParameters,"UTF-8");
               
        post.setEntity(entity);
        String postStr = EntityUtils.toString(entity);
        Log.println("SUBMITTING TELEMETRY:" + url + "?" + postStr);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {
        	// Getting the status code.
        	int statusCode = response.getStatusLine().getStatusCode();

        	// Getting the response body.
        	String responseBody = EntityUtils.toString(response.getEntity());
        	
        	Log.println("RETURN CODE:" + statusCode);
        	if (statusCode < 200 || statusCode >= 300) {
        		// Debug error
            	Log.println("ERROR :" + statusCode + "\n" + responseBody);
            	return false;
        	}
        	
        	//  Log.println(EntityUtils.toString(response.getEntity()));
        	return true;
        }
    }
	
//	public static void main(String args[]) {
//		Date timestamp = new Date();
//		TimeZone tz = TimeZone.getTimeZone("UTC");
//		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
//		df.setTimeZone(tz);
//		String timestampAsISO = df.format(timestamp);
//		System.err.println(timestampAsISO);
//	}
	
}
