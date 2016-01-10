package main.java;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;


public class Main {
	
	public static void main(String[] args) {
		try {
			run(args);
		} catch (WFError e) {
			Sys.log(e.getMessage());
			Sys.halt();
		}
	}
	
	public static void run(String[] args) throws WFError {
		Arguments arguments = new Arguments();
		arguments.parse(args);
		
		String configFileLocation;
		if (arguments.containsKey("-c")) {
			configFileLocation = arguments.value("-c");
		} else
			configFileLocation = Configuration.DEFAULT_LOCATION;
			
		Configuration configuration = Configuration.getInstance(configFileLocation);
		if (!configuration.isSaved())
			ConfigurationBuilder.initialize(configFileLocation);
		
		Sys.init(configuration, arguments);
		
		if (arguments.containsKey("--usage", "--help")) {
			Sys.println("WakaTimeFreshBooks grabs time logs for a given date from WakaTime and logs the entries to FreshBooks.");
			Sys.println("A running event log can be found by default at ~/.w2f.log");
			Sys.println("Configuration properties can be found by default at ~/.w2f.properties");
			Sys.println("");
			Sys.println("Single-Run Arguments:");
			Sys.println("-------------------");
			Sys.println("-c {config-file-path}  -- specify location of config file (default ~/.w2f.properties)");
			Sys.println("-l {log-file-path}     -- specify location of log file (default ~/.w2f.log)");
			Sys.println("");
			Sys.println("-d {date}              -- date to log (default today). Use yyyy-MM-dd format");
			Sys.println("-p {project}           -- only log time for the named project");
			Sys.println("");
			Sys.println("--dry-run              -- see what will be logged (true by default, unless --submit is used)");
			Sys.println("--interactive          -- with --submit, run in interactive mode, enter messages as entry is logged, choose what to log");
			Sys.println("--merge                -- merge time logs into a single entry by project");
			Sys.println("--submit               -- submit timelogs (nothing ever gets submitted unless this flag is present)");
			Sys.println("--timestamps           -- print start and end timestamps to log");
			Sys.println("--verbose              -- verbose output and logging");
			Sys.println("--quiet                -- print only to log, nothing to standard out");
			Sys.println("--silent               -- no output, not even to logs");
			Sys.println("");
			Sys.println("");
			Sys.println("Configure Every-Run Settings (with respect to config file in -c):");
			Sys.println("-------------------");
			Sys.println("--add-project                      -- associate a WakaTime project with FreshBooks");
			Sys.println("--remove-project                   -- dissociate a WakaTime project with FreshBooks");
			Sys.println("--set-event-log-size={length}      -- set the max number of events to log");
			Sys.println("--set-freshbooks-api-key={api-key} -- set FreshBooks API Key");
			Sys.println("--set-freshbooks-account={account} -- set FreshBooks account");
			Sys.println("--set-wakatime-api-key={api-key}   -- set WakaTime API Key");
			Sys.finish();
		} else if (arguments.containsKey("--add-project")) {
			ConfigurationBuilder.addFreshbooksProject(configuration);
			Sys.finish();
		} else if (arguments.containsKey("--remove-project")) {
			ConfigurationBuilder.removeFreshbooksProject(configuration);
			Sys.finish();
		} else if (arguments.containsKey("--set-wakatime-api-key")) {
			configuration.setWakatimeApiKey(arguments.value("--set-wakatime-api-key"));
			Sys.log("Set WakaTime API Key to %s", arguments.requiredValue("--set-wakatime-api-key"));
			Sys.finish();
		} else if (arguments.containsKey("--set-freshbooks-api-key")) {
			configuration.setFreshbooksApiKey(arguments.requiredValue("--set-freshbooks-api-key"));
			Sys.log("Set WakaTime API Key to %s", arguments.value("--set-freshbooks-api-key"));
			Sys.finish();
		} else if (arguments.containsKey("--set-freshbooks-account")) {
			configuration.setFreshbooksAccount(arguments.requiredValue("--set-freshbooks-account"));
			Sys.log("Set WakaTime API Key to %s", arguments.value("--set-freshbooks-account"));
			Sys.finish();
		} else if (arguments.containsKey("--set-event-log-size")) {
			configuration.setEventLogSize(arguments.requiredValue("--set-event-log-size"));
			Sys.log("Set Event Log Size to %s", arguments.value("--set-event-log-size"));
			Sys.finish();
		}
		
		Date date = arguments.getDate();
		boolean submit = arguments.containsKey("--submit");
		boolean interactive = arguments.containsKey("--interactive");
		boolean verbose = arguments.containsKey("--verbose");
		boolean merge = arguments.containsKey("--merge");
		String project = arguments.value("-p");
		
		WakaTimeAPI waka = new WakaTimeAPI(configuration);
		
		if (arguments.containsKey("--timestamps"))
			Sys.log("=========== Started at %s ===========", Calendar.getInstance().getTime());
		
		Sys.log("Fetching logs for %s...", date);
		
		Duration[] durations = waka.getDurations(date, project);
		if (merge) {
			Map<String, Double> map = new LinkedHashMap<>();
			for (Duration duration : durations) {
				double hours = 0;
				if (map.containsKey(duration.getProject()))
					hours = map.get(duration.getProject()).doubleValue();
				hours += duration.getDuration();
				map.put(duration.getProject(), Double.valueOf(hours));
			}
			durations = new Duration[map.size()];
			int index = 0;
			for (Map.Entry<String, Double> entry : map.entrySet()) {
				Duration duration = new Duration();
				duration.setProject(entry.getKey());
				duration.setDuration(entry.getValue());
				durations[index++] = duration;
			}
		}
		
		Sys.log("  * Found %s entries from WakaTime...", durations.length);
		
		if (!submit) {
			Sys.log("");
			Sys.log("Informative View (nothing will be logged):");
			boolean hasUnassociatedProjects = false;
			for (Duration duration : durations) {
				if (configuration.isProjectConfigured(duration.getProject())) {
					double minutes = duration.getDuration() / 60d;
					double hours = minutes / 60d;
					
					String hoursStr = new DecimalFormat("0.##").format(hours);
					
					Sys.log("  + Will log %s hours to FreshBooks project %s, task %s",
							hoursStr,
							configuration.getFreshbooksProject(duration.getProject()).getName(),
							configuration.getFreshbooksTask(duration.getProject()).getName());
				} else {
					hasUnassociatedProjects = true;
					Sys.log("  x FreshBooks not configured for %s, will not log", duration.getProject());
				}
			}
			if (hasUnassociatedProjects) {
				Sys.println("");
				Sys.println("Configure missing projects by running with --add-project");
			}
		} else {
			FreshBooksAPI fbapi = new FreshBooksAPI(configuration);
			for (Duration duration : durations) {
				if (configuration.isProjectConfigured(duration.getProject())) {
					double minutes = duration.getDuration() / 60d;
					double hours = minutes / 60d;
					
					String hoursStr = new DecimalFormat("0.##").format(hours);
					String message = null;
					
					if (interactive) {
						Sys.println("  + Will log %s hours to FreshBooks project %s, task %s",
								hoursStr,
								configuration.getFreshbooksProject(duration.getProject()).getName(),
								configuration.getFreshbooksTask(duration.getProject()).getName());
						Sys.println("Enter one of the following numeric options to continue:");
						Sys.println("  1) Log time entry without message");
						Sys.println("  2) Enter message, then log time entry");
						Sys.println("  3) Skip this time entry");
						
						Scanner scanner = new Scanner(System.in);
						
						int selection;
						{
							selection = 1 + ConfigurationBuilder.getIndexSelection(scanner, 3);
							//scanner.close();
						}
						
						if (selection == 1) {
							//Nothing to do, keep going to log entry
							scanner.close();
						} else if (selection == 2) {
							message = getMessage(scanner);
							scanner.close();
						} else if (selection == 3) {
							//Skip entry
							Sys.log("  - Entry skipped.");
							scanner.close();
							continue;
						}
					}
					
					fbapi.createEntry(
							configuration.getFreshbooksProject(duration.getProject()).getId(), 
							configuration.getFreshbooksTask(duration.getProject()).getId(), 
							hoursStr, message, date);
					Sys.log("  + Logged %s hours to FreshBooks project %s, task %s.",
							hoursStr,
							configuration.getFreshbooksProject(duration.getProject()).getName(),
							configuration.getFreshbooksTask(duration.getProject()).getName());
				} else {
					if (verbose)
						Sys.log("  - Skipping time entry for %s,  no FreshBooks project associated.", duration.getProject());
				}
			}
		}
		
		if (arguments.containsKey("--timestamps"))
			Sys.log("=========== Finished at %s ===========", Calendar.getInstance().getTime());
		
		Sys.finish();
	}
	
	private static String getMessage(Scanner scanner) {
		Sys.println("Enter time entry message:");
		Sys.print(" -> ");
		
		scanner.nextLine();
		String value = scanner.nextLine();
		
		return value;
	}
	

}
