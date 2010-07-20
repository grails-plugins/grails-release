package grails.plugins.publish.svn

import grails.plugins.publish.PluginDeployer

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.plugins.publishing.DefaultPluginPublisher
import org.springframework.core.io.FileSystemResource
import org.tmatesoft.svn.core.SVNAuthenticationException

class SvnDeployer implements PluginDeployer {
    def svnClient
    def workDir
    def askUser
    def out
    def pluginListFile

    SvnDeployer(svnClient, workDir, pluginListFile, out, askUser) {
        this.svnClient = svnClient
        this.workDir = workDir
        this.pluginListFile = pluginListFile
        this.out = out
        this.askUser = askUser
    }

    /**
     * 
     */
    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile, boolean makeLatest = true) {
        // Extract information from the POM.
        def pom = new XmlSlurper().parseText(pomFile.text)
        def pluginName = pom.artifactId.text()
        def pluginVersion = pom.version.text()
        def basePath = "grails-${pluginName}"
        def trunk = "${basePath}/trunk"

        // Is the current directory a working copy for the Subversion
        // repository? If yes, we can use it to perform the commits.
        def useTempWc = false
        def wc = new File(".")
        if (!handleAuthentication { svnClient.isWorkingCopyForRepository(wc, trunk) }) {
            // The current directory isn't a working copy, so create
            // a temporary working directory for the Subversion
            // repository.
            useTempWc = true
            wc = new File(workDir, "publish-wc")
            cleanLocalWorkingCopy(wc)
        }

        // We want to commit the new version to the Subversion repository,
        // but to do that we must first ensure that the repository already
        // contains the plugin. If it doesn't, we need to add the path
        // before committing the files.
        handleAuthentication {
            if (useTempWc && !svnClient.pathExists(trunk)) {
                // Path does not exist, so create it now.
                out.println "Creating path '$trunk' in the repository"
                svnClient.createPath(trunk, "Adding '${pluginName}' plugin to the repository.")
            }
        }

        // Check out the trunk of the Subversion project to our temporary
        // working directory unless we're working with the current directory
        // as the working copy. In the latter case, we just do an update.
        if (useTempWc) {
            out.println "Checking out '$trunk' from the repository to a temporary location"
            handleAuthentication { svnClient.checkOut(wc, trunk) }
        }
        else {
            out.println "Updating your working copy"
            handleAuthentication { svnClient.update(wc) }
        }

        // Create SHA1 and MD5 checksums for the plugin package.
        def packageBytes = pluginPackage.readBytes()
        def sha1File = new File(wc, "${pluginPackage.name}.sha1")
        def md5File = new File(wc, "${pluginPackage.name}.md5")
        sha1File.text = DigestUtils.shaHex(packageBytes)
        md5File.text = DigestUtils.md5Hex(packageBytes)

        // Copy the plugin package, plugin descriptor, and POM files to
        // the working copy so that we can commit them.
        def destFiles = [ new File(wc, pluginPackage.name), new File(wc, pluginXmlFile.name), new File(wc, pomFile.name), sha1File, md5File ]
        copyIfNotSame(pluginPackage, destFiles[0])
        copyIfNotSame(pluginXmlFile, destFiles[1])
        copyIfNotSame(pomFile, destFiles[2])
        handleAuthentication { svnClient.addFilesToSvn(destFiles) }

        // Commit the changes.
        out.println "Committing the new version of the plugin and its metadata to the repository"
        handleAuthentication {
            svnClient.commit(wc, "Releasing version ${pluginVersion} of the '${pluginName}' plugin.")
        }

        // Tag the release.
        out.println "Tagging this version of the plugin"
        handleAuthentication {
            svnClient.tag(
                    "${basePath}/trunk",
                    "${basePath}/tags",
                    "RELEASE_${pluginVersion.replaceAll('\\.','_')}",
                    "Tagging the ${pluginVersion} release of the '${pluginName}' plugin.")
        }

        // Do we make this the latest release too?
        if (makeLatest) {
            out.println "Tagging this release as the latest"
            handleAuthentication {
                svnClient.tag(
                        "${basePath}/trunk",
                        "${basePath}/tags",
                        "LATEST_RELEASE",
                        "Making version ${pluginVersion} of the '${pluginName}' plugin the latest.")
            }
        }

        // Support for legacy Grails clients: update the master plugin list
        // in the Subversion repository.
        updatePluginList(pluginName, !makeLatest)
    }

    protected final cleanLocalWorkingCopy(localWorkingCopy) {
        if (localWorkingCopy.exists()) {
            localWorkingCopy.deleteDir()
        }
        localWorkingCopy.mkdirs()
    }

    protected final updatePluginList(pluginName, skipLatest) {
        pluginListFile.delete()

        // Get newest version of plugin list
        out.println "Generating new plugin list"
        handleAuthentication { svnClient.fetchFile(".plugin-meta/plugins-list.xml", pluginListFile) }

        def remoteRevision = handleAuthentication { svnClient.latestRevision }
        def publisher = new DefaultPluginPublisher(remoteRevision.toString(), svnClient.repoUrl.toString())
        def updatedList = publisher.publishRelease(pluginName, new FileSystemResource(pluginListFile), !skipLatest)
        pluginListFile.withWriter("UTF-8") { w ->
            publisher.writePluginList(updatedList, w)
        }

        // Prepare the temporary working copy directory for the plugin
        // master list.
        def wc = new File(workDir, "publish-wc")
        cleanLocalWorkingCopy(wc)

        def remotePath = ".plugin-meta"
        handleAuthentication {
            if (!svnClient.pathExists(remotePath)) {
                // Path does not exist, so create it now.
                out.println "Creating path '$remotePath' in the repository"
                svnClient.createPath(remotePath, "Adding $remotePath to the repository.")
            }
        }

        out.println "Committing updated plugin list to the repostiory"

        // Check out the latest plugin list from the repository to a
        // temporary directory, then commit the modified plugin list
        // from there.
        def wcPluginList = new File(wc, "plugins-list.xml")
        handleAuthentication { svnClient.checkOut(wc, remotePath) }
        handleAuthentication { svnClient.addFilesToSvn([ wcPluginList ]) }
        copyIfNotSame(pluginListFile, wcPluginList)
        handleAuthentication { svnClient.commit(wc, "Updating plugin list for plugin '$pluginName'.") }
    }

    protected copyIfNotSame(srcFile, destFile) {
        if (srcFile.canonicalFile != destFile.canonicalFile) {
            FileUtils.copyFile(srcFile, destFile)
        }
    }

    /**
     * Executes a closure that may throw an SVNAuthenticationException.
     * If that exception is thrown, this method asks the user for his
     * username and password, updates the Subversion credentials and
     * tries to execute the closure again. Any exception thrown at that
     * point will propagate out.
     * @param askUser A closure taking a string argument that requests
     * input from the user and returns the response (the entered text
     * in other words).
     * @param c The closure to execute within the try/catch.
     */
    private handleAuthentication(c) {
        try {
            return c()
        }
        catch (SVNAuthenticationException ex) {
            def username = askUser("Enter your Subversion username:")
            def password = askUser("Enter your Subversion password:")
            svnClient.setCredentials(username, password)
            return c()
        }
    }
}
