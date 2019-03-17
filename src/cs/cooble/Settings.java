package cs.cooble;

import com.sun.istack.internal.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by Matej on 4.1.2018.
 */
public class Settings {

    private File file;
    private Map<String, String> attributes;

    public Settings(File file) {
        this.file = file;
        attributes = new HashMap<>();
    }

    public void addAttribute(String name, @Nullable String defaultVal) {
        attributes.put(name, defaultVal);
    }

    @Nullable
    public String getValue(String name) {
        return attributes.get(name);
    }

    public void saveFile() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        attributes.forEach(new BiConsumer<String, String>() {
            @Override
            public void accept(String s, String s2) {
                if (s2 == null)
                    s2 = "";
                try {
                    writer.write(s + "== " + s2+"\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        writer.close();
    }

    public void loadFile() throws IOException {
        if(!file.exists()) {
            file.createNewFile();
            saveFile();
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String s = reader.readLine();
        while (s != null) {
            String[] ray = s.split("==");
            if (ray.length == 2) {
                String key = ray[0].trim();
                String val = ray[1].trim();
                attributes.put(key, val);
            }
            s = reader.readLine();
        }
        reader.close();
    }

    public void setValue(String key, String value) {
        attributes.put(key,value);
    }

    public Map<String,String> getMap() {
        return attributes;
    }
}
