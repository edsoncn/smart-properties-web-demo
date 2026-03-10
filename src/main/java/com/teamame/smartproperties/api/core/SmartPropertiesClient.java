package com.teamame.smartproperties.api.core;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SmartPropertiesClient {

	public static Response callSmartPropertiesCodesService(String urlString, String apiToken, Map<String, String> urlParams) {
        HttpURLConnection connection = null;

        try {
            System.out.println("Calling Smart Properties Codes Service with URL: " + urlString);

        	if(urlParams != null && urlParams.size() > 0) {
                StringBuilder urlStringBuilder = new StringBuilder(urlString);
        		if(!urlString.contains("?")) urlStringBuilder.append("?");
                urlStringBuilder.append(urlParams.entrySet().stream().map(entry -> {
                    try {
                        return URLEncoder.encode(entry.getKey(), "UTF-8").concat("=").concat(URLEncoder.encode(entry.getValue(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.joining("&")));
                urlString = urlStringBuilder.toString();
        		System.out.println(urlString);
        	}
        	
            final URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", apiToken);
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // response code
            System.out.println(connection.getResponseCode());
            Response response = new Response();
            response.setResponseCode(connection.getResponseCode());

            // response headers
            final Map<String, List<String>> headerFields = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                System.out.println("Header [Key: " + entry.getKey() + " - Value: " + entry.getValue() + "]");
            }

            // response body
            if (connection.getInputStream() != null) {
                final InputStream inputStream = connection.getInputStream();
                
                try(final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                    final StringBuilder responseSb = new StringBuilder();

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                    	responseSb.append(line);
                    }

                    response.setResponseString(responseSb.toString());
                    System.out.println(response.getResponseString());
                    return response;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
	}
	
	public static class Response {
		
		private int responseCode;
		private String responseString;
		
		public int getResponseCode() {
			return responseCode;
		}
		
		public void setResponseCode(int responseCode) {
			this.responseCode = responseCode;
		}
		
		public String getResponseString() {
			return responseString;
		}
		
		public void setResponseString(String responseString) {
			this.responseString = responseString;
		}
		
	}
	
}
