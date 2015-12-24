package main.java;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class Configuration {
	
	public static final String DEFAULT_LOCATION = 
			new File(System.getProperty("user.home"), ".w2f.properties").getAbsolutePath();
	
	private static Configuration impl;
	
	public static Configuration getInstance(String configFileLocation) {
		if (impl == null)
			impl = new Configuration(configFileLocation);
		return impl;
	}
	
	private final Properties properties;
	private final String configFileLocation;
	
	private Configuration(String configFileLocation) {
		this.properties = new Properties();
		this.configFileLocation = configFileLocation;
		
		File file = getConfigurationFile();
		if (file.exists()) {
			try {
				properties.load(new FileReader(file));
			} catch (IOException e) {
				Sys.error("File " + file.getName() + " could not be loaded, using defaults.");
			}
		} else
			Sys.log("Warning: No file exists at " + file.getAbsolutePath());
		
		try {
			TrustManager[] trustAllCerts = new TrustManager[] {
			   new X509TrustManager() {
			      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			        return null;
			      }

			      public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

			      public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

			   }
			};

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
			    public boolean verify(String hostname, SSLSession session) {
			      return true;
			    }
			};
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (KeyManagementException e) {
			throw new RuntimeException("Init failure: " + e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Init failure: " + e.getMessage(), e);
		}
	}
	
	public String getAbsoluteLocation() {
		return getConfigurationFile().getAbsolutePath();
	}
	
	private File getConfigurationFile() {
		return new File(configFileLocation);
	}
	
	public boolean isSaved() {
		return getConfigurationFile().exists();
	}
	
	public boolean isProjectConfigured(String project) {
		return (properties.containsKey("projects." + cleanProjectName(project) + ".freshbooks.project.id"));
	}
	
	public String getFreshbooksApiKey() {
		return getOrDie("global.freshbooks.api_key");
	}
	
	public void setFreshbooksApiKey(String key) {
		properties.setProperty("global.freshbooks.api_key", key);
		save();
	}
	
	public int getEventLogSize() {
		String value = properties.getProperty("global.logging.size", "1000");
		try {
			return Integer.valueOf(value);
		} catch (Exception e) {
			return 1000;
		}
	}
	
	public void setEventLogSize(String size) {
		Integer length;
		try { 
			length = Integer.valueOf(size);
		} catch (Exception e) {
			throw new WFError("Invalid size given: " + size);
		}
		
		if (length > Integer.MAX_VALUE)
			throw new WFError("Can't store that many lines, choose a smaller amount: " + size);
		
		properties.setProperty("global.logging.size", length.toString());
		save();
	}
	
	public String getFreshbooksAccount() {
		return getOrDie("global.freshbooks.account");
	}
	
	public void setFreshbooksAccount(String account) {
		properties.setProperty("global.freshbooks.account", account);
		save();
	}
	
	public NameValue getFreshbooksProject(String project) {
		NameValue nv = new NameValue();
		nv.id = getOrDie("projects." + cleanProjectName(project) + ".freshbooks.project.id");
		nv.name = getOrDie("projects." + cleanProjectName(project) + ".freshbooks.project.name");
		return nv;
	}
	
	public NameValue getFreshbooksTask(String project) {
		NameValue nv = new NameValue();
		nv.id = getOrDie("projects." + cleanProjectName(project) + ".freshbooks.task.id");
		nv.name = getOrDie("projects." + cleanProjectName(project) + ".freshbooks.task.name");
		return nv;
	}
	
	public void associateProject(String project, NameValue fbProject, NameValue fbTask) {
		String projectPropName = cleanProjectName(project);
		properties.setProperty("projects." + projectPropName + ".freshbooks.project.id", fbProject.id);
		properties.setProperty("projects." + projectPropName + ".freshbooks.task.id", fbTask.id);
		properties.setProperty("projects." + projectPropName + ".freshbooks.project.name", fbProject.name);
		properties.setProperty("projects." + projectPropName + ".freshbooks.task.name", fbTask.name);
		save();
	}
	
	public void dissociateProject(String project) {
		String projectPropName = cleanProjectName(project);
		properties.remove("projects." + projectPropName + ".freshbooks.project.id");
		properties.remove("projects." + projectPropName + ".freshbooks.task.id");
		properties.remove("projects." + projectPropName + ".freshbooks.project.name");
		properties.remove("projects." + projectPropName + ".freshbooks.task.name");
		save();
	}
	
	private void save() {
		try {
			FileWriter writer = new FileWriter(getConfigurationFile());
			properties.store(writer, null);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String cleanProjectName(String project) {
		return project.toLowerCase().replace(" ", "_").trim();
	}
	
	private String getOrDie(String property) {
		return getOrDie(property, "Required properties not configured, check ~/.w2f.properties");
	}
	
	private String getOrDie(String property, String message) {
		if (properties.containsKey(property))
			return properties.getProperty(property);
		else
			throw new RuntimeException(message);
	}
	
	public String getWakatimeApiKey() {
		if (properties.containsKey("global.wakatime.api_key"))
			return properties.getProperty("global.wakatime.api_key");
		else
			throw new RuntimeException("WakaTime API Key not set, run with --set-wakatime-api-key {key}.");
	}
	
	public void setWakatimeApiKey(String key) {
		properties.setProperty("global.wakatime.api_key", key);
		save();
	}

}
