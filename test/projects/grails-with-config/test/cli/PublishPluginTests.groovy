import grails.test.AbstractCliTestCase

class PublishPluginTests extends AbstractCliTestCase {
    def packageFile = new File("grails-with-config-0.1.zip").canonicalFile
    def pdFile = new File("plugin.xml").canonicalFile
    def basePom = new File("pom.xml").canonicalFile
    def pomFile = new File("target/pom.xml").canonicalFile

    void setUp() {
        packageFile.delete()
        pdFile.delete()
        basePom.delete()
        pomFile.delete()
    }

    void tearDown() {
        packageFile.delete()
        pdFile.delete()
        basePom.delete()
        pomFile.delete()
    }

    void testDefault() {
        execute([ "publish-plugin", "--dryRun" ])
        enterInput "n"
             
        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure SCM is not enabled.
        assertFalse "SCM enabled when it shouldn't be.",
                output.contains("Project is not under source control. Do you want to import it now?")

        // Make sure it's publishing to Grails central.
        assertTrue "Command is not publishing to Grails central.", output.contains("Publishing to Grails Central")
        
        // Make sure the main public plugin portal is notified.
        assertTrue "Command is not notifying grailsCentral plugin portal.",
                output.contains("Notifying plugin portal 'http://grails.org/plugin/with-config' of release")

        verifyUploadFiles()
    }

    void testWithMavenRepo() {
        execute([ "publish-plugin", "--dryRun", "--repository=maven1", "--noScm" ])
        enterInput "n"

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure SCM is not enabled.
        assertFalse "SCM enabled when it shouldn't be.",
                output.contains("Project is not under source control. Do you want to import it now?")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Maven repository 'maven1'")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")
        
        // Make sure that no plugin portal is notified, since this repository has none configured.
        assertTrue "Command is notifying a plugin portal.", output.contains("No default portal defined for repository 'maven1' - skipping portal notification")

        assertTrue "Command not confirming that username/password config used.", output.contains("Using configured username and password from grails.project.repos.maven1")
        
        verifyUploadFiles()
    }

    void testWithExistingPOM() {
        basePom.text = """\
<?xml version="1.0" ?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.springsource</groupId>
    <artifactId>my-plugin</artifactId>
    <version>0.3</version>
    <packaging>grails-plugin</packaging>
</project>
"""

        execute([ "publish-plugin", "--dryRun", "--repository=maven1", "--scm" ])
        enterInput "n"

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure SCM is enabled.
        assertTrue "SCM is not enabled.",
                output.contains("Project is not under source control. Do you want to import it now?")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Maven repository 'maven1'")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")
        
        // Make sure that no plugin portal is notified, since this repository has none configured.
        assertTrue "Command is notifying a plugin portal.", output.contains("No default portal defined for repository 'maven1' - skipping portal notification")
        
        verifyUploadFiles(basePom)
    }

    void testWithExplicitMavenRepo() {
        execute([ "publish-plugin", "--dryRun", "--repository=maven1-snapshots" ])
        enterInput "n"

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Maven repository 'maven1-snapshots'")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")

        assertTrue "Command not confirming that username/password config used.", output.contains("Using configured username and password from grails.project.repos.maven1")
        
        // Make sure the main public plugin portal is notified.
        assertTrue "Command is not notifying the my-portal plugin portal.", output.contains("Notifying plugin portal 'http://beta.grails.org/plugin/with-config' of release")
        
        verifyUploadFiles()
    }

    void testWithSubversionRepo() {
        execute([ "publish-plugin", "--dryRun", "--repository=svn1" ])
        enterInput "n"

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Subversion repository 'svn1'")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")

        // Make sure the main public plugin portal is notified.
        assertTrue "Command is not notifying grailsCentral plugin portal.", output.contains("Notifying plugin portal 'http://grails.org/plugin/with-config' of release")

        verifyUploadFiles()
    }

    void testLegacySubversionConfig() {
        execute([ "publish-plugin", "--dryRun", "--repository=myRepo" ])
        enterInput "n"

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Subversion repository 'myRepo'")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")
        
        verifyUploadFiles()
    }

    void testUnknownRepository() {
        execute([ "publish-plugin", "--dryRun", "--repository=dummy" ])
        enterInput "n"

        assertEquals 1, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")

        // Check that command tells the user that the repository isn't
        // recognised.
        assertTrue "Command is not reporting an unknown repository.", output.contains("No configuration found for repository 'dummy'")
    }

    void testIncompleteCredentialsInConfig() {
        execute([ "publish-plugin", "--dryRun", "--repository=bad" ])
        enterInput "n"

        assertEquals 1, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Subversion repository 'bad'")

        // Check that it warns the user that he or she must declare the
        // username *and* password, or neither.
        assertTrue "Command is not reporting username/password error.", output.contains("grails.project.repos.bad.username and .password must both be defined or neither")
    }

    void verifyUploadFiles(pom = pomFile) {
        // Ensure that it is publishing the appropriate plugin package
        // and associated metadata files.
        assertTrue "Command not publishing correct package.", output.contains("Deploying the plugin package ${packageFile.path}")
        assertTrue "Command not publishing XML plugin descriptor.", output.contains("with plugin descriptor ${pdFile.path}")
        assertTrue "Command not publishing POM.", output.contains("and POM file ${pom.path}")
        assertTrue "Command is not publishing a release version.", output.contains("This is a release version")

        // Make sure that those files exist.
        assertTrue "Plugin package does not exist.", packageFile.exists()
        assertTrue "Plugin XML descriptor does not exist.", pdFile.exists()
        assertTrue "POM does not exist.", pom.exists()
    }
}
