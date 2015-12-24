package main.java;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import sun.misc.BASE64Encoder;


public class FreshBooksAPI {

	private final Configuration config;
	
	public FreshBooksAPI(Configuration config) {
		this.config = config;
	}
	
	public List<NameValue> listProjects() {
		StringBuilder xml = new StringBuilder();
        xml.append("<request method=\"project.list\">");
        xml.append(tag("page", Integer.toString(1)));
        xml.append(tag("per_page", "100"));
        xml.append("</request>");
        
		Response response = sendRequest(xml.toString());
		if (!response.isSuccess())
			throw new WFError("Failed to list FreshBooks projects, stopping; " + response.getEntity());
		
		Document document = createDocumentFromString(response.entity);
		NodeList nodes = document.getDocumentElement().getElementsByTagName("project");
		
		List<NameValue> projects = new ArrayList<NameValue>();
		for (int i = 0; i < nodes.getLength(); i++) {
			NodeList data = nodes.item(i).getChildNodes();
			NameValue project = new NameValue();
			for (int k = 0; k < data.getLength(); k++) {
				Node current = data.item(k);
				if ("project_id".equals(current.getNodeName())) {
					project.setId(current.getTextContent());
				} else if ("name".equals(current.getNodeName()))
					project.setName(current.getTextContent());
			}
			projects.add(project);
		}
		
		return projects;
	}
	
	public List<NameValue> listTasks(String projectId) {
		StringBuilder xml = new StringBuilder();
        xml.append("<request method=\"task.list\">");
        xml.append(tag("project_id", projectId));
        xml.append(tag("page", Integer.toString(1)));
        xml.append(tag("per_page", "100"));
        xml.append("</request>");
        
		Response response = sendRequest(xml.toString());
		if (!response.isSuccess())
			throw new WFError("Failed to list FreshBooks tasks, stopping; " + response.getEntity());
		
		Document document = createDocumentFromString(response.entity);
		NodeList nodes = document.getDocumentElement().getElementsByTagName("task");
		
		List<NameValue> tasks = new ArrayList<NameValue>();
		for (int i = 0; i < nodes.getLength(); i++) {
			NodeList data = nodes.item(i).getChildNodes();
			NameValue task = new NameValue();
			for (int k = 0; k < data.getLength(); k++) {
				Node current = data.item(k);
				if ("task_id".equals(current.getNodeName())) {
					task.setId(current.getTextContent());
				} else if ("name".equals(current.getNodeName()))
					task.setName(current.getTextContent());
			}
			tasks.add(task);
		}
		
		return tasks;
	}
	
	public boolean createEntry(String projectId, String taskId, String hours, String message, Date date) {
		final StringBuilder xml = new StringBuilder();
        xml.append("<request method=\"time_entry.create\">");
        xml.append("<time_entry>");
        xml.append(tag("project_id", projectId));
        xml.append(tag("task_id", taskId));
        xml.append(tag("hours", hours));
        if (message != null)
        	xml.append(cdata("notes", message, false));
        xml.append(tag("date", new SimpleDateFormat("yyyy-MM-dd").format(date)));
        xml.append("</time_entry>");
        xml.append("</request>");
        
        Response response = sendRequest(xml.toString());
        if (!response.isSuccess())
			throw new WFError("Failed to log FreshBooks time entry, stopping; " + response.getEntity());
        
        return response.isSuccess();
	}
	
	private Response sendRequest(String payload) {
		final HttpsURLConnection conn;
		try {
			String base = String.format("https://%s.freshbooks.com/api/2.1/xml-in", config.getFreshbooksAccount());
			URL url = new URL(base);
			conn = (HttpsURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
            String creds = String.format("%s:%s", config.getFreshbooksApiKey(), "X");
            String auth = "Basic " + new BASE64Encoder().encode(creds.getBytes());
            
            conn.addRequestProperty("Authorization", auth);
            conn.addRequestProperty("User-Agent", "WakaTimeFreshBooks");
            
		} catch (MalformedURLException e) {
			return new Response(false, 400, "Failed to build endpoint URL");
		} catch (IOException e) {
			return new Response(false, 500, "Couldn't open connection");
		}
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			writer.write(payload);
		} catch (IOException e) {
			return new Response(false, 500, "Couldn't write to connection");
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException f) { }
		}
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} catch (IOException e) {
			return new Response(false, 500, "Couldn't read connection");
		}
		String line = null;
		StringBuilder xml = new StringBuilder();
		
		try {
			while((line = reader.readLine()) != null) {
				xml.append(line);
			}
		} catch (IOException e) {
			return new Response(false, 500, "Couldn't read connection");
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException f) {}
		}
		
		return new Response(true, 200, xml.toString());
	}
	
	protected String tag(String tag, CharSequence content) {
        return tag(tag, content, true);
    }

    protected String tag(String tag, CharSequence content, boolean required) {
        if (!required && (content == null || "".equals(content)))
            return "";

        return String.format("<%s>%s</%s>", tag, content, tag);
    }

    protected String cdata(String tag, CharSequence content) {
        return cdata(tag, content, true);
    }

    protected String cdata(String tag, CharSequence content, boolean required) {
        if (!required && (content == null || "".equals(content)))
            return "";

        return String.format("<%s><![CDATA[%s]]></%s>", tag, content, tag);
    }
    
    protected Document createDocumentFromString(final String xml) {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			final DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(new InputSource(new StringReader(xml)));
		} catch (final Exception e) {
			throw new WFError(e);
		}
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
	
	
}
