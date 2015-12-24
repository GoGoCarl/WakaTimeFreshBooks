package main.java;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;


public class ConfigurationBuilder {
	
	public static void initialize(String configFileLocation) {
		Configuration configuration = Configuration.getInstance(configFileLocation);
		
		Scanner scanner = new Scanner(System.in);
		
		Sys.println("##########");
		Sys.println("Welcome to WakaTimeFreshBooks. Connect your WakaTime and FreshBooks accounts to get started.");
		Sys.println("You can view/edit your configuration at any time at: %s", configuration.getAbsoluteLocation());
		Sys.println("##########");
		
		String wakaApiKey = null, discoveredWakaApiKey = findWakatimeApiKey();
		if (discoveredWakaApiKey != null && !"".equals(discoveredWakaApiKey)) {
			Sys.println("");
			if (ask(scanner, "Is this the WakaTime API Key you would like to use? " + discoveredWakaApiKey))
				wakaApiKey = discoveredWakaApiKey;
		}
		
		if (wakaApiKey == null) {
			Sys.println("");
			Sys.println("Enter WakaTime API Key:");
			Sys.print(" -> ");
			
			wakaApiKey = scanner.next();
		}
		
		Sys.println("Enter FreshBooks Account Name (from {account}.freshbooks.com):");
		Sys.print(" -> ");
		
		String fbAccount = scanner.next();
		
		Sys.println("Enter FreshBooks API Key:");
		Sys.print(" -> ");
		
		String fbApiKey = scanner.next();
		
		configuration.setFreshbooksAccount(fbAccount);
		configuration.setFreshbooksApiKey(fbApiKey);
		configuration.setWakatimeApiKey(wakaApiKey);
		
		Sys.println("");
		Sys.println("Run with --usage flag for running options. Run with --submit flag to log time.");
		
		Sys.println("");
		if (ask(scanner, "Would you like to associate any " +
				"WakaTime projects with FreshBooks projects now?")) {
			Sys.println("");
			addFreshbooksProject(configuration);
		} else
			Sys.println("You can associate projects at any time by running with --add-project");
		
		scanner.close();
	}
	
	public static void removeFreshbooksProject(Configuration configuration) {
		WakaTimeAPI waka = new WakaTimeAPI(configuration);
		
		Scanner scanner = new Scanner(System.in);
		
		boolean again = false;
		
		do {
			Sys.println("### Select WakaTime project by number:");
			List<NameValue> wp = new ArrayList<NameValue>();
			for (NameValue nv : waka.listProjects()) {
				if (configuration.isProjectConfigured(nv.getName()))
					wp.add(nv);
			}
			Collections.sort(wp);
			
			if (wp.isEmpty()) {
				Sys.println("You do not have any WakaTime project associated with FreshBooks yet.");
				break;
			} else {
				for (int i = 0; i < wp.size(); i++) {
					NameValue c = wp.get(i);
					Sys.println("    %s) %s (%s; %s)", i+1, c.getName(),
							configuration.getFreshbooksProject(c.getName()).getName(),
							configuration.getFreshbooksTask(c.getName()).getName());
				}
				
				int wakaProjectIndex = getIndexSelection(scanner, wp.size());
				
				NameValue wakaProject = wp.get(wakaProjectIndex);
				
				if (ask(scanner, "Are you sure you want to dissociate this " +
						"project with FreshBooks? Time will not be logged.")) {
					configuration.dissociateProject(wakaProject.getName());
				
					Sys.println("Removed associated of WakaTime project %s from FreshBooks", wakaProject.getName());
				}
				
				Sys.println("");
				again = ask(scanner, "Would you like to remove another project?");
			}
		} while (again);
		
		scanner.close();
	}
	
