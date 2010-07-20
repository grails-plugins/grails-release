package grails.plugins.publish.svn

import org.apache.commons.codec.digest.DigestUtils
import org.gmock.WithGMock
import org.tmatesoft.svn.core.SVNAuthenticationException
import org.tmatesoft.svn.core.SVNErrorCode
import org.tmatesoft.svn.core.SVNErrorMessage

@WithGMock
class SvnDeployerUnitTests extends GroovyTestCase {
    def baseDir
    def pluginListFile

    void setUp() {
        baseDir = new File("target/tmp")
        baseDir.mkdirs()
        pluginListFile = new File(baseDir, "plugin-list.xml")
    }

    void tearDown() {
        baseDir.deleteDir()
    }

    void testDeployPlugin() {
        def zipContent = "Hello world"
        def zipFile = new File(baseDir, "grails-pdf-generator-1.1.2.zip")
        zipFile.text = zipContent

        def expectedMd5Sum = DigestUtils.md5Hex(zipContent.bytes)
        def expectedSha1Sum = DigestUtils.shaHex(zipContent.bytes)

        def pluginXmlFile = new File(baseDir, "plugin.xml")
        pluginXmlFile.text = "<plugin></plugin>"

        def pomFile = new File(baseDir, "pom.xml")
        pomFile.text = """\
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.example</groupId>
  <artifactId>pdf-generator</artifactId>
  <version>1.1.2</version>
  <packaging>zip</packaging>
</project>
"""

        // These are the files that should be added to the Subversion
        // repository if they're not already there.
        def wcDir = new File(".")
        def expectedFiles = [
                new File(wcDir, zipFile.name),
                new File(wcDir, "plugin.xml"),
                new File(wcDir, "pom.xml"),
                new File(wcDir, "${zipFile.name}.sha1"),
                new File(wcDir, "${zipFile.name}.md5") ]

        // The current directory is a working copy for the repository.
        def mockSvnClient = mock()
        mockSvnClient.isWorkingCopyForRepository(wcDir, "grails-pdf-generator/trunk").returns(true)
        
        // No need to check out the trunk since the current directory
        // already a checkout of trunk. Need to update the working copy
        // though.
        mockSvnClient.update(wcDir)
        mockSvnClient.addFilesToSvn(expectedFiles)
        mockSvnClient.commit(wcDir, "Releasing version 1.1.2 of the 'pdf-generator' plugin.")
        mockSvnClient.tag(
                "grails-pdf-generator/trunk",
                "grails-pdf-generator/tags",
                "RELEASE_1_1_2",
                "Tagging the 1.1.2 release of the 'pdf-generator' plugin.")
        mockSvnClient.tag(
                "grails-pdf-generator/trunk",
                "grails-pdf-generator/tags",
                "LATEST_RELEASE",
                "Making version 1.1.2 of the 'pdf-generator' plugin the latest.")

        mockUpdatePluginList(mockSvnClient, false)

        play {
            def deployer = new SvnDeployer(mockSvnClient, baseDir, pluginListFile, System.out, null)
            deployer.deployPlugin(zipFile, pluginXmlFile, pomFile)

            // Check that the checksums are as expected.
            assertEquals expectedSha1Sum, expectedFiles[3].text
            assertEquals expectedMd5Sum, expectedFiles[4].text
        }
    }

