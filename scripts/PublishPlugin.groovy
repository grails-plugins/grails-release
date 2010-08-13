import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.groovy.grails.cli.CommandLineHelper

includeTargets << grailsScript("_GrailsPluginDev")
includeTargets << new File(mavenPublisherPluginDir, "scripts/_GrailsMaven.groovy")

target(default: "Publishes a plugin to either a Subversion or Maven repository.") {
    depends(parseArguments, processDefinitions, packagePlugin, generatePom)

    // Use the Grails Central Plugin repository as the default.
    def repoName = argsMap["repository"]
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
    
    // 'plugin' comes from the packagePlugin target.
    def isRelease = !plugin.version.endsWith("-SNAPSHOT")
    if (argsMap["snapshot"]) isRelease = false
    
    deployer.deployPlugin(new File(pluginZip), new File("plugin.xml"), new File(pomFileLocation), isRelease)
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
    Map remoteRepos = [:]
    String local

    void localRepository(String s) { local = s }

    void remoteRepository(Map args, Closure c = null) {
        if (!args?.id) throw new Exception("Remote repository misconfigured: Please specify a repository 'id'. Eg. remoteRepository(id:'myRepo')")
        if (!args?.url) throw new Exception("Remote repository misconfigured: Please specify a repository 'url'. Eg. remoteRepository(url:'http://..')")
        remoteRepos[args.id] = new Expando(args: args, configurer: c)
    }
}
