package test;

import main.java.Main;


public class Runner {
	
	public static void main(String[] args) {
		testDefault();
	}
	
	public static void testDefault() {
		Main.main(new String[] { });
	}
	
	public static void testDryRun() {
		Main.main(new String[] { "--dry-run" });
	}
	
	public static void testMerge() {
		Main.main(new String[] { "--merge" });
	}
	
	public static void testUsage() {
		Main.main(new String[] { "--usage" });
	}
	
	public static void testSilent() {
		Main.main(new String[] { "--silent" });
	}
	
	public static void testQuiet() {
		Main.main(new String[] { "--quiet" });
	}
	
	public static void testConfig() {
		Main.main(new String[] { "--add-project" });
	}
	
	public static void setApiKeyFB() {
		Main.main(new String[] { "--set-freshbooks-api-key" });
	}
	
	public static void setApiKeyWaka() {
		Main.main(new String[] { "--set-wakatime-api-key" });
	}

}
