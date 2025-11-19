package com.mcmodsync.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcmodsync.McModSync;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class HttpApiServer {
    private static final int PORT = 56552;
    private static final Gson GSON = new Gson();
    private static HttpServer server;

    public static void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            server.createContext("/api/mods", new ModListHandler());
            server.createContext("/api/download/", new ModDownloadHandler());
            
            server.setExecutor(null); // 使用默认线程池
            server.start();
            
            McModSync.LOGGER.info("HTTP API Server started on port {}", PORT);
            McModSync.LOGGER.info("API endpoint: http://localhost:{}/api/mods", PORT);
        } catch (IOException e) {
            McModSync.LOGGER.error("Failed to start HTTP API server", e);
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            McModSync.LOGGER.info("HTTP API Server stopped");
        }
    }

    static class ModListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                List<ModInfo> mods = scanMods();
                JsonArray array = new JsonArray();
                
                for (ModInfo mod : mods) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("fileName", mod.fileName);
                    obj.addProperty("sha256", mod.sha256);
                    obj.addProperty("fileSize", mod.fileSize);
                    obj.addProperty("downloadUrl", 
                        "http://localhost:" + PORT + "/api/download/" + urlEncode(mod.fileName));
                    array.add(obj);
                }

                String response = GSON.toJson(array);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, bytes.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }

                McModSync.LOGGER.info("Sent mod list: {} mods", mods.size());
            } catch (Exception e) {
                McModSync.LOGGER.error("Error handling mod list request", e);
                String error = "{\"error\":\"" + e.getMessage() + "\"}";
                byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            }
        }
    }

    static class ModDownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                String path = exchange.getRequestURI().getPath();
                String fileName = URLDecoder.decode(path.substring("/api/download/".length()), 
                    StandardCharsets.UTF_8);

                Path modsDir = FMLPaths.MODSDIR.get();
                Path modFile = modsDir.resolve(fileName);

                if (!Files.exists(modFile) || !Files.isRegularFile(modFile)) {
                    String error = "File not found: " + fileName;
                    byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(404, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.getResponseBody().close();
                    return;
                }

                byte[] fileData = Files.readAllBytes(modFile);

                exchange.getResponseHeaders().set("Content-Type", "application/java-archive");
                exchange.getResponseHeaders().set("Content-Disposition", 
                    "attachment; filename*=UTF-8''" + urlEncode(fileName));
                exchange.sendResponseHeaders(200, fileData.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileData);
                }

                McModSync.LOGGER.info("Downloaded: {} ({} bytes)", fileName, fileData.length);
            } catch (Exception e) {
                McModSync.LOGGER.error("Error handling download request", e);
                String error = "Error: " + e.getMessage();
                byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            }
        }
    }

    private static List<ModInfo> scanMods() throws IOException {
        List<ModInfo> mods = new ArrayList<>();
        Path modsDir = FMLPaths.MODSDIR.get();

        if (!Files.exists(modsDir)) {
            return mods;
        }

        Files.list(modsDir)
            .filter(p -> p.toString().endsWith(".jar"))
            .filter(p -> !p.getFileName().toString().equals("mcmodsync-1.0.0.jar"))
            .forEach(p -> {
                try {
                    String fileName = p.getFileName().toString();
                    String sha256 = calculateSHA256(p);
                    long fileSize = Files.size(p);
                    mods.add(new ModInfo(fileName, sha256, fileSize));
                    McModSync.LOGGER.debug("Found mod: {} - {}", fileName, sha256.substring(0, 16));
                } catch (Exception e) {
                    McModSync.LOGGER.warn("Failed to process mod: {}", p.getFileName(), e);
                }
            });

        return mods;
    }

    private static String calculateSHA256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileData = Files.readAllBytes(file);
        byte[] hashBytes = digest.digest(fileData);

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String urlEncode(String str) {
        try {
            return java.net.URLEncoder.encode(str, StandardCharsets.UTF_8)
                .replace("+", "%20");
        } catch (Exception e) {
            return str;
        }
    }

    private static class ModInfo {
        final String fileName;
        final String sha256;
        final long fileSize;

        ModInfo(String fileName, String sha256, long fileSize) {
            this.fileName = fileName;
            this.sha256 = sha256;
            this.fileSize = fileSize;
        }
    }
}
