import grails.test.AbstractCliTestCase

class GeneratePomTests extends AbstractCliTestCase {
    void testDefault() {
        runAndVerify()
    }

    private runAndVerify() {
        execute([ "generate-pom" ])

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "GeneratePom script not found.", output.contains("Script not found:")

        // First check that the POM file exists.
        def pomFile = new File("target/pom.xml")
        assertTrue "POM file does not exist", pomFile.exists()

        // Now check the content using XmlSlurper.
        def pom = new XmlSlurper().parseText(pomFile.text)
        assertEquals "4.0.0", pom.modelVersion.text()
        assertEquals "org.grails.plugins", pom.groupId.text()
        assertEquals "phantom", pom.artifactId.text()
        assertEquals "0.3", pom.version.text()
        assertEquals "zip", pom.packaging.text()

        assertEquals "", pom.dependencies.text()
        assertEquals 0, pom.email.size()

        assertEquals "Plugin summary/headline", pom.name.text()
        assertEquals "Brief description of the plugin.\n", pom.description.text()
        assertEquals "http://grails.org/plugin/phantom", pom.url.text()
        assertEquals 1, pom.developers.developer.size()
        assertEquals "Your name", pom.developers.developer[0].name.text()
        assertEquals 0, pom.developers.developer[0].email.size()
    }
}
