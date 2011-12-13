import grails.test.AbstractCliTestCase

class PublishPluginTests extends AbstractCliTestCase {
    def packageFile = new File("grails-phantom-0.3.zip").canonicalFile
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
        assertTrue "Command is not publishing to mySvn repo.", output.contains("Publishing to Subversion repository 'mySvn'")

        verifyUploadFiles()

        def eventLine = output.find(~/^Starting to publish plugin: .*$/)
        assert "PublishPluginStart event not fired.", eventLine
        assert "PublishPluginStart event args not correct.",
                (eventLine.contains("name:grails-phantom") &&
                 eventLine.contains("group:org.grails.plugins") &&
                 eventLine.contains("version:0.3") &&
                 eventLine.contains("isSnapshot:false"))

        eventLine = output.find(~/^Finished publishing plugin: .*$/)
        assert "PublishPluginEnd event not fired.", eventLine
        assert "PublishPluginEnd event args not correct.",
                (eventLine.contains("name:grails-phantom") &&
                 eventLine.contains("group:org.grails.plugins") &&
                 eventLine.contains("version:0.3") &&
                 eventLine.contains("isSnapshot:false"))

        eventLine = output.find(~/^Starting to deploy plugin: .*$/)
        assert "DeployPluginStart event not fired.", eventLine
        assert "DeployPluginStart event args not correct.",
                (eventLine.contains("name:grails-phantom") &&
                 eventLine.contains("group:org.grails.plugins") &&
                 eventLine.contains("version:0.3") &&
                 eventLine.contains("isSnapshot:false"))

        eventLine = output.find(~/^Finished deploying plugin: .*$/)
        assert "DeployPluginEnd event not fired.", eventLine
        assert "DeployPluginEnd event args not correct.",
                (eventLine.contains("name:grails-phantom") &&
                 eventLine.contains("group:org.grails.plugins") &&
                 eventLine.contains("version:0.3") &&
                 eventLine.contains("isSnapshot:false"))

        eventLine = output.find(~/^Starting to ping plugin portal: .*$/)
        assert "PingPortalStart event not fired.", eventLine
        assert "PingPortalStart event args not correct.",
                (eventLine.contains("name:grails-phantom") &&
                 eventLine.contains("group:org.grails.plugins") &&
                 eventLine.contains("version:0.3") &&
                 eventLine.contains("isSnapshot:false"))

        eventLine = output.find(~/^Finished pinging plugin portal: .*$/)
        assert "PingPortalEnd event not fired.", eventLine
        assert "PingPortalEnd event args not correct.",
                (eventLine.contains("name:grails-phantom") &&
                 eventLine.contains("group:org.grails.plugins") &&
                 eventLine.contains("version:0.3") &&
                 eventLine.contains("isSnapshot:false"))
    }

    void testExplicitSnapshot() {
        execute([ "publish-plugin", "--dry-run", "--repository=grailsCentral" ])
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
