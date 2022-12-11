package org.ds2os.vsl.service.tools;

import com.google.gson.Gson;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ds2os.vsl.service.Utils;
import org.ds2os.vsl.service.db.JsonServiceManifest;
import org.ds2os.vsl.service.security.SecurityUtils;
import org.ds2os.vsl.service.security.SelfSignedCertificateBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.zip.ZipOutputStream;

public class ServicePackager {
    public static JsonServiceManifest createServiceManifest(String serviceId, String developerId, String versionNumber, String executablePath, String contextModelPath) throws Exception {
        JsonServiceManifest manifest = new JsonServiceManifest(serviceId, developerId, versionNumber, null, null, null, null, null, null, null);
        String executableHash = SecurityUtils.hashFile(executablePath);
        manifest.setExecutableHash(executableHash);
        String contextHash = SecurityUtils.hashFile(contextModelPath);
        manifest.setContextModelHash(contextHash);
        return manifest;
    }

    public static void createServicePackage(String serviceId, String serviceJarPath, String outputFile, JsonServiceManifest manifest, String contextModelPath, String certificatePath) throws Exception {
        FileOutputStream fos = new FileOutputStream(outputFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        Utils.addToZipFile(serviceJarPath, zos);
        Utils.addToZipFile(serviceId + ".json", new ByteArrayInputStream((new Gson()).toJson(manifest).getBytes(StandardCharsets.UTF_8)), zos);
        Utils.addToZipFile(contextModelPath, zos);
        Utils.addToZipFile(certificatePath, zos);
        zos.close();
        fos.close();
    }

    public static void main(String[] args) {
        if (args.length != 7 && args.length != 8) {
            System.err.println("Usage: java -jar packager.jar <serviceId> <serviceJarPath> <contextModelPath> <keystorePath> <keystorePw> <outputFolder> <manifestPath | developerId versionNumber>");
            System.exit(-1);
        }
        String serviceId = args[0];
        String serviceJarPath = args[1];
        String contextModelPath = args[2];
        String keystorePath = args[3];
        String keystorePw = args[4];
        String outputFolder = args[5];
        String manifestPath = null;
        String developerId = null;
        String versionNumber = null;
        if (args.length > 7) {
            developerId = args[6];
            versionNumber = args[7];
        } else {
            manifestPath = args[6];
        }
        JsonServiceManifest manifest = null;
        KeyStore keyStore = null;
        X509Certificate serviceCertificate = null;
        Path path = Paths.get(outputFolder);
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            System.err.println("Couldn't create folder " + outputFolder);
            System.exit(1);
        }
        if (manifestPath != null) {
            try {
                manifest = Utils.loadJsonFile(manifestPath, JsonServiceManifest.class);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Couldn't load manifest file from " + manifestPath);
                System.exit(1);
            }
            if (manifest.getExecutableHash() == null)
                try {
                    String executableHash = SecurityUtils.hashFile(serviceJarPath);
                    manifest.setExecutableHash(executableHash);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Couldn't read the file at " + serviceJarPath);
                    System.exit(1);
                }
        } else {
            try {
                manifest = createServiceManifest(serviceId, developerId, versionNumber, serviceJarPath, contextModelPath);
                Utils.saveToJsonFile(path.toString() + File.separator + serviceId + ".json", manifest);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Couldn't create service manifest");
                System.exit(1);
            }
        }
        try {
            keyStore = SecurityUtils.loadKeyStore(keystorePath, keystorePw);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Couldn't load keystore at " + keystorePath);
            System.exit(1);
        }
        try {
            serviceCertificate = signService(serviceId, keyStore, keystorePw);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Couldn't create certificate for service " + serviceId);
            System.exit(1);
        }
        try {
            SecurityUtils.saveToFile(serviceCertificate, path.toString() + File.separator + serviceId + ".crt");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Couldn't save certificate for service " + serviceId);
            System.exit(1);
        }
        try {
            createServicePackage(serviceId, serviceJarPath, path

                    .toString() + File.separator + serviceId + ".zip", manifest, contextModelPath, path

                    .toString() + File.separator + serviceId + ".crt");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Couldn't create service package");
            System.exit(1);
        }
        System.out.println("Created service package!");
    }

    public static X509Certificate signService(String serviceId, KeyStore keyStore, String password) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        Certificate rootCertificate = keyStore.getCertificate(serviceId);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(serviceId, password.toCharArray());
        PublicKey publicKey = rootCertificate.getPublicKey();
        SelfSignedCertificateBuilder selfSignedCertificateBuilder = new SelfSignedCertificateBuilder(serviceId, publicKey, privateKey);
        selfSignedCertificateBuilder.addDefaultExtensions();
        return (X509Certificate) selfSignedCertificateBuilder.build();
    }
}
