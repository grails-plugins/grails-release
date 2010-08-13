import grails.test.AbstractCliTestCase

class PublishPluginTests extends AbstractCliTestCase {
    def packageFile = new File("grails-dummy-0.1.zip").canonicalFile
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
             
        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's publishing to Grails central.
        assertTrue "Command is not publishing to Grails central.", output.contains("Publishing to Grails Central")

        verifyUploadFiles()
    }

    void testExplicitySnapshot() {
        execute([ "publish-plugin", "--dry-run", "--snapshot" ])

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "PublishPlugin script not found.", output.contains("Script not found:")

        // Make sure it's publishing to Grails central.
        assertTrue "Command is not publishing to Grails central.", output.contains("Publishing to Grails Central")

        verifyUploadFiles(false)
    }

    void verifyUploadFiles(isRelease = true) {
        // Ensure that it is publishing the appropriate plugin package
        // and associated metadata files.
        assertTrue "Command not publishing correct package.", output.contains("Deploying the plugin package ${packageFile.path}")
        assertTrue "Command not publishing XML plugin descriptor.", output.contains("with plugin descriptor ${pdFile.path}")
        assertTrue "Command not publishing POM.", output.contains("and POM file ${pomFile.path}")
        
        if (isRelease) {
            assertTrue "Command is not publishing a release version.", output.contains("This is a release version")
        }
        else {
            assertTrue "Command is publishing a release version when it shouldn't be.", output.contains("This is not a release version")
        }

        // Make sure that those files exist.
        assertTrue "Plugin package does not exist.", packageFile.exists()
        assertTrue "Plugin XML descriptor does not exist.", pdFile.exists()
        assertTrue "POM does not exist.", pomFile.exists()
    }
}
