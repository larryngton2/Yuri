package ddlc.yuri.api.gui.click.novoline.config;

import ddlc.yuri.api.config.Config;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.api.config.GithubConfigFetcher;
import ddlc.yuri.api.gui.click.novoline.CategoryTab;
import ddlc.yuri.api.gui.click.novoline.GuiTheme;
import ddlc.yuri.managers.impl.ProgressBarManager;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.progress.ProgressBarEntry;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConfigTab extends CategoryTab {

    private static final File AUTO_LOAD_FILE = new File(Minecraft.getMinecraft().mcDataDir, "yuri_autoload.txt");
    private static String autoLoadConfigPath = loadAutoLoadPath();
    private static boolean hasAutoLoaded = false;

    private final List<ConfigEntry> configs = new CopyOnWriteArrayList<>();
    private final Map<ConfigEntry, String> remotePathMap = new HashMap<>();
    private ConfigTextField newConfigName;
    private ConfigEntry selectedConfig;

    public ConfigTab(float posX, float posY) {
        super(null, posX, posY);
        checkAutoLoad();
        refreshConfigs();
    }

    private static void checkAutoLoad() {
        if (!hasAutoLoaded && autoLoadConfigPath != null && !autoLoadConfigPath.isEmpty()) {
            hasAutoLoaded = true;
            triggerConfigDownload(autoLoadConfigPath, null);
        }
    }

    private static String loadAutoLoadPath() {
        try {
            if (AUTO_LOAD_FILE.exists()) {
                return new String(Files.readAllBytes(AUTO_LOAD_FILE.toPath())).trim();
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static void saveAutoLoadPath(String path) {
        try {
            Files.write(AUTO_LOAD_FILE.toPath(), path.getBytes());
        } catch (Throwable ignored) {
        }
    }

    private void triggerConfigDownload(String path) {
        triggerConfigDownload(path, this::refreshConfigs);
    }

    private static void triggerConfigDownload(String path, Runnable onComplete) {
        if (path == null || path.isEmpty()) {
            return;
        }
        String name = new File(path).getName();
        float cx = Minecraft.getMinecraft().displayWidth / 2f;
        float cy = 30f;
        ProgressBarEntry entry = ProgressBarManager.add(0f, cx, cy);
        new Thread(() -> {
            boolean ok = GithubConfigFetcher.downloadRemoteConfigWithProgress(path, entry);
            if (ok) {
                ConfigManager.getInstance().loadConfig(name);
            }
            ProgressBarManager.remove(entry);
            if (onComplete != null) {
                Minecraft.getMinecraft().addScheduledTask(onComplete);
            }
        }, "yuri-config-download").start();
    }

    @Override
    public String drawScreen(int mouseX, int mouseY, float animationProgress) {
        float offsetY = (1.0F - animationProgress) * -18.0F;
        float drawX = getPosX();
        float drawY = getPosY() + offsetY;
        int headerAlpha = (int) (255 * animationProgress);

        int height = 0;
        if (opened) {
            for (ConfigEntry config : configs) {
                height += config.getEntryHeight();
            }
            height += 1;
        }

        net.minecraft.client.gui.Gui.drawRect(drawX - 1, drawY, drawX + 101, drawY + 15 + height,
                RenderUtils.withAlpha(GuiTheme.PANEL, headerAlpha));
        FontUtils.getFont("sf", 21).drawStringWithShadow(
                "Configs",
                drawX + 4,
                drawY + 4,
                new Color(255, 255, 255, headerAlpha).getRGB()
        );

        if (opened) {
            configs.forEach(config -> config.drawScreen(mouseX, mouseY, animationProgress));
        }

        return null;
    }

    public void refreshConfigs() {
        configs.clear();
        remotePathMap.clear();
        selectedConfig = null;

        for (Config config : ConfigManager.getInstance().getElements()) {
            configs.add(new ConfigItem(config.getName(), this));
        }

        newConfigName = new ConfigTextField("Config name", this);
        configs.add(newConfigName);

        configs.add(new ConfigButton("Load", this, configName -> {
            if (configName.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().loadConfig(configName);
        }));

        configs.add(new ConfigButton("Save", this, ignored -> {
            String name = selectedConfig instanceof ConfigItem
                    ? selectedConfig.getName()
                    : newConfigName.getValue();
            if (name == null || name.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().saveConfig(name);
            refreshConfigs();
        }));

        configs.add(new ConfigButton("Delete", this, configName -> {
            if (configName.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().deleteConfig(configName);
            refreshConfigs();
        }));

        configs.add(new ConfigButton("Auto Load", this, ignored -> {
            if (selectedConfig != null && remotePathMap.containsKey(selectedConfig)) {
                String path = remotePathMap.get(selectedConfig);
                if (path != null) {
                    if (path.equals(autoLoadConfigPath)) {
                        autoLoadConfigPath = "";
                    } else {
                        autoLoadConfigPath = path;
                    }
                    saveAutoLoadPath(autoLoadConfigPath);
                    refreshConfigs();
                }
            }
        }));

        try {
            List<String> remote = GithubConfigFetcher.listRemoteConfigs();
            if (remote != null && !remote.isEmpty()) {
                configs.add(new ConfigButton("Online configs", this, ignored -> {
                }));
                for (String path : remote) {
                    final String p = path;
                    final String name = new File(p).getName();
                    boolean isAuto = p.equals(autoLoadConfigPath);
                    String label = (isAuto ? "[Auto] " : "") + "↳ " + name;
                    ConfigButton onlineBtn = new ConfigButton(label, this, configName -> {
                        triggerConfigDownload(p);
                    });
                    remotePathMap.put(onlineBtn, p);
                    configs.add(onlineBtn);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
        }

        if (opened) {
            configs.forEach(config -> config.mouseClicked(mouseX, mouseY, mouseButton));
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        configs.forEach(config -> config.keyTyped(typedChar, keyCode));
    }

    public List<ConfigEntry> getConfigs() {
        return configs;
    }

    public ConfigEntry getSelectedConfig() {
        return selectedConfig;
    }

    public void setSelectedConfig(ConfigEntry selectedConfig) {
        this.selectedConfig = selectedConfig;
    }
}