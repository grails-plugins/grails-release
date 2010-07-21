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
        assertEquals "org.example.grails", pom.groupId.text()
        assertEquals "dummy", pom.artifactId.text()
        assertEquals "0.1", pom.version.text()
        assertEquals "zip", pom.packaging.text()

        assertEquals "", pom.dependencies.text()

        assertEquals "Dummy plugin", pom.name.text()
        assertEquals "A dummy plugin. Only used for testing.", pom.description.text()
        assertEquals "http://grails.org/plugin/dummy", pom.url.text()
        assertEquals "Apache License 2.0", pom.licenses.license[0].name.text()
        assertEquals "http://www.apache.org/licenses/LICENSE-2.0.txt", pom.licenses.license[0].url.text()
  /*
  <organization>...</organization>
  <developers>...</developers>
  <contributors>...</contributors>
  */
        
    }
}
