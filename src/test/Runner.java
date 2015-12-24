package test;

import main.java.Main;


public class Runner {
	
	public static void main(String[] args) {
		Main.main(new String[] { });
	}
	
	public static void testDefault() {
		Main.main(new String[] { });
	}
	
	public static void testDryRun() {
		Main.main(new String[] { "--dry-run" });
	}
	
	public static void testUsage() {
		Main.main(new String[] { "--usage" });
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
