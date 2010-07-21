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


includeTargets << grailsScript("_GrailsPackage")	

// Open source licences.
globalLicenses = Collections.unmodifiableMap([
		APACHE: [ name: "Apache License 2.0", url: "http://www.apache.org/licenses/LICENSE-2.0.txt" ],
		GPL2: [ name: "GNU General Public License 2", url: "http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt"],
		GPL3: [ name: "GNU General Public License 3", url: "http://www.gnu.org/licenses/gpl.txt"] ])

artifact = groovy.xml.NamespaceBuilder.newInstance(ant, 'antlib:org.apache.maven.artifact.ant')

target(init: "Initialisation for maven deploy/install") {
	depends(packageApp)

	plugin = pluginManager?.allPlugins?.find { it.basePlugin }

	if(!plugin) {
		includeTargets << grailsScript("_GrailsWar")	
		war()
	}
	else {
		includeTargets << grailsScript("_GrailsPluginDev")	
		packagePlugin()
		plugin = pluginManager?.allPlugins?.find { it.basePlugin }
	}

	generatePom()
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

	def plugin = pluginManager?.allPlugins?.find { it.basePlugin }
	def pluginInstance = plugin.pluginClass.newInstance()

	new File(pomFileLocation).withWriter { w ->
		def xml = new groovy.xml.MarkupBuilder(w)

		xml.project(xmlns: "http://maven.apache.org/POM/4.0.0", 
				'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance", 
				'xsi:schemaLocation': "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd") {
			modelVersion "4.0.0"
			if(plugin) {
				def group = "org.grails.plugins"
				if (getOptionalProperty(pluginInstance, 'group')) {
					group = pluginInstance.group
				}
				else if(getOptionalProperty(pluginInstance, 'groupId')) {
					group = pluginInstance.groupId
				}

				groupId group
				artifactId plugin.fileSystemShortName 
				packaging "zip"
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
			}
			else {
				groupId buildConfig.grails.project.groupId ?: (buildConfig?.grails?.project?.groupId ?: grailsAppName)
				artifactId grailsAppName
				packaging "war"
				version grailsAppVersion
				name grailsAppName				
			}
				
				
			if(plugin && plugin.dependencyNames) {
				dependencies {					
					corePlugins = pluginManager.allPlugins.findAll { it.pluginClass.name.startsWith("org.codehaus.groovy.grails.plugins") }*.name	
					
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
								artifactId depName
								version depVersion
							}							
						}
					}					
				}
			}
		}
	}
}


target(mavenInstall:"Installs a plugin or application into your local Maven cache") {
	depends(init)
	def deployFile = plugin ? new File(pluginZip) : grailsSettings.projectWarFile
	def ext = plugin ? "zip" : "war"
	installOrDeploy(deployFile, ext, false)
}

private generateChecksum(File file) {
	def checksum = new File("${file.parentFile.absolutePath}/${file.name}.sha1")
	checksum.write ChecksumHelper.computeAsString(file, "sha1")	
	return checksum
}
private installOrDeploy(File file, ext, boolean deploy, repos = [:]) {
	if (!deploy) {
	        ant.checksum file:pomFileLocation, algorithm:"sha1", todir:projectTargetDir
	        ant.checksum file:file, algorithm:"sha1", todir:projectTargetDir		
	}

	def pomCheck = generateChecksum(new File(pomFileLocation))
	def fileCheck = generateChecksum(file)

    artifact."${ deploy ? 'deploy' : 'install' }"(file: file) {
		if(ext == 'zip') {
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


private getOptionalProperty(obj, prop) {
	return obj.hasProperty(prop) ? obj."$prop" : null
}

target(mavenDeploy:"Deploys the plugin to a Maven repository") {
	depends(init)
	def protocols = [ 	http: "wagon-http",
						scp:	"wagon-ssh",
						scpexe:	"wagon-ssh-external",
						ftp: "wagon-ftp",
						webdav: "wagon-webdav" ]
	
	def distInfo = new DistributionManagementInfo()
	if(grailsSettings.config.grails.project.dependency.distribution instanceof Closure) {
		def callable = grailsSettings.config.grails.project.dependency.distribution
		callable.delegate = distInfo
		callable.resolveStrategy = Closure.DELEGATE_FIRST
		try {
			callable.call()				
		}
		catch(e) {
			println "Error reading dependency distribution settings: ${e.message}"
			exit 1
		}

	}
	def protocol = protocols.http
	def repo = argsMap.repository ? distInfo.remoteRepos[argsMap.repository] : null		
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
	
	artifact.'install-provider'(artifactId:protocol, version:"1.0-beta-2")
	
	
	def deployFile = plugin ? new File(pluginZip) : grailsSettings.projectWarFile
	def ext = plugin ? "zip" : "war"	
	try {
		installOrDeploy(deployFile, ext, true, [remote:repo, local:distInfo.local])
	}
	catch(e) {
		println "Error deploying artifact: ${e.message}"
		println "Have you specified a configured repository to deploy to (--repository argument) or specified distributionManagement in your POM?"
	}
}

class DistributionManagementInfo {
	Map remoteRepos = [:]
	String local
	void localRepository(String s) { local = s }
	void remoteRepository(Map args, Closure c = null) {
		if(!args?.id) throw new Exception("Remote repository misconfigured: Please specify a repository 'id'. Eg. remoteRepository(id:'myRepo')")
		if(!args?.url) throw new Exception("Remote repository misconfigured: Please specify a repository 'url'. Eg. remoteRepository(url:'http://..')")		
		def e = new Expando()
		e.args = args
		e.configurer = c
		remoteRepos[args.id] = e
	}
}
