package com.csl.octa.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.MessageDigest;

public class PackListener implements Listener {

    private static final String GITHUB_API_URL =
            "https://api.github.com/repos/TesseractCreations/Tesseract-Resourcepack/releases/latest";

    private String cachedPackUrl = null;
    private byte[] cachedHash = null;
    private long lastFetch = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        fetchAndApply(event);
    }

    private void fetchAndApply(PlayerJoinEvent event) {
        // Run async so we don't block the main thread
        event.getPlayer().getServer().getScheduler().runTaskAsynchronously(
                event.getPlayer().getServer().getPluginManager().getPlugin("Octa"), // change to your plugin name
                () -> {
                    try {
                        long now = System.currentTimeMillis();

                        // Only re-fetch from GitHub if cache expired
                        if (cachedPackUrl == null || (now - lastFetch) > CACHE_DURATION) {
                            String[] result = fetchLatestPackUrl();
                            if (result == null) {
                                event.getPlayer().kick(net.kyori.adventure.text.Component.text(
                                        "§cFailed to fetch resource pack. Please rejoin."));
                                return;
                            }
                            cachedPackUrl = result[0];
                            cachedHash = computeSHA1(cachedPackUrl);
                            lastFetch = now;
                        }

                        final String url = cachedPackUrl;
                        final byte[] hash = cachedHash;

                        // Must send the resource pack on the main thread
                        event.getPlayer().getServer().getScheduler().runTask(
                                event.getPlayer().getServer().getPluginManager().getPlugin("Octa"),
                                () -> event.getPlayer().setResourcePack(
                                        url,
                                        hash,
                                        net.kyori.adventure.text.Component.text(
                                                "§6You must accept the resource pack to play on this server."),
                                        true // forced — kicks if declined
                                )
                        );

                    } catch (Exception e) {
                        e.printStackTrace();
                        event.getPlayer().getServer().getScheduler().runTask(
                                event.getPlayer().getServer().getPluginManager().getPlugin("Octa"),
                                () -> event.getPlayer().kick(net.kyori.adventure.text.Component.text(
                                        "§cFailed to apply resource pack. Please rejoin."))
                        );
                    }
                }
        );
    }

    /**
     * Hits the GitHub releases API and finds the first .zip asset download URL.
     */
    private String[] fetchLatestPackUrl() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(GITHUB_API_URL).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "Octa-Plugin");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() != 200) {
                System.err.println("[Octa] GitHub API returned status: " + conn.getResponseCode());
                return null;
            }

            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            JsonObject release = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();

            JsonArray assets = release.getAsJsonArray("assets");
            if (assets == null || assets.isEmpty()) {
                System.err.println("[Octa] No assets found in latest release.");
                return null;
            }

            for (var element : assets) {
                JsonObject asset = element.getAsJsonObject();
                String name = asset.get("name").getAsString();
                if (name.endsWith(".zip")) {
                    String downloadUrl = asset.get("browser_download_url").getAsString();
                    return new String[]{downloadUrl};
                }
            }

            System.err.println("[Octa] No .zip asset found in latest release.");
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Downloads the file at the given URL and computes its SHA-1 hash.
     * Minecraft requires the SHA-1 hash for resource pack verification.
     */
    private byte[] computeSHA1(String fileUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(fileUrl).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Octa-Plugin");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(true);

        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        try (var inputStream = conn.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        return digest.digest();
    }
}