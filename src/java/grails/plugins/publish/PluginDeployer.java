package grails.plugins.publish;

import java.io.File;
import groovy.util.ConfigObject;

public interface PluginDeployer {
    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile);
}

