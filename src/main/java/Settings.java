import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class Settings {

    public static String hostname= "localhost";
    public static int port = 8080;
    public static String username = System.getProperty("user.name");
    public static String microphone = "";


    public static void Load(){
        try {
            System.out.println("Loading settings");
            File settingsFile = new File("settings.ini");
            Wini ini;
            if(!settingsFile.exists())
                Save();

            ini = new Wini(settingsFile);

            hostname = ini.get("Settings", "hostname");
            port = ini.get("Settings", "port", int.class);
            username = ini.get("Settings", "username");
            microphone = ini.get("Settings", "microphone");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void Save(){
        try {
            System.out.println("Saving settings");
            File settingsFile = new File("settings.ini");
            Wini ini;
            if(!settingsFile.exists())
                settingsFile.createNewFile();

            ini = new Wini(settingsFile);

            ini.put("Settings", "hostname", hostname);
            ini.put("Settings", "port", port);
            ini.put("Settings", "username", username);
            ini.put("Settings", "microphone", microphone);

            ini.store();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
