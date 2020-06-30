import org.codehaus.groovy.grails.cli.CommandLineHelper

includeTargets << grailsScript("_GrailsPluginDev")
includeTargets << new File(releasePluginDir, "scripts/_GrailsMaven.groovy")

USAGE = """
    publish-plugin [--repository=REPO] [--protocol=PROTOCOL] [--portal=PORTAL] [--dry-run] [--snapshot] [--ping-only]

where
    REPO     = The name of a configured maven compatible repository to deploy the plugin to.
               (default: Grails Central Plugin Repository).

    PROTOCOL = The protocol to use when deploying to a Maven-compatible
               repository. Can be one of 'http', 'scp', 'scpexe', 'ftp', or
               'webdav'. (default: 'http').

    PORTAL   = The portal to inform of the plugin's release.
               (default: Grails Plugin Portal).

    --dry-run      = Shows you what will happen when you publish the plugin,
                     but doesn't actually publish it.

    --snapshot     = Force this release to be a snapshot version, i.e. it isn't
                     automatically made the latest available release.

    --no-overwrite = Don't fail if this plugin has already been published.
                     This is useful if this plugin is being published from a
                     continuous integration server and you don't want the
                     command to exit with failure.

    --allow-overwrite = Allow any existing plugin to be overwritten.

    --ping-only    = Don't publish/deploy the plugin, only send a notification
                     to the plugin portal. This is useful if portal
                     notification failed during a previous attempt to publish
                     the plugin. Mutually exclusive with the --dry-run option.

    --binary       = Release as a binary plugin.
"""

