package com.mcmodsync.client;

import com.mcmodsync.McModSync;
import com.mcmodsync.network.ModFileRequestPayload;
import com.mcmodsync.network.ModListPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

public class ModSyncClient {
    private static final Map<String, FileDownload> downloads = new HashMap<>();
    private static boolean needsRestart = false;

    /**
     * 处理从服务器收到的 mod 列表
     */
    public static void handleModList(List<ModListPayload.ModInfo> serverMods, IPayloadContext context) {
        try {
            McModSync.LOGGER.info("Processing mod list from server...");

            // 获取客户端已安装的 mod
            Map<String, String> clientMods = getClientMods();

            // 找出缺失的 mod
            List<ModListPayload.ModInfo> missingMods = new ArrayList<>();
            for (ModListPayload.ModInfo serverMod : serverMods) {
                String clientHash = clientMods.get(serverMod.fileName());
                if (clientHash == null || !clientHash.equals(serverMod.sha256())) {
                    missingMods.add(serverMod);
                    McModSync.LOGGER.info("Missing or outdated mod: {}", serverMod.fileName());
                }
            }

            if (missingMods.isEmpty()) {
                McModSync.LOGGER.info("All server mods are already installed");
                showMessage("§a[Mod Sync] All mods up to date!");
                return;
            }

            McModSync.LOGGER.info("Found {} missing mods, requesting downloads...", missingMods.size());
            
            // 计算总大小
            long totalSize = missingMods.stream().mapToLong(ModListPayload.ModInfo::fileSize).sum();
            long totalSizeMB = totalSize / (1024 * 1024);
            
            showMessage("§e[Mod Sync] Downloading " + missingMods.size() + " mods (" + totalSizeMB + " MB)...");
            McModSync.LOGGER.info("Total download size: {} MB", totalSizeMB);

            // 请求下载缺失的 mod
            for (ModListPayload.ModInfo mod : missingMods) {
                downloads.put(mod.fileName(), new FileDownload(mod.fileName(), mod.fileSize()));
                McModSync.LOGGER.info("Requesting download: {} ({} MB)", 
                    mod.fileName(), mod.fileSize() / (1024 * 1024));
                PacketDistributor.sendToServer(new ModFileRequestPayload(mod.fileName()));
            }

        } catch (Exception e) {
            McModSync.LOGGER.error("Failed to process mod list", e);
        }
    }

    /**
     * 处理从服务器收到的文件数据
     */
    public static void handleFileData(String fileName, byte[] data, int chunkIndex, int totalChunks) {
        try {
            FileDownload download = downloads.get(fileName);
            if (download == null) {
                McModSync.LOGGER.warn("Received data for unknown file: {}", fileName);
                return;
            }

            download.addChunk(chunkIndex, data);
            
            // 每收到 50 个分块显示一次进度
            if ((chunkIndex + 1) % 50 == 0) {
                int progress = (int) ((chunkIndex + 1) * 100.0 / totalChunks);
                McModSync.LOGGER.info("Downloading {}: {}% ({}/{})", 
                    fileName, progress, chunkIndex + 1, totalChunks);
            }

            if (download.isComplete(totalChunks)) {
                saveModFile(fileName, download.getData());
                downloads.remove(fileName);

                McModSync.LOGGER.info("Downloaded mod: {} ({} chunks)", fileName, totalChunks);
                showMessage("§a[Mod Sync] Downloaded: " + fileName);

                if (downloads.isEmpty()) {
                    needsRestart = true;
                    showMessage("§a[Mod Sync] All mods downloaded! Please restart your game.");
                }
            }

        } catch (Exception e) {
            McModSync.LOGGER.error("Failed to handle file data", e);
        }
    }

    private static Map<String, String> getClientMods() {
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
                        String fileName = p.getFileName().toString();
                        String hash = calculateSHA256(p);
                        mods.put(fileName, hash);
                    } catch (Exception e) {
                        McModSync.LOGGER.warn("Failed to process client mod: {}", p.getFileName(), e);
                    }
                });
        } catch (Exception e) {
            McModSync.LOGGER.error("Failed to scan client mods", e);
        }

        return mods;
    }

    private static void saveModFile(String fileName, byte[] data) throws Exception {
        Path modsDir = FMLPaths.MODSDIR.get();
        if (!Files.exists(modsDir)) {
            Files.createDirectories(modsDir);
        }

        Path targetFile = modsDir.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
            fos.write(data);
        }

        McModSync.LOGGER.info("Saved mod file: {}", targetFile);
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

    private static void showMessage(String message) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
            }
        });
    }

    private static class FileDownload {
        private final String fileName;
        private final long fileSize;
        private final Map<Integer, byte[]> chunks = new HashMap<>();

        public FileDownload(String fileName, long fileSize) {
            this.fileName = fileName;
            this.fileSize = fileSize;
        }

        public void addChunk(int index, byte[] data) {
            chunks.put(index, data);
        }

        public boolean isComplete(int totalChunks) {
            return chunks.size() == totalChunks;
        }

        public byte[] getData() {
            int totalSize = 0;
            for (byte[] chunk : chunks.values()) {
                totalSize += chunk.length;
            }

            byte[] result = new byte[totalSize];
            int offset = 0;

            List<Integer> indices = new ArrayList<>(chunks.keySet());
            Collections.sort(indices);

            for (int index : indices) {
                byte[] chunk = chunks.get(index);
                System.arraycopy(chunk, 0, result, offset, chunk.length);
                offset += chunk.length;
            }

            return result;
        }
    }
}
