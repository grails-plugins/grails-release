package grails.plugins.publish.maven

import grails.plugins.publish.PluginDeployer

/**
 * Implementation of {@link PluginDeployer} that deploys plugin packages
 * to a Maven-compatible repository using the Maven Ant tasks.
 */
class MavenDeployer implements PluginDeployer {
    def ant
    def mavenTasks
    def repoDefn
    def protocol

    MavenDeployer(ant, repoDefinition, protocol) {
        this.ant = ant
        this.mavenTasks = groovy.xml.NamespaceBuilder.newInstance(ant, 'antlib:org.apache.maven.artifact.ant')
        this.repoDefn = repoDefinition
        this.protocol = protocol
    }
    
    /**
     * Deploys the given plugin package to the Maven repository configured
     * at object instantiation.
     * @param pluginPackage The location of the plugin zip file.
     * @param pluginXmlFile The location of the XML plugin descriptor.
     * @param pomFile The location of the POM (pom.xml).
     */
    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile) {
        mavenTasks.'install-provider'(artifactId: protocol, version: "1.0-beta-2")
        mavenTasks.deploy(file: pluginPackage) {
            attach file: pluginXmlFile, type:"xml", classifier: "plugin"
            pom(file: pomFile)
            
            if (repoDefn.configurer) {
                remoteRepository(repoDefn.args, repoDefn.configurer)
            }
            else {
                remoteRepository(repoDefn.args)
            }
        }
    }
}