target(publishPlugin: "Publishes a plugin to a Maven repository.") {
    depends(parseArguments, checkGrailsVersion, packagePlugin, processDefinitions, generatePom)

    // Handle old names for options. Trying to be consistent with Grails 2.0 conventions.
    if (argsMap["dryRun"]) { argsMap["dry-run"] = true }
    if (argsMap["pingOnly"]) { argsMap["ping-only"] = true }
    if (argsMap["noOverwrite"]) { argsMap["no-overwrite"] = true }
    if (argsMap["allowOverwrite"]) { argsMap["allow-overwrite"] = true }
    if (argsMap["promptAuth"]) { argsMap["prompt-auth"] = true }

    // Read the plugin information from the POM.
    pluginInfo = new XmlSlurper().parse(new File(pomFileLocation))
    isRelease = !pluginInfo.version.text().endsWith("-SNAPSHOT")
    if (argsMap["snapshot"]) isRelease = false

    pluginInfo = [
            name : pluginInfo.artifactId.text(),
            group : pluginInfo.groupId.text(),
            version : pluginInfo.version.text(),
            isSnapshot : !isRelease ]
    event "PublishPluginStart", [ pluginInfo ]

    // Use the Grails Central Plugin repository as the default.
    def repoClass = classLoader.loadClass("grails.plugins.publish.Repository")
    def repo = repoClass.grailsCentral

    // Add the Grails Central portal to the distribution info under the
    // ID 'grailsCentral'. 'distributionInfo' is created by the processDefinitions
    // target and contains the configuration for all the declared repositories
    // and portals.
    def grailsCentralPortal = distributionInfo.portals["grailsCentral"]
    if (!grailsCentralPortal) {
        grailsCentralPortal = [ url: repoClass.GRAILS_CENTRAL_PORTAL_URL ]
        distributionInfo.portals["grailsCentral"] = grailsCentralPortal
    }
    else {
        if (grailsCentralPortal["url"]) {
            println "WARN: You cannot change the URL for the 'grailsCentral' portal - ignoring the user-specified value"
        }
        grailsCentralPortal["url"] = repoClass.GRAILS_CENTRAL_PORTAL_URL
    }

    // Is a default repository configured for this project? If yes, use that
    // unless a '--repository' command line option is specified.
    def repoName = argsMap["repository"] ?: grailsSettings.config.grails.project.repos.default
    def type = "grailsCentral"

    if (repoName && repoName != "grailsCentral") {
        // First look for the repository definition for this name.
        def repoDefn = distributionInfo.remoteRepos[repoName]
        def defaultPortal
        def url
        if (repoDefn) {
            type = repoDefn.args["type"] ?: "maven"
            url = repoDefn.args["url"]

            // If the repository defines a portal, then we should use that
            // ahead of the public Grails plugin portal.
            defaultPortal = repoDefn.args["portal"]
        }

        // Check that the repository is defined.
        if (url) {
            repo = repoClass.newInstance(
                    repoName,
                    new URI(url),
                    defaultPortal ?: null)
            def repoNames = [maven: "Maven", grailsCentral: "Grails"]
            println "Publishing to ${repoNames[type]} repository '$repoName'"
        }
        else {
            println "No configuration found for repository '$repoName'"
            exit 1
        }
    }
    else {
        println "Publishing to Grails Central"
    }

    def deployer
    if (argsMap["dry-run"]) {
        def retval = processAuthConfig.call(repo.name) { username, password ->
            if (username) {
                println "Using configured username and password from grails.project.repos.${repo.name}"
            }
        }

        if (retval) return retval

        deployer = classLoader.loadClass("grails.plugins.publish.print.DryRunDeployer").newInstance()
    }
    else if (type == "grailsCentral") {
        deployer = classLoader.loadClass("grails.plugins.publish.portal.GrailsCentralDeployer").newInstance() { msg ->
            // This closure is executed whenever the deployer needs to
            // ask for user input.
            return userInput(
                    new CommandLineHelper(),
                    msg,
                    "GrailsCentralDeployer requires an answer to \"${msg}\", but you are running in non-interactive mode.")
        }

        def retval = processAuthConfig.call(repo.name) { username, password ->
            if (username) {
                deployer.username = username
                deployer.password = password

                def gcp = distributionInfo.portals["grailsCentral"]
                if (gcp && !gcp?.username) {
                    gcp.username = username
                    gcp.password = password

                }
            }
        }

        if (retval) return retval
        def uri = repo?.uri?.toString()
        if (uri) {
            deployer.portalUrl = uri
        }
    }
    else if (type == "maven") {
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

        // Add a configurer to the repository definition if the username and password
        // have been defined.
        def projectConfig = grailsSettings.config.grails.project
        def retval = processAuthConfig.call(repo.name) { username, password ->
            if (username) {
                if (projectConfig.repos."${repo.name}".custom) {
                    println "WARN: username and password defined in config as well as a 'custom' entry - ignoring the provided username and password."
                }
                else {
                    println "Using configured username and password from grails.project.repos.${repo.name}"
                    repoDefn.configurer = { authentication username: username, password: password }
                    repoDefn.args.remove "username"
                    repoDefn.args.remove "password"
                }
            }
        }

        if (retval) return retval

        if (argsMap["protocol"]) {
            protocol = protocols[argsMap["protocol"]]
        }
        else if (repo.uri) {
            if (!repo.uri.scheme) {
                println "Invalid URL for repository '${repo.name}': ${repo.uri}"
                exit 1
                return 1
            }

            if (protocols[repo.uri.scheme]) {
                protocol = protocols[repo.uri.scheme]
            }
            else {
                println "WARNING: unknown protocol '${repo.uri.scheme}' for repository '${repo.name}'"
            }
        }

        if (argsMap["prompt-auth"]) {
            def inputHelper = new CommandLineHelper()
            username = userInput(
                    inputHelper,
                    "Username for repository: ",
                    "You haven't configured the plugin repository username - required in non-interactive mode")
            password = userInput(
                    inputHelper,
                    "Password for repository: ",
                    "You haven't configured the plugin repository password - required in non-interactive mode")

            repoDefn.configurer = { authentication username: username, password: password }
        }

        deployer = classLoader.loadClass("grails.plugins.publish.maven.MavenDeployer").newInstance(ant, repoDefn, protocol)
    }
    else {
        println "Unknown type '$type' defined for repository '$repoName'"
        exit 1
    }

    if (!argsMap["ping-only"]) {
        event "DeployPluginStart", [ pluginInfo, pluginZip, pomFileLocation ]

        def pomFile = pomFileLocation as File
        if (deployer.isVersionAlreadyPublished(pomFile)) {
            if (argsMap["no-overwrite"]) {
                println "This version of the plugin has already been published."
                event "StatusFinal", ["Plugin publication cancelled with clean exit."]
                exit(0)
            } else {
                if (argsMap["allow-overwrite"]) {
                    println "This version of the plugin has already been published, it will be overwritten."
                } else if (isInteractive) {
                    def inputHelper = new CommandLineHelper()
                    def answer = userInput(
                            inputHelper,
                            "This version has already been published. Do you want to replace it " +
                                "(not recommended except for snapshots)? (y,N) ",
                            "This version of the plugin has already been published.")
                    if (!answer?.equalsIgnoreCase("y")) {
                        event "StatusFinal", ["Plugin publication cancelled."]
                        exit(1)
                    }
                } else {
                    println "This version of the plugin has already been published"
                    println "Use the --allow-overwrite option to publish the plugin."
                    event "StatusFinal", ["Plugin publication cancelled."]
                    exit(1)
                }
            }
        }

        try {
            deployer.deployPlugin(pluginZip as File, new File(basedir, "plugin.xml"), new File(pomFileLocation), isRelease)
        }
        catch (Exception ex) {
            event "StatusError", ["Failed to publish plugin: ${ex.message}"]
            exit 1
        }

        event "DeployPluginEnd", [ pluginInfo, pluginZip, pomFileLocation ]
    }

    // What's the URL of the portal to ping? The explicit 'portal' argument
    // takes precedence, then the portal configured for the current repository,
    // and finally the public Grails plugin portal.
    def portalName = argsMap["portal"] ?: repo.defaultPortal
    def portalDefn = null
    if (portalName) {
        // Pick the configured portal with the given name, assuming one
        // exists with that name.
        portalDefn = distributionInfo.portals[portalName]

        if (!portalDefn?.url) {
            println "No portal defined with ID '${portalName}'"
            println "Plugin has been published, but the plugin portal has not been notified."
            exit 1
        }
    }
    else {
        // We don't ping the grails.org portal if a repository has been specified
        // but that repository has no default portal configured.
        println "No default portal defined for repository '${repoName}' - skipping portal notification"
        event "PublishPluginEnd", [ pluginInfo ]
        return
    }

    // Add the plugin name to the URL, making sure first that the base portal URI
    // ends with '/'. Otherwise the resolve won't do what we want.
    def portalUrl = new URI(portalDefn.url)
    if (!portalUrl.path.endsWith("/")) portalUrl = new URI(portalUrl.toString() + "/")
    portalUrl = portalUrl.resolve(pluginInfo.name)

    // Now that we have a URL, simply send a PUT request with the appropriate
    // JSON content.
    println "Notifying plugin portal '${portalUrl}' of release..."

    if (!argsMap["dry-run"]) {
        event "PingPortalStart", [ pluginInfo, portalUrl, repo.uri.toString() ]

        def username = portalDefn.username
        def password = portalDefn.password

        if (!username) {
            def inputHelper = new CommandLineHelper()
            username = userInput(
                    inputHelper,
                    "Username for portal (leave empty if authentication not required): ",
                    "You haven't configured the plugin portal username - required in non-interactive mode")
            password = userInput(
                    inputHelper,
                    "Password for portal (leave empty if authentication not required): ",
                    "You haven't configured the plugin portal password - required in non-interactive mode")
        }

        def converterConfig = new org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer()
        converterConfig.initialize(grailsApp)
        def rest = classLoader.loadClass("grails.plugins.rest.client.RestBuilder").newInstance()
        def springGsonMessageConverter = rest.restTemplate.messageConverters.find {
            it.class.name == 'org.springframework.http.converter.json.GsonHttpMessageConverter'
        }
        if (springGsonMessageConverter) {
            rest.restTemplate.messageConverters.remove springGsonMessageConverter
        }
        def jsonParams = pluginInfo + [ url : repo.uri.toString() ]
        def resp = rest.put(portalUrl.toString()) {
            auth username, password
            json({ jsonParams })
        }
        if (grailsConsole.verbose) {
            grailsConsole.log("Updating plugin with JSON: ${portalUrl}")
            grailsConsole.log(jsonParams.toString())
        }
        switch(resp.status) {
            case 400:
                println "ERROR: Plugin update failed: ${resp.json?.message}"; break
            case 401:
                println "ERROR: Portal authentication failed. Are your username and password correct?"; break
            case 403:
                println "ERROR: You do not have permission to update the plugin portal."; break

            default:
                if (resp.class.name == "grails.plugins.rest.client.ErrorResponse") {
                    println "ERROR: Notification failed - status ${resp.status} - ${resp.json.message}"
                }
                else {
                    println "Notification successful"
                }
        }

        event "PingPortalEnd", [ pluginInfo, portalUrl, repo.uri.toString() ]
    }

    event "PublishPluginEnd", [ pluginInfo ]
}

private userInput(inputHelper, msg, nonInteractiveErrorMsg) {
    if (!isInteractive) {
        event "StatusError", [nonInteractiveErrorMsg]
        event "StatusFinal", ["Plugin publication cancelled."]
        exit 1
        return ""
    }
    else {
        return requiresSecureInput(msg) ? secureUserInput(inputHelper, msg) : inputHelper.userInput(msg)
    }
}

private secureUserInput(inputHelper, msg) {
    if (binding.variables.containsKey("grailsConsole")) {
        try {
            if (grailsConsole.metaClass.respondsTo(grailsConsole, "secureUserInput", msg)) {
                return grailsConsole.secureUserInput(msg)
            }
            else {
                return grailsConsole.reader.readLine(msg, new Character("*" as char))
            }
        }
        catch (ClassNotFoundException e) {
            return inputHelper.userInput(msg)
        }
    }
    else {
        return inputHelper.userInput(msg)
    }
}

private requiresSecureInput(msg) { msg.toLowerCase().contains("password") }

setDefaultTarget(publishPlugin)
