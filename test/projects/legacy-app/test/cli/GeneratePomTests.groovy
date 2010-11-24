import grails.test.AbstractCliTestCase

class GeneratePomTests extends AbstractCliTestCase {
    void testDefault() {
        runAndVerify()
    }

    void testWithExistingPom() {
        def pomFile = new File("target/pom.xml")
        pomFile.text = """\
<?xml version="1.0" ?>
<project>
</project>
"""

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
        assertEquals "org.legacy.grails", pom.groupId.text()
        assertEquals "legacy-app", pom.artifactId.text()
        assertEquals "1.0-SNAPSHOT", pom.version.text()
        assertEquals "war", pom.packaging.text()

        assertEquals 0, pom.dependencies.dependency.size()

        assertEquals "legacy-app", pom.name.text()
    }
}
