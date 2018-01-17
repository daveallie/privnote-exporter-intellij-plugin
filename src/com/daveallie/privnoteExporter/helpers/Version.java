package com.daveallie.privnoteExporter.helpers;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;

public class Version {
    public static String CURRENT_VERSION = PluginManager.getPlugin(PluginId.getId("com.daveallie.privnoteExporter")).getVersion();
}
