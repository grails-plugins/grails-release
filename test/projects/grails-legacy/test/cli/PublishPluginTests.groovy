import grails.test.AbstractCliTestCase

class PublishPluginTests extends AbstractCliTestCase {
    def packageFile = new File("grails-legacy-1.1-SNAPSHOT.zip").canonicalFile
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
        execute([ "publish-plugin", "--dry-run" ])
        enterInput ""
        enterInput "n"
             
        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's publishing to Grails central.
        assertTrue "Command is not publishing to Grails central.", output.contains("Publishing to Grails Central")

        verifyUploadFiles()
    }

    void testWithMavenRepo() {
        execute([ "publish-plugin", "--dry-run", "--repository=maven1" ])
        enterInput ""
        enterInput "n"

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Maven repository 'maven1'")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")
        
        verifyUploadFiles()
    }

    void testWithExistingPOM() {
        basePom.text = """\
<?xml version="1.0" ?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.springsource</groupId>
    <artifactId>my-plugin</artifactId>
    <version>0.3-SNAPSHOT</version>
    <packaging>grails-plugin</packaging>
</project>
"""

        execute([ "publish-plugin", "--dry-run", "--repository=maven1" ])
        enterInput ""
        enterInput "n"

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Maven repository 'maven1'")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")
        
        verifyUploadFiles(basePom)
    }

    void testWithExplicitMavenRepo() {
        execute([ "publish-plugin", "--dry-run", "--repository=maven1-snapshots" ])
        enterInput ""
        enterInput "n"

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Maven repository 'maven1-snapshots'")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")
        
        verifyUploadFiles()
    }

    void testWithSubversionRepo() {
        execute([ "publish-plugin", "--dry-run", "--repository=svn1" ])
        enterInput ""
        enterInput "n"

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's reporting the repository it's publishing to.
        assertTrue "Command is not reporting the correct repository.", output.contains("Publishing to Subversion repository 'svn1'")

        // Make sure it's not publishing to Grails central.
        assertFalse "Command is publishing to Grails central when it shouldn't be.", output.contains("Publishing to Grails Central")
        
        verifyUploadFiles()
    }

    void testLegacySubversionConfig() {
        execute([ "publish-plugin", "--dry-run", "--repository=myRepo" ])
        enterInput ""
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
        execute([ "publish-plugin", "--dry-run", "--repository=dummy" ])
        enterInput ""
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

    void verifyUploadFiles(pom = pomFile) {
        // Ensure that it is publishing the appropriate plugin package
        // and associated metadata files.
        assertTrue "Command not publishing correct package.", output.contains("Deploying the plugin package ${packageFile.path}")
        assertTrue "Command not publishing XML plugin descriptor.", output.contains("with plugin descriptor ${pdFile.path}")
        assertTrue "Command not publishing POM.", output.contains("and POM file ${pom.path}")
        assertTrue "Command is publishing a release version when it shouldn't be.", output.contains("This is not a release version")

        // Make sure that those files exist.
        assertTrue "Plugin package does not exist.", packageFile.exists()
        assertTrue "Plugin XML descriptor does not exist.", pdFile.exists()
        assertTrue "POM does not exist.", pom.exists()
    }
}
