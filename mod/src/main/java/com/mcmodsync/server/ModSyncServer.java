package com.mcmodsync.server;

import com.mcmodsync.McModSync;
import com.mcmodsync.network.ModFileDataPayload;
import com.mcmodsync.network.ModListPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class ModSyncServer {
    private static final int CHUNK_SIZE = 30000; // 每个数据包发送 30KB

    /**
     * 玩家登录时，发送服务器 mod 列表
     */
    public static void sendModListToPlayer(ServerPlayer player) {
        try {
            List<ModListPayload.ModInfo> mods = scanServerMods();
            McModSync.LOGGER.info("Sending mod list to player {}: {} mods", player.getName().getString(), mods.size());
            PacketDistributor.sendToPlayer(player, new ModListPayload(mods));
        } catch (Exception e) {
            McModSync.LOGGER.error("Failed to send mod list to player", e);
        }
    }

    /**
     * 扫描服务器 mods 目录
     */
    public static List<ModListPayload.ModInfo> scanServerMods() {
        List<ModListPayload.ModInfo> mods = new ArrayList<>();
        Path modsDir = FMLPaths.MODSDIR.get();

        if (!Files.exists(modsDir)) {
            return mods;
        }

        try {
            Files.list(modsDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(p -> !p.getFileName().toString().equals("mcmodsync-1.0.0.jar")) // 排除自己
                .forEach(p -> {
                    try {
                        String fileName = p.getFileName().toString();
                        String sha256 = calculateSHA256(p);
                        long fileSize = Files.size(p);
                        mods.add(new ModListPayload.ModInfo(fileName, sha256, fileSize));
                        McModSync.LOGGER.debug("Found mod: {} ({})", fileName, sha256);
                    } catch (Exception e) {
                        McModSync.LOGGER.warn("Failed to process mod: {}", p.getFileName(), e);
                    }
                });
        } catch (IOException e) {
            McModSync.LOGGER.error("Failed to scan mods directory", e);
        }

        return mods;
    }

    /**
     * 处理客户端的文件下载请求
     */
    public static void handleFileRequest(String fileName, ServerPlayer player) {
        try {
            Path modsDir = FMLPaths.MODSDIR.get();
            Path modFile = modsDir.resolve(fileName);

            if (!Files.exists(modFile)) {
                McModSync.LOGGER.error("Requested mod file not found: {}", fileName);
                return;
            }

            byte[] fileData = Files.readAllBytes(modFile);
            int totalChunks = (int) Math.ceil((double) fileData.length / CHUNK_SIZE);

            McModSync.LOGGER.info("Sending mod {} to player {} ({} chunks, {} bytes)",
                fileName, player.getName().getString(), totalChunks, fileData.length);

            // 分块发送文件
            for (int i = 0; i < totalChunks; i++) {
                int start = i * CHUNK_SIZE;
                int end = Math.min(start + CHUNK_SIZE, fileData.length);
                byte[] chunk = new byte[end - start];
                System.arraycopy(fileData, start, chunk, 0, chunk.length);

                PacketDistributor.sendToPlayer(player,
                    new ModFileDataPayload(fileName, chunk, i, totalChunks)
                );
            }

            McModSync.LOGGER.info("Finished sending mod {} to player", fileName);
        } catch (Exception e) {
            McModSync.LOGGER.error("Failed to send mod file: {}", fileName, e);
        }
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
}
