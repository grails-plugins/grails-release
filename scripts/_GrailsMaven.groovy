/*
 * Copyright 2004-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import grails.util.*
import org.codehaus.groovy.grails.plugins.*
import org.apache.ivy.util.ChecksumHelper

scriptScope = grails.util.BuildScope.WAR
scriptEnv = "production"

includeTargets << grailsScript("_GrailsPackage")

// Open source licences.
globalLicenses = [
        APACHE: [ name: "Apache License 2.0", url: "http://www.apache.org/licenses/LICENSE-2.0.txt" ],
        GPL2: [ name: "GNU General Public License 2", url: "http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt"],
        GPL3: [ name: "GNU General Public License 3", url: "http://www.gnu.org/licenses/gpl.txt"] ]

artifact = groovy.xml.NamespaceBuilder.newInstance(ant, 'antlib:org.apache.maven.artifact.ant')

target(mavenInstall:"Installs a plugin or application into your local Maven cache") {
    depends(init)
    def deployFile = isPlugin ? new File(pluginZip) : grailsSettings.projectWarFile
    def ext = isPlugin ? deployFile.name[-3..-1] : "war"
    installOrDeploy(deployFile, ext, false, [local:distributionInfo.localRepo])
}

target(mavenDeploy:"Deploys the plugin to a Maven repository") {
    depends(init)
    def protocols = [     http: "wagon-http",
                        scp:    "wagon-ssh",
                        scpexe:    "wagon-ssh-external",
                        ftp: "wagon-ftp",
                        webdav: "wagon-webdav" ]
    
    def protocol = protocols.http
    def repoName = argsMap.repository
    def repo = repoName ? distributionInfo.remoteRepos[repoName] : null
    if(argsMap.protocol) {
        protocol = protocols[argsMap.protocol]
    }
    else if(repo) {
        def url = repo?.args?.url
        if(url) {
            def i = url.indexOf('://')
            def urlProt = url[0..i-1]
            protocol = protocols[urlProt] ?: protocol
        }
    }

    def retval = processAuthConfig.call(repoName) { username, password ->
        if (username) {
            def projectConfig = grailsSettings.config.grails.project
            if (projectConfig.repos."${repoName}".custom) {
                println "WARN: username and password defined in config as well as a 'custom' entry - ignoring the provided username and password."
            }
            else {
                println "Using configured username and password from grails.project.repos.${repoName}"
                repo.configurer = { authentication username: username, password: password }
                repo.args.remove "username"
                repo.args.remove "password"
            }
        }
    }

    if (retval) return retval
    
    artifact.'install-provider'(artifactId:protocol, version:"1.0-beta-2")
    
    
    def deployFile = isPlugin ? new File(pluginZip) : grailsSettings.projectWarFile
    def ext = isPlugin ? deployFile.name[-3..-1] : "war"
    try {
        installOrDeploy(deployFile, ext, true, [remote:repo, local:distributionInfo.localRepo])
    }
    catch(e) {
        println "Error deploying artifact: ${e.message}"
        println "Have you specified a configured repository to deploy to (--repository argument) or specified distributionManagement in your POM?"
    }
}

target(init: "Initialisation for maven deploy/install") {
    depends(packageApp, processDefinitions)

    isPlugin = pluginManager?.allPlugins?.any { it.basePlugin }

    if (!isPlugin) {
        includeTargets << grailsScript("_GrailsWar")
        war()
    }

    generatePom()
}

target(processDefinitions: "Reads the repository definition configuration.") {
    def projectConfig = grailsSettings.config.grails.project
    distributionInfo = classLoader.loadClass("grails.plugins.publish.DistributionManagementInfo").newInstance()

    if (projectConfig.dependency.distribution instanceof Closure) {
        // Deal with the DSL form of configuration, which is the old approach.
        def callable = grailsSettings.config.grails.project.dependency.distribution?.clone()
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
    else if (projectConfig.repos || projectConfig.portal) {
        // Handle standard configuration.
        for (entry in projectConfig.portal) {
            // Add this portal to the distribution info. The key is the portal ID
            // while the value is a map of options that must include 'url'.
            distributionInfo.portals[entry.key] = entry.value
        }

        for (entry in projectConfig.repos) {
            // Add this repository to the distribution info. The key is the repository
            // ID while the value is a map containing the repository configuration.
            def props = entry.value + [id: entry.key]
            def c = props.remove("custom")
            distributionInfo.remoteRepos[entry.key] = new Expando(args: props, configurer: c)
        }

        distributionInfo.localRepo = projectConfig.mavenCache ?: null
    }
}

target(generatePom: "Generates a pom.xml file for the current project unless './pom.xml' exists.") {
    depends(packageApp)

    pomFileLocation = "${grailsSettings.projectTargetDir}/pom.xml"
    basePom = new File("${basedir}/pom.xml")

    if (basePom.exists()) {
        pomFileLocation = basePom.absolutePath
        println "Skipping POM generation because 'pom.xml' exists in the root of the project."
        return 1
    }

    // Get hold of the plugin instance for this plugin if it's a plugin
    // project. If it isn't, then these variables will be null.
    def plugin = pluginManager?.allPlugins?.find { it.basePlugin }
    def pluginInstance = plugin?.pluginClass?.newInstance()

    if (plugin) {
        includeTargets << grailsScript("_GrailsPluginDev")
        packagePlugin()

        // This script variable doesn't exist pre-Grails 1.4.
        if (!binding.variables.containsKey("pluginInfo")) {
            pluginInfo = pluginSettings.getPluginInfo(basedir)
        }
    }

    new File(pomFileLocation).withWriter { w ->
        def xml = new groovy.xml.MarkupBuilder(w)

        xml.project(xmlns: "http://maven.apache.org/POM/4.0.0", 
                'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance", 
                'xsi:schemaLocation': "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd") {
            modelVersion "4.0.0"
            if (plugin) {
                def group = "org.grails.plugins"
                if (getOptionalProperty(pluginInstance, 'group')) {
                    group = pluginInstance.group
                }
                else if (getOptionalProperty(pluginInstance, 'groupId')) {
                    group = pluginInstance.groupId
                }

                groupId group
                artifactId plugin.fileSystemShortName 
                packaging pluginInfo.packaging == "binary" ? "jar" : "zip"
                version plugin.version

                // I think description() and url() resolve against the AntBuilder
                // by default, so we have to call them explicitly on the MarkupBuilder.
                if (getOptionalProperty(pluginInstance, "title")) name pluginInstance.title
                if (getOptionalProperty(pluginInstance, "description")) delegate.description pluginInstance.description
                if (getOptionalProperty(pluginInstance, "documentation")) delegate.url pluginInstance.documentation
                if (getOptionalProperty(pluginInstance, "license")) {
                    def l = globalLicenses[pluginInstance.license]
                    if (l) {
                        licenses {
                            license {
                                name l.name
                                delegate.url l.url
                            }
                        }
                    }
                    else {
                        event("StatusUpdate", [ "Unknown license: ${pluginInstance.license}" ])
                    }
                }
                if (getOptionalProperty(pluginInstance, "organization")) {
                    organization {
                        name pluginInstance.organization.name
                        delegate.url pluginInstance.organization.url
                    }
                }

                // Handle the developers
                def devs = []
                if (getOptionalProperty(pluginInstance, "author")) {
                    def author = [ name: pluginInstance.author ]
                    if (getOptionalProperty(pluginInstance, "authorEmail")) {
                        author["email"] = pluginInstance.authorEmail
                    }

                    devs << author
                }
                if (getOptionalProperty(pluginInstance, "developers")) {
                    devs += pluginInstance.developers
                }

                if (devs) {
                    developers {
                        for (d in devs) {
                            developer {
                                name d.name
                                if (d.email) email d.email
                            }
                        }
                    }
                }

                // Handle the issue tracker
                if (getOptionalProperty(pluginInstance, "issueManagement")) {
                    def trackerInfo = pluginInstance.issueManagement
                    issueManagement {
                        if (trackerInfo.system) system trackerInfo.system
                        if (trackerInfo.url) delegate.url trackerInfo.url
                    }
                }

                // Source control
                if (getOptionalProperty(pluginInstance, "scm")) {
                    def scmInfo = pluginInstance.scm
                    scm {
                        if (scmInfo.connection) connection scmInfo.connection
                        if (scmInfo.developerConnection) developerConnection scmInfo.developerConnection
                        if (scmInfo.tag) tag scmInfo.tag
                        if (scmInfo.url) delegate.url scmInfo.url
                    }
                }
            }
            else {
                groupId buildConfig.grails.project.groupId ?: (config?.grails?.project?.groupId ?: grailsAppName)
                artifactId grailsAppName
                packaging "war"
                version grailsAppVersion
                name grailsAppName
            }
                
                
            if(plugin) {
                dependencies {
                    corePlugins = pluginManager.allPlugins.findAll { it.pluginClass.name.startsWith("org.codehaus.groovy.grails.plugins") }*.name
                    if(pluginInstance != null && pluginInstance.hasProperty('dependsOn')) {
                        for(dep in pluginInstance.dependsOn) {
                            String depName = dep.key
                            if(!corePlugins.contains(dep.key)) {
                                // Note: specifying group in dependsOn is a Grails 1.3 feature
                                // 1.2 users don't have this capability
                                def depGroup = "org.grails.plugins"
                                if(depName.contains(":")) {
                                    def i = depName.split(":")
                                    depGroup = i[0]
                                    depName = i[1]
                                }
                                String depVersion = dep.value
                                def upper = GrailsPluginUtils.getUpperVersion(depVersion)
                                def lower = GrailsPluginUtils.getLowerVersion(depVersion)
                                if(upper == lower) depVersion = upper
                                else {
                                    upper = upper == '*' ? '' : upper
                                    lower = lower == '*' ? '' : lower

                                    depVersion = "[$lower,$upper]"
                                }

                                dependency {
                                    groupId depGroup
                                    artifactId GrailsNameUtils.getScriptName(depName)
                                    version depVersion
                                    type "zip"
                                }
                            }
                        }        
                    }
                    def dependencyManager = grailsSettings.dependencyManager
                    def appDeps = dependencyManager.getApplicationDependencyDescriptors()
                    def allowedScopes = ['runtime','compile']
                    for(dep in appDeps) {
                        if(allowedScopes.contains(dep.scope)  && dep.exported) {
                            def moduleId = dep.getDependencyRevisionId()
                            dependency {
                                groupId moduleId.organisation
                                artifactId moduleId.name
                                version moduleId.revision
                                scope dep.scope
                            }                            
                        }
                    }
                    
                    def pluginDeps = dependencyManager.getPluginDependencyDescriptors()
                    def pluginsInstalledViaInstallPlugin = grails.util.Metadata.current.getInstalledPlugins()
                    for(dep in pluginDeps) {
                        def moduleId = dep.getDependencyRevisionId()                        
                        if(allowedScopes.contains(dep.scope) && dep.exported && !pluginsInstalledViaInstallPlugin.containsKey(moduleId.name) ) {                            

                            dependency {
                                groupId moduleId.organisation
                                artifactId moduleId.name
                                version moduleId.revision
                                type "zip"
                                scope dep.scope
                            }                        
                        }
                    }
                }
            }
        }
    }
    println "POM generated: ${pomFileLocation}"
}

processAuthConfig = { repoName, c ->
    // Get credentials for authentication if defined in the config.
    def projectConfig = grailsSettings.config.grails.project
    def username = projectConfig.repos."${repoName}".username
    def password = projectConfig.repos."${repoName}".password

    // Check whether only one of the authentication parameters has been set. If
    // so, exit with an error.
    if (!username ^ !password) {
        println "grails.project.repos.${repoName}.username and .password must both be defined or neither."
        return 1
    }

    c(username, password)
    return 0
}

private installOrDeploy(File file, ext, boolean deploy, repos = [:]) {
    if (!deploy) {
            ant.checksum file:pomFileLocation, algorithm:"sha1", todir:projectTargetDir
            ant.checksum file:file, algorithm:"sha1", todir:projectTargetDir
    }

    def pomCheck = generateChecksum(new File(pomFileLocation))
    def fileCheck = generateChecksum(file)

    artifact."${ deploy ? 'deploy' : 'install' }"(file: file) {
        if (isPlugin) {
            attach file:"${basedir}/plugin.xml",type:"xml", classifier:"plugin"
        }

        if (!deploy) {
            attach file:"${projectTargetDir}/pom.xml.sha1",type:"pom.sha1"
            attach file:"${projectTargetDir}/${file.name}.sha1",type:"${ext}.sha1"
        }

        pom(file: pomFileLocation)
        if(repos.remote) {
            def repo = repos.remote
            if(repo.configurer) {
                remoteRepository(repo.args, repo.configurer)
            }
            else {
                remoteRepository(repo.args)
            }
        }
        if(repos.local) {
            localRepository(path:repos.local)
        }

    }    
}

private generateChecksum(File file) {
    def checksum = new File("${file.parentFile.absolutePath}/${file.name}.sha1")
    checksum.write ChecksumHelper.computeAsString(file, "sha1")
    return checksum
}


private getOptionalProperty(obj, prop) {
    return obj.hasProperty(prop) ? obj."$prop" : null
}