    void testDeployPluginNotLatest() {
        def zipContent = "Hello world"
        def zipFile = new File(baseDir, "grails-pdf-generator-0.5-SNAPSHOT.zip")
        zipFile.text = zipContent

        def expectedMd5Sum = DigestUtils.md5Hex(zipContent.bytes)
        def expectedSha1Sum = DigestUtils.shaHex(zipContent.bytes)

        def pluginXmlFile = new File(baseDir, "plugin.xml")
        pluginXmlFile.text = "<plugin></plugin>"

        def pomFile = new File(baseDir, "pom.xml")
        pomFile.text = """\
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.grails.plugins</groupId>
  <artifactId>pdf-generator</artifactId>
  <version>0.5-SNAPSHOT</version>
  <packaging>zip</packaging>
</project>
"""

        def pluginListFile = new File(baseDir, "plugin-list.xml")

        // These are the files that should be added to the Subversion
        // repository if they're not already there.
        def wcDir = new File(baseDir, "publish-wc")
        def expectedFiles = [
                new File(wcDir, zipFile.name),
                new File(wcDir, "plugin.xml"),
                new File(wcDir, "pom.xml"),
                new File(wcDir, "${zipFile.name}.sha1"),
                new File(wcDir, "${zipFile.name}.md5") ]

        // The current directory is not a working copy for the repository.
        def mockSvnClient = mock()
        mockSvnClient.isWorkingCopyForRepository(new File("."), "grails-pdf-generator/trunk").returns(false)
        
        // The plugin is not already in the repository.
        mockSvnClient.pathExists("grails-pdf-generator/trunk").returns(false)
        mockSvnClient.createPath("grails-pdf-generator/trunk", "Adding 'pdf-generator' plugin to the repository.")
        mockSvnClient.checkOut(wcDir, "grails-pdf-generator/trunk")
        mockSvnClient.addFilesToSvn(expectedFiles)
        mockSvnClient.commit(wcDir, "Releasing version 0.5-SNAPSHOT of the 'pdf-generator' plugin.")
        mockSvnClient.tag(
                "grails-pdf-generator/trunk",
                "grails-pdf-generator/tags",
                "RELEASE_0_5-SNAPSHOT",
                "Tagging the 0.5-SNAPSHOT release of the 'pdf-generator' plugin.")

        mockUpdatePluginList(mockSvnClient, true)

        play {
            def deployer = new SvnDeployer(mockSvnClient, baseDir, pluginListFile, System.out, null)
            deployer.deployPlugin(zipFile, pluginXmlFile, pomFile, false)
        }
    }

