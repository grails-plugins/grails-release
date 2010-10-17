import groovyx.net.http.HTTPBuilder
import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.groovy.grails.cli.CommandLineHelper

import static groovyx.net.http.Method.PUT
import static groovyx.net.http.ContentType.JSON

includeTargets << grailsScript("_GrailsPluginDev")
includeTargets << new File(mavenPublisherPluginDir, "scripts/_GrailsMaven.groovy")

USAGE = """
    publish-plugin [--repository=REPO] [--protocol=PROTOCOL] [--portal=PORTAL] [--dry-run] [--snapshot]

where
    REPO     = The name of a configured repository to deploy the plugin to. Can be
               a Subversion repository or a Maven-compatible one.
               (default: Grails Central Plugin Repository).

    PROTOCOL = The protocol to use when deploying to a Maven-compatible repository.
               Can be one of 'http', 'scp', 'scpexe', 'ftp', or 'webdav'.
               (default: 'http').
	           
    PORTAL   = The portal to inform of the plugin's release.
               (default: Grails Plugin Portal).
	           
    --dry-run  = Shows you what will happen when you publish the plugin, but doesn't
                 actually publish it.
	           
    --snapshot = Force this release to be a snapshot version, i.e. it isn't automatically
                 made the latest available release.
"""

target(default: "Publishes a plugin to either a Subversion or Maven repository.") {
    depends(parseArguments, processDefinitions, packagePlugin, generatePom)

    // Use the Grails Central Plugin repository as the default.
    def repoName = argsMap["repository"]
    def portalName = argsMap["portal"]
    def type = "svn"
    def url = "https://svn.codehaus.org/grails-plugins"
    if (repoName) {
        // First look for the repository definition for this name. This
        // could either be from the newer Maven-based definitions or the
        // legacy Subversion-based ones.
        def repoDefn = distributionInfo.remoteRepos[repoName]
        if (repoDefn) {
            type = repoDefn.args["type"] ?: "maven"
            url = repoDefn.args["url"]

            // If the repository defines a portal, then we should use that
            // ahead of the public Grails plugin portal (but not in preference
            // to one declared in the command arguments).
            if (!portalName) portalName = repoDefn.args["portal"]
        }
        else {
            type = "svn"
            url = grailsSettings.config.grails.plugin.repos.distribution."$repoName"
        }
        
        // Check that the repository is defined.
        if (url) {
            println "Publishing to ${type == 'svn' ? 'Subversion' : 'Maven'} repository '$repoName'"
        }
        else {
            println "No configuration found for repository '$repoName'"
            exit(1)
        }
    }
    else {
        println "Publishing to Grails Central"
    }

    def deployer
    if (argsMap["dry-run"]) {
        deployer = classLoader.loadClass("grails.plugins.publish.print.DryRunDeployer").newInstance()
    }
    else if (type == "svn") {
        // Helper class for getting user input from the command line.
        def inputHelper = new CommandLineHelper()

        // Create a deployer for Subversion and...
        def svnClient = classLoader.loadClass("grails.plugins.publish.svn.SvnClient").newInstance(url)
        deployer = classLoader.loadClass("grails.plugins.publish.svn.SvnDeployer").newInstance(
                svnClient,
                grailsSettings.projectWorkDir,
                new File(grailsSettings.grailsWorkDir, "plugins-list-${repoName}.xml"),
                System.out) { msg ->
            // This closure is executed whenever the deployer needs to
            // ask for user input.
            return inputHelper.userInput(msg)
        }
    }
    else if (type == "maven"){
        // Work out the protocol to use. This may be provided as a
        // '--protocol' argument on the command line or inferred from
        // the repository URL.
        def protocols = [
                http: "wagon-http",
                scp: "wagon-ssh",
                scpexe: "wagon-ssh-external",
                ftp: "wagon-ftp",
                webdav: "wagon-webdav" ]
        def protocol = protocols.http
        
        def repoDefn = distributionInfo.remoteRepos[repoName]
        repoDefn.args.remove "portal"

        if (argsMap["protocol"]) {
            protocol = protocols[argsMap["protocol"]]
        }
        else if (url) {
            def i = url.indexOf('://')
            if (i == -1) {
                println "Invalid URL for repository '$repoName': $url"
                exit(1)
                return 1
            }
            
            def urlProt = url[0..<i]
            if (protocols[urlProt]) {
                protocol = protocols[urlProt]
            }
            else {
                println "WARNING: unknown protocol '$urlProt' for repository '$repoName'"
            }
        }
        
        deployer = classLoader.loadClass("grails.plugins.publish.maven.MavenDeployer").newInstance(ant, repoDefn, protocol)
    }
    else {
        println "Unknown type '$type' defined for repository '$repoName'"
        exit(1)
    }
    
    // Read the plugin information from the POM.
    def pluginInfo = new XmlSlurper().parse(new File(pomFileLocation))
    def isRelease = !pluginInfo.version.text().endsWith("-SNAPSHOT")
    if (argsMap["snapshot"]) isRelease = false
    
    deployer.deployPlugin(new File(pluginZip), new File("plugin.xml"), new File(pomFileLocation), isRelease)

    // Ping the plugin portal with the details of this release.
    if (!argsMap["dry-run"]) {
        // What's the URL of the portal to ping? The explicit 'portal' argument
        // takes precedence, then the portal configured for the current repository,
        // and finally the public Grails plugin portal.
        def portalUrl = "http://grails.org/plugin/${pluginInfo.artifactId.text()}"
        if (portalName) {
            // Pick the configured portal with the given name, assuming one
            // exists with that name.
            portalUrl = distributionInfo.portals[portalName]

            if (!portalUrl) {
                println "No portal defined with ID '${portalName}'"
                println "Plugin has been published, but the plugin portal has not been notified."
                exit 1
            }

            // Add the plugin name to the URL.
            if (!portalUrl.endsWith('/')) portalUrl += '/'
            portalUrl += pluginInfo.artifactId.text()
        }

        // Now that we have a URL, simply send a PUT request with the appropriate
        // JSON content.
        println "Notifiying plugin portal '${portalUrl}' of release..."
        def inputHelper = new CommandLineHelper()
        def username = inputHelper.userInput("Username for portal (leave empty if authentication not required):")
        def password = inputHelper.userInput("Password for portal (leave empty if authentication not required):")

        def http = new HTTPBuilder(portalUrl)
        http.auth.basic username, password
        http.request(PUT, JSON) { req ->
            body = [
                name : pluginInfo.artifactId.text(),
                version : pluginInfo.version.text(),
                group : pluginInfo.groupId.text(),
                url : url
            ]
            
            response.success = { resp ->
                println "Notification successful"
            }

            response.failure = { resp, reader ->
                println reader.text
            }
        }
    }
}

target(processDefinitions: "Reads the repository definition configuration.") {
    distributionInfo = new DistributionManagementInfo()
    if (grailsSettings.config.grails.project.dependency.distribution instanceof Closure) {
        def callable = grailsSettings.config.grails.project.dependency.distribution
        callable.delegate = distributionInfo
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        try {
            callable.call()				
        }
        catch (e) {
            println "Error reading dependency distribution settings: ${e.message}"
            exit 1
        }
    }
}

class DistributionManagementInfo {
    Map portals = [:]
    Map remoteRepos = [:]
    String local

    void localRepository(String s) { local = s }

    void remoteRepository(Map args, Closure c = null) {
        if (!args?.id) throw new Exception("Remote repository misconfigured: Please specify a repository 'id'. Eg. remoteRepository(id:'myRepo')")
        if (!args?.url) throw new Exception("Remote repository misconfigured: Please specify a repository 'url'. Eg. remoteRepository(url:'http://..')")
        remoteRepos[args.id] = new Expando(args: args, configurer: c)
    }

    void portal(Map args) {
        portals[args.id] = args.url
    }
}
