package grails.plugins.publish.portal

import grails.plugins.publish.PluginDeployer
import grails.plugins.rest.client.RestBuilder

/**
 * A deployer capable of deploying to the central repository at http://grails.org.
 * This implementation uses the grails.org REST API to implement the PluginDeployer interface.
 *
 * @author Graeme Rocher
 */
class GrailsCentralDeployer implements PluginDeployer {

    //private proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8888))
    RestBuilder rest = new RestBuilder(connectTimeout: 1000, readTimeout: 10000, proxy: null)
    String portalUrl = "http://grails.org/plugins"
    String username
    String password

    boolean isVersionAlreadyPublished(File pomFile) {
        def (pluginName, pluginVersion) = parsePom(pomFile)
        def resp = rest.get("$portalUrl/api/v1.0/plugin/$pluginName/$pluginVersion")   
        return resp?.status == 200
    }

    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile, boolean isRelease) {
        def (pluginName, pluginVersion) = parsePom(pomFile)
        def base = new URL(portalUrl)
        base = base.port > -1 ? "http://$base.host:$base.port" : "http://$base.host"
        def url = "$base/api/v1.0/publish/$pluginName/$pluginVersion"
        println "Publishing to $url"
        def resp = rest.post(url) {
            auth username, password
            contentType "multipart/form-data"
            accept "text/plain"
            zip = pluginPackage
            pom = pomFile
            xml = pluginXmlFile
        }

        switch (resp.status) {
        case 200:
            println "Plugin successfully published."
            break

        case 401:
            throw new RuntimeException("Repository authentication failed. Do you have an account " +
                    "and are your username and password correct?")
            break

        case 403:
            throw new RuntimeException(resp.text)
            break

        default:
            throw new RuntimeException("Server error deploying to Grails central repository " +
                    "(status ${resp.status}):\n${resp.text}")
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
