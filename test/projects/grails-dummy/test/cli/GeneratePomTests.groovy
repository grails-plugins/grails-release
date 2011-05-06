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

        assertEquals 3, pom.dependencies.dependency.size()
        assertEquals "org.grails.plugins", pom.dependencies.dependency[0].groupId.text()
        assertEquals "debug", pom.dependencies.dependency[0].artifactId.text()
        assertEquals "[1.0,)", pom.dependencies.dependency[0].version.text()

        assertEquals "org.grails.plugins", pom.dependencies.dependency[1].groupId.text()
        assertEquals "shiro", pom.dependencies.dependency[1].artifactId.text()
        assertEquals "1.1-SNAPSHOT", pom.dependencies.dependency[1].version.text()

        assertEquals "org.grails.plugins", pom.dependencies.dependency[2].groupId.text()
        assertEquals "geb-spock", pom.dependencies.dependency[2].artifactId.text()
        assertEquals "[1.1,1.3]", pom.dependencies.dependency[2].version.text()

        assertEquals "Dummy plugin", pom.name.text()
        assertEquals "A dummy plugin. Only used for testing.", pom.description.text()
        assertEquals "http://grails.org/plugin/dummy", pom.url.text()
        assertEquals "Apache License 2.0", pom.licenses.license[0].name.text()
        assertEquals "http://www.apache.org/licenses/LICENSE-2.0.txt", pom.licenses.license[0].url.text()
        assertEquals "SpringSource", pom.organization.name.text()
        assertEquals "http://www.springsource.org/", pom.organization.url.text()
        assertEquals 3, pom.developers.developer.size()
        assertEquals "Jane Doe", pom.developers.developer[0].name.text()
        assertEquals "jdoe@springsource.org", pom.developers.developer[0].email.text()
        assertEquals "Peter Ledbrook", pom.developers.developer[1].name.text()
        assertEquals "pledbrook@somewhere.net", pom.developers.developer[1].email.text()
        assertEquals "Graeme Rocher", pom.developers.developer[2].name.text()
        assertEquals "grocher@somewhere.net", pom.developers.developer[2].email.text()
        assertEquals "JIRA", pom.issueManagement.system.text()
        assertEquals "http://jira.codehaus.org/browse/GRAILSPLUGINS", pom.issueManagement.url.text()
        assertEquals "http://svn.grails-plugins.codehaus.org/browse/grails-plugins/", pom.scm.url.text()
    }
}
