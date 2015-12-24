package main.java;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.Gson;


public class WakaTimeAPI {
	
	private static final String BASE_URL = "https://wakatime.com/api/v1";
	
	private final Configuration config;
	
	public WakaTimeAPI(Configuration config) {
		this.config = config;
	}
	
	public Duration[] getDurations(Date date, String project) {
		SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("date", fmt.format(date));
		if (project != null)
			params.put("project", project);
		
		Response response = sendRequest("/users/current/durations", params);
		
		if (!response.isSuccess())
			throw new RuntimeException("Failed to retrieve duration list, stopping; " + response.getEntity());
		
		Gson gson = new Gson();
		DurationResponse dr = gson.fromJson(response.getEntity(), DurationResponse.class);
		
		return dr.data;
	}
	
	public NameValue[] listProjects() {
		SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
		
		Date today = Calendar.getInstance().getTime();
		Date lastYr = today;
		
		Calendar lastYrCal = Calendar.getInstance();
		lastYrCal.add(Calendar.DATE, -7);
		lastYr = lastYrCal.getTime();
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("start", fmt.format(lastYr));
		params.put("end", fmt.format(today));
		
		Response response = sendRequest("/users/current/summaries", params);
		
		if (!response.isSuccess())
			throw new WFError("Failed to retrieve duration list, stopping; " + response.getEntity());
		
		Gson gson = new Gson();
		StatsResponse sr = gson.fromJson(response.getEntity(), StatsResponse.class);
		
		HashMap<String, NameValue> set = new HashMap<String, NameValue>();
		for (StatsInner i : sr.data)
			for (NameValue p : i.projects)
				set.put(p.name, p);
		
		return set.values().toArray(new NameValue[set.size()]);
	}
	
	private Response sendRequest(String endpoint, Map<String, String> params) {
		if (params == null)
			params = new HashMap<String, String>();
		params.put("api_key", config.getWakatimeApiKey());
		
		final HttpsURLConnection conn;
		try {
			URL url = new URL(BASE_URL + endpoint + mapParams(params));
			conn = (HttpsURLConnection) url.openConnection();
			conn.setDoInput(true);
		} catch (MalformedURLException e) {
			return new Response(false, 400, "Failed to build endpoint URL");
		} catch (IOException e) {
			return new Response(false, 500, "Couldn't open connection");
		}
		
		final BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
			return new Response(false, 500, "Couldn't read connection");
		}
		String line = null;
		StringBuilder json = new StringBuilder();
		
		try {
			while((line = reader.readLine()) != null) {
				json.append(line);
			}
		} catch (IOException e) {
			return new Response(false, 500, "Couldn't read connection");
		}
		
		return new Response(true, 200, json.toString());
	}
	
	private String mapParams(Map<String, String> params) throws UnsupportedEncodingException {
		if (params == null || params.isEmpty())
			return "";
		
		StringBuilder out = new StringBuilder();
		out.append('?');
		
		for (Iterator<Map.Entry<String, String>> iter = params.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry<String, String> entry = iter.next();
			out.append(entry.getKey());
			out.append('=');
			out.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			if (iter.hasNext())
				out.append('&');
		}
		
		return out.toString();
	}
	
	public static class Response {
		
		boolean success;
		int status;
		String entity;
		
		public Response(boolean success, int status, String entity) {
			this.success = success;
			this.status = status;
			this.entity = entity;
		}
		
		public boolean isSuccess() {
			return success;
		}
		
		public int getStatus() {
			return status;
		}
		
		public String getEntity() {
			return entity;
		}
		
	}
	
	public class DurationResponse {
		
		private Duration[] data;
		
	}
	
	public class StatsResponse {
		
		private StatsInner[] data;
		
	}
	
	public class StatsInner {
		
		private NameValue[] projects;
		
	}

}
