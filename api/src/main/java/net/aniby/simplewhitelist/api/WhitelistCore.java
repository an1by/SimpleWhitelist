package net.aniby.simplewhitelist.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

public class WhitelistCore {
    public static final String COMMAND_PERMISSION = "simplewhitelist.command";
    public static final Gson GSON = new GsonBuilder().create();

    public static void saveDefaultFile(File file, String resourcePath) throws IOException {
        saveDefaultFile(file, resourcePath, WhitelistCore.class);
    }

    public static void saveDefaultFile(File file, String resourcePath, Class<?> _class) throws IOException {
        if (!file.exists()) {
            File folder = file.toPath().toAbsolutePath().getParent().toFile();
            if (!folder.exists()) {
                folder.mkdirs();
            }
            file.createNewFile();

            InputStream initialStream = _class.getResourceAsStream(resourcePath);
            OutputStream outStream = new FileOutputStream(file);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = initialStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            initialStream.close();
            outStream.close();
        }
    }
}
