package com.mcmodsync.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcmodsync.McModSync;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModDownloader {
    private static final Gson GSON = new Gson();
    private final String serverUrl;

    public ModDownloader(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
    }

    public int syncMods(ProgressCallback callback) throws Exception {
        // 1. 获取服务器 mod 列表
        List<ModInfo> serverMods = fetchModList();
        if (serverMods.isEmpty()) {
            McModSync.LOGGER.info("Server has no mods to sync");
            return 0;
        }

        // 2. 获取本地 mod
        Map<String, String> localMods = getLocalMods();

        // 3. 找出需要下载的 mod
        List<ModInfo> toDownload = new ArrayList<>();
        for (ModInfo mod : serverMods) {
            String localHash = localMods.get(mod.fileName);
            if (localHash == null || !localHash.equals(mod.sha256)) {
                toDownload.add(mod);
            }
        }

        if (toDownload.isEmpty()) {
            return 0;
        }

        // 4. 下载 mod
        McModSync.LOGGER.info("Downloading {} mods", toDownload.size());
        int downloaded = 0;
        for (int i = 0; i < toDownload.size(); i++) {
            ModInfo mod = toDownload.get(i);
            callback.onProgress(i + 1, toDownload.size(), mod.fileName);
            
            if (downloadMod(mod)) {
                downloaded++;
            }
        }

        return downloaded;
    }

    private List<ModInfo> fetchModList() throws IOException {
        URL url = new URL(serverUrl + "api/mods");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("Server returned: " + code);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            JsonArray array = GSON.fromJson(reader, JsonArray.class);
            List<ModInfo> mods = new ArrayList<>();
            
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                mods.add(new ModInfo(
                    obj.get("fileName").getAsString(),
                    obj.get("sha256").getAsString(),
                    obj.get("downloadUrl").getAsString()
                ));
            }
            
            return mods;
        }
    }

    private Map<String, String> getLocalMods() {
        Map<String, String> mods = new HashMap<>();
        Path modsDir = FMLPaths.MODSDIR.get();

        if (!Files.exists(modsDir)) {
            return mods;
        }

        try {
            Files.list(modsDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .forEach(p -> {
                    try {
                        String hash = calculateSHA256(p);
                        mods.put(p.getFileName().toString(), hash);
                    } catch (Exception e) {
                        McModSync.LOGGER.warn("Failed to hash: {}", p.getFileName(), e);
                    }
                });
        } catch (IOException e) {
            McModSync.LOGGER.error("Failed to list mods", e);
        }

        return mods;
    }

    private boolean downloadMod(ModInfo mod) {
        try {
            McModSync.LOGGER.info("Downloading: {}", mod.fileName);
            
            // URL 已经在服务器端编码好了，直接使用
            URL url = new URL(mod.downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Charset", "UTF-8");

            Path modsDir = FMLPaths.MODSDIR.get();
            if (!Files.exists(modsDir)) {
                Files.createDirectories(modsDir);
            }

            Path tempFile = modsDir.resolve(mod.fileName + ".tmp");
            Path targetFile = modsDir.resolve(mod.fileName);

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // 验证哈希
            String downloadedHash = calculateSHA256(tempFile);
            if (!downloadedHash.equals(mod.sha256)) {
                Files.delete(tempFile);
                throw new IOException("Hash mismatch for " + mod.fileName);
            }

            Files.move(tempFile, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            McModSync.LOGGER.info("Downloaded: {}", mod.fileName);
            return true;

        } catch (Exception e) {
            McModSync.LOGGER.error("Failed to download: {}", mod.fileName, e);
            return false;
        }
    }

    private String calculateSHA256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileData = Files.readAllBytes(file);
        byte[] hashBytes = digest.digest(fileData);

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public interface ProgressCallback {
        void onProgress(int current, int total, String fileName);
    }

    private static class ModInfo {
        final String fileName;
        final String sha256;
        final String downloadUrl;

        ModInfo(String fileName, String sha256, String downloadUrl) {
            this.fileName = fileName;
            this.sha256 = sha256;
            this.downloadUrl = downloadUrl;
        }
    }
}
