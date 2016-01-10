package main.java;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;


public class Sys {
	
	private static Sys impl = null;
	private static LogWriter writer = null;
	
	public static void init(Configuration configuration, Arguments arguments) {
		impl = new Sys(configuration, arguments);
		if (arguments.containsKey("--silent"))
			writer = new SilentLogWriter();
		else if (arguments.containsKey("--quiet"))
			writer = new QuietLogWriter();
		else
			writer = new StandardLogWriter();
	}
	
	public static void finish() {
		if (impl != null) impl.writeLogs();
		System.exit(0);
	}
	
	public static void halt() {
		if (impl != null) impl.writeLogs();
		System.exit(1);
	}
	
	public static String print(String template, Object... args) {
		return writer.print(template, args);
	}
	
	public static String println(String template, Object... args) {
		return writer.println(template, args);
	}
	
	public static String error(String template, Object... args) {
		return writer.error(template, args);
	}
	
	public static String log(String template, Object... args) {
		return writer.log(template, args);
	}
	
	private final int MAX_LOG_LINES;
	private final LogQueue logs;
	private final String logFileLocation;
	
	private Sys(Configuration configuration, Arguments arguments) {
		this.MAX_LOG_LINES = configuration.getEventLogSize();
		this.logs = new LogQueue(MAX_LOG_LINES);
		this.logFileLocation = arguments.value("-l");
	}
	
	private void writeLogs() {
		File logFile = new File(logFileLocation);
		
		int numLines = 0;
		if (logFile.exists()) {
			LineNumberReader lnr = null;
			try {
				lnr = new LineNumberReader(new FileReader(logFile));
				lnr.skip(Long.MAX_VALUE);
				numLines = lnr.getLineNumber() + 1;
			} catch (IOException e) {
				numLines = 0;
			} finally {
				try {
					if (lnr != null)
						lnr.close();
				} catch (IOException e) { }
			}
		}
		
		if (numLines + logs.end <= MAX_LOG_LINES) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
				for (String line : logs.getAll())
					writer.println(line);
			} catch (IOException e) {
				
			} finally {
				if (writer != null) writer.close();
			}
		} else if (logs.end >= MAX_LOG_LINES) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, false)));
				for (int i = logs.end - MAX_LOG_LINES; i < logs.end; i++) {
					writer.println(logs.get(i));
				}
			} catch (IOException e) {
				
			} finally {
				if (writer != null) writer.close();
			}
		} else {
			int numLinesToWrite = MAX_LOG_LINES - logs.end;
			int numLinesToSkip = numLines - numLinesToWrite;
			
			Scanner scanner;
			try {
				scanner = new Scanner(logFile);
			} catch (FileNotFoundException e) {
				//impossible
				throw new RuntimeException("Can't happen.");
			}
			ArrayList<String> oldInput = new ArrayList<String>();
			for (int i = 0; i < numLinesToSkip; i++)
				scanner.nextLine();
			while (scanner.hasNextLine())
			    oldInput.add(scanner.nextLine());

			scanner.close();

			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, false)));
				for (String line : oldInput)
					writer.println(line);
				for (String line : logs.getAll())
					writer.println(line);
			} catch (IOException e) {
				
			} finally {
				if (writer != null) writer.close();
			}
		}
	}
	
	static interface LogWriter {
		
		public String print(String template, Object... args);
		
		public String println(String template, Object... args);
		
		public String error(String template, Object... args);
		
		public String log(String template, Object... args);
		
	}
	
	static class StandardLogWriter implements LogWriter {
		
		public String print(String template, Object... args) {
			String message = String.format(template, args);
			System.out.print(message);
			return message;
		}
		
		public String println(String template, Object... args) {
			String message = String.format(template, args);
			System.out.println(message);
			return message;
		}
		
		public String error(String template, Object... args) {
			String message = String.format("(!) ERROR: " + template, args);
			System.out.println(message);
			if (impl != null) impl.logs.add(message);
			return message;
		}
		
		public String log(String template, Object... args) {
			String message = String.format(template, args);
			System.out.println(message);
			if (impl != null) impl.logs.add(message);
			return message;
		}
		
	}
	
	/**
	 * Print only to log, nothing to standard out.
	 */
	static class QuietLogWriter implements LogWriter {
		
		public String print(String template, Object... args) {
			return String.format(template, args);
		}
		
		public String println(String template, Object... args) {
			return String.format(template, args);
		}
		
		public String error(String template, Object... args) {
			String message = String.format("(!) ERROR: " + template, args);
			if (impl != null) impl.logs.add(message);
			return message;
		}
		
		public String log(String template, Object... args) {
			String message = String.format(template, args);
			if (impl != null) impl.logs.add(message);
			return message;
		}
		
	}
	
	/**
	 * Print nothing.
	 */
	static class SilentLogWriter implements LogWriter {
		
		public String print(String template, Object... args) {
			return String.format(template, args);
		}
		
		public String println(String template, Object... args) {
			return String.format(template, args);
		}
		
		public String error(String template, Object... args) {
			return String.format("(!) ERROR: " + template, args);
		}
		
		public String log(String template, Object... args) {
			return String.format(template, args);
		}
		
	}

}
