package grails.plugins.publish;

import java.io.File;

public interface PluginDeployer {
    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile, boolean isRelease);
}
