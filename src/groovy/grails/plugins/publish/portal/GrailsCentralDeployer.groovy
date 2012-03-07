package grails.plugins.publish.portal

import grails.plugins.publish.PluginDeployer
import grails.plugins.rest.client.RestBuilder
import org.springframework.core.io.*
/**
 * A deployer capable of deploying to the central repository at http://grails.org. This implementation uses the grails.org REST API
 * to implement the PluginDeployer interface.
 *
 * @author Graeme Rocher
 */
class GrailsCentralDeployer implements PluginDeployer {
    //private proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8888))
    RestBuilder rest = new RestBuilder(connectTimeout: 1000, readTimeout:10000, proxy:null )
    String portalUrl = "http://grails.org"
    String username
    String password
    
    boolean isVersionAlreadyPublished(File pomFile) {
        def (pluginName, pluginVersion) = parsePom(pomFile)
        def resp = rest.get("$portalUrl/api/v1.0/plugin/$pluginName/$pluginVersion")   

        return resp?.status == 200

    }
    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile, boolean isRelease) {
        def (pluginName, pluginVersion) = parsePom(pomFile)
        def url = "$portalUrl/api/v1.0/publish/$pluginName/$pluginVersion"
        def resp = rest.post(url) {
            auth username, password
            contentType "multipart/form-data"
            zip = new FileSystemResource(pluginPackage)
            pom = new FileSystemResource(pomFile)
            xml = new FileSystemResource(pluginXmlFile)
        }
        if(resp.status != 200) {
            throw new RuntimeException( "Server returned error deploying to Grails central repository: ${resp.status}" )
        } else {
            println "Plugin successfully published."
        }
    }
    /**
     * Parses the given POM file (must have a 'text' property) and returns
     * a tuple of the plugin name and version (in that order).
     */
    protected final parsePom(pomFile) {
        def pom = new XmlSlurper().parseText(pomFile.text)
        return [pom.artifactId.text(), pom.version.text()]
    }
}
