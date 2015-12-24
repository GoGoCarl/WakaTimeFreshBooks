package main.java;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class Arguments {
	
	private static final String[] SUPPORTED_DATE_FORMATS = {
		"yyyy-MM-dd", "yyyy/MM/dd", "MM/dd/yyyy"
	};
	
	private final Map<String, String> arguments;
	
	public Arguments() {
		this.arguments = new LinkedHashMap<>();
		
		Map<String, String> defaultArgs = new HashMap<String, String>();
		defaultArgs.put("-d", new SimpleDateFormat(SUPPORTED_DATE_FORMATS[0]).format(Calendar.getInstance().getTime()));
		defaultArgs.put("-l", new File(System.getProperty("user.home"), ".w2f.log").getAbsolutePath());
		
		this.arguments.putAll(defaultArgs);
	}
	
	public void parse(String[] args) {
		int index;
		for (index = 0; index < args.length; index++) {
			String current = args[index];
			if (current.startsWith("--")) {
				String[] split = current.split("=");
				if (split.length == 1)
					arguments.put(split[0], arguments.get(split[0]));
				else
					arguments.put(split[0], split[1].trim());
			} else if (current.startsWith("-")) {
				try {
					String key = current;
					String value = args[++index];
					
					arguments.put(key, value.trim());
				} catch (IndexOutOfBoundsException e) {
					throw new WFError("The argument " + current + " must be accompanied by a specification. Run with --usage for more information.");
				}
			}
		}
		
		if (containsKey("--dry-run"))
			arguments.remove("--submit");
		else if (!containsKey("--submit"))
			arguments.put("--dry-run", null);
	}
	
	public boolean containsKey(String... key) {
		for (String current : key) {
			if (arguments.containsKey(current))
				return true;
		}
		return false;
	}
	
	public Date getDate() {
		return parseDate(value("-d"));
	}
	
	public String value(String key) {
		return arguments.get(key);
	}
	
	public String requiredValue(String key) {
		String value = arguments.get(key);
		if (value != null && !"".equals(value))
			return value;
		else
			throw new WFError("ERROR: The argument " + key + "required that a value be set, i.e.: " + key + "={value}");
	}
	
	private static final Date parseDate(String date) {
		for (String format : SUPPORTED_DATE_FORMATS) {
			SimpleDateFormat fmt = new SimpleDateFormat(format);
			fmt.setLenient(false);
			try {
				return fmt.parse(date);
			} catch (ParseException e) {
				continue;
			}
		}
		return Calendar.getInstance().getTime();
	}

}
