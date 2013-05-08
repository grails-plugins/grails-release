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

        // Make sure that the script was found.
        assertFalse "GeneratePom script not found.", output.contains("Script not found:")

        // First check that the POM file exists.
        def pomFile = new File("target/pom.xml")
        assertTrue "POM file does not exist", pomFile.exists()

        // Now check the content using XmlSlurper.
        def pom = new XmlSlurper().parseText(pomFile.getText("UTF-8"))
        assertEquals "4.0.0", pom.modelVersion.text()
        assertEquals "org.example.grails", pom.groupId.text()
        assertEquals "dummy", pom.artifactId.text()
        assertEquals "0.1", pom.version.text()
        assertEquals "zip", pom.packaging.text()

        assertEquals 5, pom.dependencies.dependency.size()

        verifyDependency pom, "org.grails.plugins", "debug", "[1.0,)"
        verifyDependency pom, "org.grails.plugins", "shiro", "1.2.0-SNAPSHOT"
        verifyDependency pom, "org.grails.plugins", "geb", "[0.5.0,0.6.0]"
        verifyDependency pom, "org.apache.maven", "maven-ant-tasks", "2.1.0"
        verifyDependency pom, "org.apache.ivy", "ivy", "2.2.0", "provided"

        def dep = pom.dependencies.dependency.find { it.artifactId.text() == 'maven-ant-tasks'}

        assert dep != null
        // check that a transitive = false dependency has generated exclusions
        assert dep.exclusions.exclusion.size() == 15

        def expectedPluginTitle = new String([68, -61, -68, 109, 109, 121, 32, 112, 108, 117, 103, 105, 110] as byte[], "UTF-8")
        assertEquals expectedPluginTitle, pom.name.text() // UTF-8 Test
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

    void verifyDependency(pom, group, name, version, scope = null) {
       def dep = pom.dependencies.dependency.find { it.artifactId.text() == name}

       assert dep != null
       assert name == dep.artifactId.text()
       assert version == dep.version.text()
       assert group == dep.groupId.text()

       if (scope) assert scope == dep.scope.text()
    }
}