	public static void addFreshbooksProject(Configuration configuration) {
		WakaTimeAPI waka = new WakaTimeAPI(configuration);
		FreshBooksAPI fb = new FreshBooksAPI(configuration);
		
		Scanner scanner = new Scanner(System.in);
		
		boolean again = false;
		
		do {
			Sys.println("### Select WakaTime project by number:");
			List<NameValue> wp = new ArrayList<NameValue>();
			for (NameValue nv : waka.listProjects())
				wp.add(nv);
			Collections.sort(wp);
			
			if (wp.isEmpty()) {
				Sys.println("You do not have any time logged in WakaTime yet.");
				break;
			} else {
				for (int i = 0; i < wp.size(); i++) {
					NameValue c = wp.get(i);
					if (configuration.isProjectConfigured(c.getName())) {
						Sys.println("    %s) %s (%s; %s)", i+1, c.getName(),
								configuration.getFreshbooksProject(c.getName()).getName(),
								configuration.getFreshbooksTask(c.getName()).getName());
					}
					else
						Sys.println("    %s) %s", i+1, c.getName());
				}
				
				int wakaProjectIndex = getIndexSelection(scanner, wp.size());
				int freshbooksProjectIndex = -1, freshbooksTaskIndex = -1;
				
				List<NameValue> fp = fb.listProjects(), ft;
				
				if (fp.isEmpty()) {
					scanner.close();
					throw new WFError("Error: Could not find any FreshBooks projects.");
				}
				
				do {
					Sys.println("### Select FreshBooks project to associate with this project by number:");
					printCollection(fp, true);
					
					freshbooksProjectIndex = getIndexSelection(scanner, fp.size());
					
					ft = fb.listTasks(fp.get(freshbooksProjectIndex).getId());
					if (ft.isEmpty()) {
						Sys.println(" x No tasks available for this project, please choose a different project.");
						Sys.println("");
						
						fp = fb.listProjects();
					} else {
						Sys.println("### Select FreshBooks task to associate with this project by number:");
						printCollection(ft, true);
						
						freshbooksTaskIndex = getIndexSelection(scanner, ft.size());
					}
				} while (freshbooksProjectIndex < 0 || freshbooksTaskIndex < 0);
				
				NameValue wakaProject = wp.get(wakaProjectIndex);
				NameValue fbProject = fp.get(freshbooksProjectIndex);
				NameValue fbTask = ft.get(freshbooksTaskIndex);
				
				Sys.println("Associated WakaTime project %s with FreshBooks project %s, task %s",
						wakaProject.getName(), fbProject.getName(), fbTask.getName());
				
				configuration.associateProject(wakaProject.getName(), fbProject, fbTask);
				
				Sys.println("");
				again = ask(scanner, "Would you like to add another project?");
			}
		} while (again);
		
		scanner.close();
	}
	
	public static boolean ask(Scanner scanner, String question) {
		Sys.println(question);
		Boolean yesOrNo = null;
		do {
			Sys.print(" Y/N -> ");
			String input = scanner.next().toLowerCase();
			if (input.startsWith("y"))
				yesOrNo = true;
			else if (input.startsWith("n"))
				yesOrNo = false;
			else
				Sys.println("Please enter Y (yes) or N (no)"); 
		} while (yesOrNo == null);
		
		return yesOrNo.booleanValue();
	}
	
	public static int getIndexSelection(Scanner scanner, int size) {
		Integer validSelection = null;
		do {
			Sys.print(" -> ");
			int selection = scanner.nextInt();
			if (selection >= 1 && selection <= size)
				validSelection = selection;
			else
				Sys.println("Please enter a number between 1 and " + (size) + ":");
		} while (validSelection == null);
		
		return validSelection - 1;
	}
	
	public static void printCollection(List<NameValue> list, boolean sort) {
		if (sort)
			Collections.sort(list);
		
		for (int i = 0; i < list.size(); i++) {
			NameValue c = list.get(i);
			Sys.println("    %s) %s", i+1, c.getName());
		}
	}
	
	private static String findWakatimeApiKey() {
		String api_key = null;
		
		File file = new File(System.getProperty("user.home"), ".wakatime.cfg");
		if (file.exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("api_key")) {
						String[] split = line.split("=");
						if (split.length == 2) {
							api_key = split[1].trim();
						}
					}
				}
			} catch (IOException ignored) {
			} finally {
				try { if (reader != null) reader.close(); } catch (IOException ig) { }
			}
		}
		
		return api_key;
	}

}
