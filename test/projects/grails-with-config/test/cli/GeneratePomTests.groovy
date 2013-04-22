import grails.test.AbstractCliTestCase

class GeneratePomTests extends AbstractCliTestCase {
    def basePom = new File("pom.xml")
    def pomFile = new File("target/pom.xml")

    void setUp() {
        basePom.delete()
        pomFile.delete()
    }

    void tearDown() {
        basePom.delete()
        pomFile.delete()
    }

    void testDefault() {
        runAndVerify()
    }

    void testWithExistingPom() {
        pomFile.text = """\
<?xml version="1.0" ?>
<project>
</project>
"""

        runAndVerify()
    }

    void testWithBasePom() {
        basePom.text = """\
<?xml version="1.0" ?>
<project>
</project>
"""

        execute([ "generate-pom" ])

        assertEquals 1, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "GeneratePom script not found.", output.contains("Script not found:")

        // It should have printed a message about the base POM already
        // existing.
        assertTrue "Command did not say that POM generation was skipped.", output.contains("Skipping POM generation because 'pom.xml' exists in the root of the project")

        // Check that the POM file does *not* exist.
        assertFalse "Generated POM file does exists", pomFile.exists()
    }

    private runAndVerify() {
        execute([ "generate-pom" ])

        assertEquals 0, waitForProcess()
        verifyHeader()

        // Make sure that the script was found.
        assertFalse "GeneratePom script not found.", output.contains("Script not found:")

        // First check that the POM file exists.
        assertTrue "POM file does not exist", pomFile.exists()

        // Now check the content using XmlSlurper.
        def pom = new XmlSlurper().parseText(pomFile.text)
        assertEquals "4.0.0", pom.modelVersion.text()
        assertEquals "org.grails.plugins", pom.groupId.text()
        assertEquals "with-config", pom.artifactId.text()
        assertEquals "0.1", pom.version.text()
        assertEquals "zip", pom.packaging.text()

        assertEquals 5, pom.dependencies.dependency.size()

        def depNames = pom.dependencies.dependency.artifactId*.text() as Set
        assert "httpclient" in depNames
        assert "commons-io" in depNames
        assert "fixtures" in depNames
        assert "spring-security-core" in depNames
        assert "hibernate" in depNames

        def ssc = pom.dependencies.dependency.find { it.artifactId.text() == "spring-security-core" }
        assert ssc.exclusions.exclusion.size() == 4
        assert ssc.exclusions.exclusion.artifactId.any { it.text() == "excluded-dep" }

        def httpc = pom.dependencies.dependency.find { it.artifactId.text() == "httpclient" }
        assert httpc.exclusions.exclusion.size() == 5
        assert httpc.exclusions.exclusion.artifactId.any { it.text() == "commons-logging" }
        assert httpc.exclusions.exclusion.artifactId.any { it.text() == "commons-codec" }

        def cio = pom.dependencies.dependency.find { it.artifactId.text() == "commons-io" }
        assert cio.exclusions.exclusion.size() == 3

        assertEquals "Plugin summary/headline", pom.name.text()
        assertEquals "Brief description of the plugin.", pom.description.text().trim()
        assertEquals "http://grails.org/plugin/with-config", pom.url.text()
        assertEquals 1, pom.licenses.size()
        assertEquals "Bumblebee", pom.licenses.license[0].name.text()
        assertEquals "http://www.bmblbee.org/licenses/LICENSE.txt", pom.licenses.license[0].url.text()
        assertEquals 0, pom.organization.size()
        assertEquals 1, pom.developers.developer.size()
        assertEquals "Your name", pom.developers.developer[0].name.text()
        assertEquals "", pom.developers.developer[0].email.text()
        assertEquals 0, pom.issueManagement.size()
        assertEquals 0, pom.scm.size()
    }
}