    void testDeployPluginNoCredentialsInUrl() {
        def zipContent = "Hello world"
        def zipFile = new File(baseDir, "grails-pdf-generator-1.1.2.zip")
        zipFile.text = zipContent

        def expectedMd5Sum = DigestUtils.md5Hex(zipContent.bytes)
        def expectedSha1Sum = DigestUtils.shaHex(zipContent.bytes)

        def pluginXmlFile = new File(baseDir, "plugin.xml")
        pluginXmlFile.text = "<plugin></plugin>"

        def pomFile = new File(baseDir, "pom.xml")
        pomFile.text = """\
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.example</groupId>
  <artifactId>pdf-generator</artifactId>
  <version>1.1.2</version>
  <packaging>zip</packaging>
</project>
"""

        def pluginListFile = new File(baseDir, "plugin-list.xml")

        // These are the files that should be added to the Subversion
        // repository if they're not already there.
        def wcDir = new File(baseDir, "publish-wc")
        def expectedFiles = [
                new File(wcDir, zipFile.name),
                new File(wcDir, "plugin.xml"),
                new File(wcDir, "pom.xml"),
                new File(wcDir, "${zipFile.name}.sha1"),
                new File(wcDir, "${zipFile.name}.md5") ]

        def askUser = mock()
        askUser.call("Enter your Subversion username:").returns("dilbert")
        askUser.call("Enter your Subversion password:").returns("password")

        // The current directory is not a working copy for the repository.
        def mockSvnClient = mock()
        mockSvnClient.isWorkingCopyForRepository(new File("."), "grails-pdf-generator/trunk").returns(false)
        
        // The plugin is not already in the repository.
        mockSvnClient.pathExists("grails-pdf-generator/trunk").raises(
                new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE)))
        mockSvnClient.setCredentials("dilbert", "password")

        // The trunk path exists, so we don't need to create it.
        mockSvnClient.pathExists("grails-pdf-generator/trunk").returns(true)
        mockSvnClient.checkOut(wcDir, "grails-pdf-generator/trunk")
        mockSvnClient.addFilesToSvn(expectedFiles)
        mockSvnClient.commit(wcDir, "Releasing version 1.1.2 of the 'pdf-generator' plugin.")
        mockSvnClient.tag(
                "grails-pdf-generator/trunk",
                "grails-pdf-generator/tags",
                "RELEASE_1_1_2",
                "Tagging the 1.1.2 release of the 'pdf-generator' plugin.")
        mockSvnClient.tag(
                "grails-pdf-generator/trunk",
                "grails-pdf-generator/tags",
                "LATEST_RELEASE",
                "Making version 1.1.2 of the 'pdf-generator' plugin the latest.")

        mockUpdatePluginList(mockSvnClient, true)

        play {
            def deployer = new SvnDeployer(mockSvnClient, baseDir, pluginListFile, System.out, askUser)
            deployer.deployPlugin(zipFile, pluginXmlFile, pomFile)
        }
    }

    void testDeployPluginPackageDoesNotExist() {
        def zipContent = "Hello world"
        def zipFile = new File(baseDir, "grails-pdf-generator-0.5-SNAPSHOT.zip")
        def pluginXmlFile = new File(baseDir, "plugin.xml")
        def pomFile = new File(baseDir, "pom.xml")

        def pluginListFile = new File(baseDir, "plugin-list.xml")

        def wcDir = new File(baseDir, "publish-wc")
        def mockSvnClient = mock()

        play {
            def deployer = new SvnDeployer(mockSvnClient, baseDir, pluginListFile, System.out, null)

            shouldFail(FileNotFoundException) {
                deployer.deployPlugin(zipFile, pluginXmlFile, pomFile, false)
            }
        }
    }

    void testDeployPluginPluginXmlDoesNotExist() {
        def zipContent = "Hello world"
        def zipFile = new File(baseDir, "grails-pdf-generator-0.5-SNAPSHOT.zip")
        zipFile.text = zipContent

        def pluginXmlFile = new File(baseDir, "plugin.xml")
        def pomFile = new File(baseDir, "pom.xml")

        def pluginListFile = new File(baseDir, "plugin-list.xml")

        def wcDir = new File(baseDir, "publish-wc")
        def mockSvnClient = mock()

        play {
            def deployer = new SvnDeployer(mockSvnClient, baseDir, pluginListFile, System.out, null)

            shouldFail(FileNotFoundException) {
                deployer.deployPlugin(zipFile, pluginXmlFile, pomFile, false)
            }
        }
    }

    void testDeployPluginPomDoesNotExist() {
        def zipContent = "Hello world"
        def zipFile = new File(baseDir, "grails-pdf-generator-0.5-SNAPSHOT.zip")
        zipFile.text = zipContent

        def pluginXmlFile = new File(baseDir, "plugin.xml")
        pluginXmlFile.text = "<plugin></plugin>"

        def pomFile = new File(baseDir, "pom.xml")

        def pluginListFile = new File(baseDir, "plugin-list.xml")

        def wcDir = new File(baseDir, "publish-wc")
        def mockSvnClient = mock()

        play {
            def deployer = new SvnDeployer(mockSvnClient, baseDir, pluginListFile, System.out, null)

            shouldFail(FileNotFoundException) {
                deployer.deployPlugin(zipFile, pluginXmlFile, pomFile, false)
            }
        }
    }

    private mockUpdatePluginList(mockSvnClient, pluginMetaExists) {
        def wcDir = new File(baseDir, "publish-wc")
        mockSvnClient.fetchFile(".plugin-meta/plugins-list.xml", pluginListFile)
        mockSvnClient.latestRevision.returns(1203)
        mockSvnClient.repoUrl.returns("http://svn.codehaus.org/grails-plugins")

        if (pluginMetaExists) {
            mockSvnClient.pathExists(".plugin-meta").returns(true)
        }
        else {
            mockSvnClient.pathExists(".plugin-meta").returns(false)
            mockSvnClient.createPath(".plugin-meta", "Adding .plugin-meta to the repository.")
        }
        mockSvnClient.checkOut(wcDir, ".plugin-meta")
        mockSvnClient.addFilesToSvn([ new File(wcDir, "plugins-list.xml") ])
        mockSvnClient.commit(wcDir, "Updating plugin list for plugin 'pdf-generator'.")
    }
}
