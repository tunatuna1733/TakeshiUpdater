// Big Thanks to VolcAddons, most of the codes are from them

package com.github.tunatuna1733.takeshiupdater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(modid = TakeshiUpdater.MODID, useMetadata=true, clientSideOnly = true)
public class TakeshiUpdater {
    public static TakeshiUpdater INSTANCE;
    public static final String MODID = "takeshiupdater";

    String modDir = System.getProperty("user.home");
    private static final String UPDATE_CHECK_URL = "https://api.github.com/repos/tunatuna1733/TakeshiAddons/releases/latest";
    private boolean updateChecked = false;
    private String latestVersion = "0.0.1";
    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        INSTANCE = this;
        EventBus eventBus = MinecraftForge.EVENT_BUS;
        eventBus.register(this);
        ClientCommandHandler.instance.registerCommand(new CommandUpdateTakeshi());
        modDir = event.getModConfigurationDirectory().getAbsolutePath();
        logger = event.getModLog();
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (!updateChecked) {
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
                    checkForUpdates(player);
                    updateChecked = true;
                }
            }, 3, TimeUnit.SECONDS);
            scheduledExecutorService.shutdown();
        }
    }

    private void checkForUpdates(EntityPlayer player) {
        try {
            URL url = new URL(UPDATE_CHECK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder res = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    res.append(line);
                }
                reader.close();
                connection.disconnect();

                String jsonResString = res.toString();
                JsonParser jsonParser = new JsonParser();
                JsonObject jsonRes = jsonParser.parse(jsonResString).getAsJsonObject();
                latestVersion = jsonRes.get("tag_name").getAsString();

                String metaDataFilePath = modDir + File.separator + "ChatTriggers" + File.separator + "modules" + File.separator + "TakeshiAddons" + File.separator + "metadata.json";
                String currentVersion = getCurrentVersion(metaDataFilePath);

                logger.info(latestVersion);
                logger.info(currentVersion);

                if (isNewerVersion(latestVersion, currentVersion)) {
                    player.addChatMessage(new ChatComponentText(
                            EnumChatFormatting.GOLD + "[" +
                                    EnumChatFormatting.GREEN + "TakeshiUpdater" +
                                    EnumChatFormatting.GOLD + "] " +
                                    EnumChatFormatting.AQUA + "A new update for TakeshiAddons is available! " +
                                    EnumChatFormatting.GREEN + EnumChatFormatting.BOLD + "v" + latestVersion
                    ));
                    player.addChatMessage(new ChatComponentText(
                            EnumChatFormatting.GOLD + "[" +
                                    EnumChatFormatting.GREEN + "TakeshiUpdater" +
                                    EnumChatFormatting.GOLD + "] " +
                                    EnumChatFormatting.GREEN + "Click here to update!"
                    ).setChatStyle(new ChatStyle().setChatClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/updatetakeshi") {
                                @Override
                                public Action getAction() {
                                    return Action.RUN_COMMAND;
                                }
                            }
                    )));
                }
            }
        } catch (MalformedURLException e) {
            player.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GOLD + "[" +
                            EnumChatFormatting.GREEN + "TakeshiUpdater" +
                            EnumChatFormatting.GOLD + "]" +
                            EnumChatFormatting.RED + " [ERROR]MalformedURLException: " + e.getMessage()
            ));
            logger.error("MalformedURLException in checkForUpdates\n" + e.getMessage());
        } catch (IOException e) {
            player.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GOLD + "[" +
                            EnumChatFormatting.GREEN + "TakeshiUpdater" +
                            EnumChatFormatting.GOLD + "]" +
                            EnumChatFormatting.RED + " [ERROR]IOException: " + e.getMessage()
            ));
            logger.error("IOException in checkForUpdates\n" + e.getMessage());
        }
    }

    private String getCurrentVersion(String metaDataFilePath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(metaDataFilePath));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            JsonObject metaDataContent = new JsonParser().parse(builder.toString()).getAsJsonObject();
            return metaDataContent.get("version").getAsString();
        } catch (FileNotFoundException e) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GOLD + "[" +
                            EnumChatFormatting.GREEN + "TakeshiUpdater" +
                            EnumChatFormatting.GOLD + "]" +
                            EnumChatFormatting.RED + " [ERROR]Metadata file not found: " + e.getMessage()
            ));
            logger.error("FileNotFoundException in getCurrentVersion\n" + e.getMessage());
            return "0.0.1";
        } catch (IOException e) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GOLD + "[" +
                            EnumChatFormatting.GREEN + "TakeshiUpdater" +
                            EnumChatFormatting.GOLD + "]" +
                            EnumChatFormatting.RED + " [ERROR]Could not read metadata file: " + e.getMessage()
            ));
            logger.error("IOException in getCurrentVersion\n" + e.getMessage());
            return "0.0.1";
        }
    }

    private boolean isNewerVersion(String version1, String version2) {
        String[] splitVersion1 = version1.split("\\.");
        String[] splitVersion2 = version2.split("\\.");

        for (int i = 0; i < Math.min(splitVersion1.length, splitVersion2.length); i++) {
            int v1 = Integer.parseInt(splitVersion1[i]);
            int v2 = Integer.parseInt(splitVersion2[i]);

            if (v1 > v2) return true;
            else if (v1 < v2) return false;
        }

        return splitVersion1.length > splitVersion2.length;
    }

    private void extractZipFile(File zipFile, File destDir) throws IOException{

        try (FileInputStream fileInputStream = new FileInputStream(zipFile); ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(fileInputStream)) {
            ZipArchiveEntry entry;

            while ((entry = (ZipArchiveEntry) zipArchiveInputStream.getNextEntry()) != null) {
                File entryFile = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(entryFile)) {
                        byte[] buf = new byte[1024];
                        int bytesRead;

                        while ((bytesRead = zipArchiveInputStream.read(buf)) != -1) {
                            fileOutputStream.write(buf, 0, bytesRead);
                        }
                    }
                }
            }
        }
    }

    public void downloadAndExtractUpdate(EntityPlayer player) {
        try {
            URL url = new URL(UPDATE_CHECK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder res = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    res.append(line);
                }
                reader.close();
                connection.disconnect();

                String jsonResString = res.toString();
                JsonParser jsonParser = new JsonParser();
                JsonObject jsonRes = jsonParser.parse(jsonResString).getAsJsonObject();
                String downloadUrl = jsonRes.getAsJsonArray("assets").get(0).getAsJsonObject().get("browser_download_url").getAsString();
                File modulesDir = new File(modDir + File.separator + "ChatTriggers" + File.separator + "modules");
                modulesDir.mkdirs();

                URL fileUrl = new URL(downloadUrl);
                String fileName = fileUrl.getFile().substring(fileUrl.getFile().lastIndexOf('/') + 1);
                File zipFile = new File(modulesDir, fileName);

                try (InputStream inputStream = fileUrl.openStream(); OutputStream outputStream = Files.newOutputStream(zipFile.toPath())) {
                    byte[] buf = new byte[1024];
                    int byteRead;
                    while ((byteRead = inputStream.read(buf)) != -1) {
                        outputStream.write(buf, 0, byteRead);
                    }
                } catch (IOException e) {
                    player.addChatMessage(new ChatComponentText(
                            EnumChatFormatting.GOLD + "[" +
                                    EnumChatFormatting.GREEN + "TakeshiUpdater" +
                                    EnumChatFormatting.GOLD + "]" +
                                    EnumChatFormatting.RED + " [ERROR]Could not download zipfile: " + e.getMessage()
                    ));
                    logger.error("IOException in downloadAndExtractUpdate\n" + e.getMessage());
                }

                extractZipFile(zipFile, modulesDir);
                zipFile.delete();

                player.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.GOLD + "[" +
                                EnumChatFormatting.GREEN + "TakeshiUpdater" +
                                EnumChatFormatting.GOLD + "] " +
                                EnumChatFormatting.GREEN + "Successfully updated TakeshiAddons to " +
                                EnumChatFormatting.GREEN + EnumChatFormatting.BOLD + "v" + latestVersion +
                                EnumChatFormatting.GREEN + "!"
                ));
                player.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.AQUA + "Click here to run '/ct load' for effects to take place."
                ).setChatStyle(new ChatStyle().setChatClickEvent(
                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ct load") {
                        @Override
                        public Action getAction() {
                            return Action.RUN_COMMAND;
                        }
                    }
                )));
            }
        } catch (MalformedURLException e) {
            player.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GOLD + "[" +
                            EnumChatFormatting.GREEN + "TakeshiUpdater" +
                            EnumChatFormatting.GOLD + "]" +
                            EnumChatFormatting.RED + " [ERROR]MalformedURLException: " + e.getMessage()
            ));
            logger.error("MalformedURLException in checkForUpdates\n" + e.getMessage());
        } catch (IOException e) {
            player.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GOLD + "[" +
                            EnumChatFormatting.GREEN + "TakeshiUpdater" +
                            EnumChatFormatting.GOLD + "]" +
                            EnumChatFormatting.RED + " [ERROR]IOException: " + e.getMessage()
            ));
            logger.error("IOException in checkForUpdates\n" + e.getMessage());
        }
    }

}
