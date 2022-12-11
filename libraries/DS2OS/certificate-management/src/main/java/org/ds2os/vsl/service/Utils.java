package org.ds2os.vsl.service;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.ds2os.vsl.service.db.JsonServiceManifest;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {
    public static void addToZipFile(String fileName, ZipOutputStream zos) throws Exception {
        Path file = Paths.get(fileName);
        FileInputStream fis = new FileInputStream(file.toFile());
        ZipEntry zipEntry = new ZipEntry(file.getFileName().toString());
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[4096];
        int length;
        while ((length = fis.read(bytes)) >= 0)
            zos.write(bytes, 0, length);
        zos.closeEntry();
        fis.close();
    }

    public static void addToZipFile(String fileName, InputStream is, ZipOutputStream zos) throws Exception {
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[4096];
        int length;
        while ((length = is.read(bytes)) >= 0)
            zos.write(bytes, 0, length);
        zos.closeEntry();
        is.close();
    }

    public static String buildVslGetParameters(Map<String, String> params) throws Exception {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (result.length() > 0)
                result.append('&');
            result.append(URI.create(entry.getKey()).toString());
            result.append('=');
            result.append(URI.create(entry.getValue()).toString());
        }
        return result.toString();
    }

    public static byte[] fromBase64String(String str) {
        return Base64.getDecoder().decode(str);
    }

    public static String getJSONFromManifest(JsonServiceManifest manifest) {
        Gson gson = new Gson();
        return gson.toJson(manifest);
    }

    public static <T> T loadJsonFile(String filename, Class<T> type) throws Exception {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(filename));
        return (T) gson.fromJson(reader, type);
    }

    public static Config parseConfig(String fileName) throws Exception {
        return Config.parseConfig(fileName);
    }

    public static Parameters parseInputParameters(String[] args) {
        Parameters result = new Parameters();
        if (args.length >= 2) {
            result.agentUrl = args[0];
            result.keystore = args[1];
            if (args.length > 2) {
                result.additionalParams = new String[args.length - 2];
                int i = 2, j = 0;
                while (i < args.length)
                    result.additionalParams[j++] = args[i++];
            }
        } else {
            System.err.println("Usage: java -jar <jarfile> <agent URL> <keystore file>");
            System.exit(-1);
        }
        return result;
    }

    public static Map<String, String> parseVslGetParameters(String address, String prefix) throws Exception {
        Map<String, String> parameters = new HashMap<>();
        if (address.equals(prefix))
            return parameters;
        String suffix = address.substring((prefix + "/").length());
        suffix = URI.create(suffix).toString();
        String[] rawParams = suffix.split("&");
        for (String p : rawParams) {
            int valIndex = p.indexOf('=');
            if (valIndex > 0) {
                String key = p.substring(0, valIndex);
                String value = p.substring(valIndex + 1);
                parameters.put(key, value);
            }
        }
        return parameters;
    }

    public static Map<String, String> parseVslResultString(String result) {
        String[] params = result.split("&");
        Map<String, String> paramMap = new HashMap<>();
        for (String p : params) {
            String k = p.split("=")[0];
            String v = p.split("=")[1];
            paramMap.put(k, v);
        }
        return paramMap;
    }

    public static void readFromConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            String quitString;
            do {
                System.out.println("Type q to end this service.");
            } while ((quitString = in.readLine()) != null && !quitString.equals("q"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> void saveToJsonFile(String filename, T object) throws Exception {
        Gson gson = new Gson();
        String json = gson.toJson(object);
        FileOutputStream outputStream = new FileOutputStream(filename);
        outputStream.write(json.getBytes());
        outputStream.close();
    }

    public static String toBase64String(byte[] bytes) throws Exception {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static void unzipFile(String filePath, String outputFolder) throws Exception {
        byte[] buffer = new byte[2048];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(filePath));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            String fileName = zipEntry.getName();
            File newFile = new File(outputFolder + File.separator + fileName);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0)
                fos.write(buffer, 0, len);
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static class Parameters {
        public String[] additionalParams = null;
        public String agentUrl = null;
        public String keystore = null;
    }
}
