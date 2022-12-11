package org.ds2os.vsl.service;

import org.ds2os.vsl.service.db.JsonServiceManifest;

import java.io.*;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarFileReader {
    public static File extractJSONManifest(File zip) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zip);
            String path = zip.getAbsolutePath();
            String fileName = zip.getName();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".json")) {
                    InputStream input = zipFile.getInputStream(entry);
                    File out = new File(entry.getName());
                    OutputStream outputStream = new FileOutputStream(out);
                    int read = 0;
                    byte[] bytes = new byte[1024];
                    while ((read = input.read(bytes)) != -1)
                        outputStream.write(bytes, 0, read);
                    return out;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        File f = new File("/home/ds2os/Desktop/slsm-project/s2store/src/main/java/org/ds2os/vsl/service/store/services/serviceFile.zip");
        File json = extractJSONManifest(f);
        JsonServiceManifest manifest = readJSONManifest(json);
        System.out.println(manifest.toString());
    }

    public static JsonServiceManifest readJSONManifest(File json) {
        try {
            return Utils.loadJsonFile(json.getName(), JsonServiceManifest.class);
        } catch (Exception e) {
            System.err.println("Couldn't parse manifest file -> " + e);
            return null;
        }
    }

    public static List<String> readManifest(File jarFile) {
        return readTextFile(jarFile, "MANIFEST.MF");
    }

    public static List<String> readTextFile(File jarFile, String textFileName) {
        JarInputStream jarIS;
        List<String> res = null;
        try {
            jarIS = new JarInputStream(new FileInputStream(jarFile));
            JarEntry entry = null;
            while ((entry = jarIS.getNextJarEntry()) != null) {
                String e = entry.getName();
                if (!entry.isDirectory() && e.endsWith(textFileName)) {
                    Scanner sc = new Scanner(jarIS);
                    res = new LinkedList<>();
                    while (sc.hasNextLine())
                        res.add(sc.nextLine());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
}
