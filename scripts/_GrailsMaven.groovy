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

artifact = groovy.xml.NamespaceBuilder.newInstance(ant, 'antlib:org.apache.maven.artifact.ant')

target(init: "Initialisation for maven deploy/install") {
	depends(packageApp)

	plugin = pluginManager?.allPlugins?.find { it.basePlugin }
	pomFileLocation = "${grailsSettings.projectTargetDir}/pom.xml"
	basePom = new File( "${basedir}/pom.xml" )
	if(basePom.exists())
		pomFileLocation = basePom.absolutePath



	if(!plugin) {
		includeTargets << grailsScript("_GrailsWar")	
		war()
	}
	else {
		includeTargets << grailsScript("_GrailsPluginDev")	
		packagePlugin()
		plugin = pluginManager?.allPlugins?.find { it.basePlugin }
		pluginInstance = plugin.pluginClass.newInstance()	
	}

	if(!basePom.exists()) {
		new File(pomFileLocation).withWriter { w ->
			xml = new groovy.xml.MarkupBuilder(w)

			xml.project {
				modelVersion "4.0.0"
				if(plugin) {
					def group = "org.grails.plugins"
					if(pluginInstance.hasProperty('group') && pluginInstance.group) {
						group = pluginInstance.group
					}
					else if(pluginInstance.hasProperty('groupId') && pluginInstance.groupId) {
						group = pluginInstance.groupId
					}

					groupId group
					artifactId plugin.fileSystemShortName 
					packaging "zip"
					version plugin.version
					name plugin.fileSystemShortName					
				}
				else {
					groupId buildConfig.grails.project.groupId ?: (config?.grails?.project?.groupId ?: grailsAppName)
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
									
									depVersion = "[$upper,$lower]"
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
