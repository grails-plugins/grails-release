package grails.plugins.publish.maven

import groovy.xml.NamespaceBuilder
import org.gmock.WithGMock

@WithGMock
class MavenDeployerUnitTests extends GroovyTestCase {
    void testDeployPlugin() {
        def packageFile = new File("grails-something-1.0.zip")
        def pluginXmlFile = new File("plugin.xml")
        def pomFile = new File("target/pom.xml")
        
        def repoDefn = [ args: [ id: "remote1", url: "http://rimu:8081/artifactory/" ] ]
        def testDelegate = new DeployTestDelegate()

        def mockAnt = mock()
        mockAnt.'install-provider'(artifactId: "wagon-http", version: "1.0-beta-2")
        mockAnt.deploy(file: packageFile, match {
            def c = it.clone()
            c.delegate = testDelegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()
            return it instanceof Closure
        })
        
        def mockNamespaceBuilder = mock(NamespaceBuilder)
        mockNamespaceBuilder.static.newInstance(mockAnt, 'antlib:org.apache.maven.artifact.ant').returns(mockAnt)

        play {
            def deployer = new MavenDeployer(mockAnt, repoDefn, "wagon-http")
            deployer.deployPlugin(packageFile, pluginXmlFile, pomFile, false)
            
            assertEquals pluginXmlFile, testDelegate.attachArgs.file
            assertEquals "xml", testDelegate.attachArgs.type
            assertEquals "plugin", testDelegate.attachArgs.classifier
            assertEquals pomFile, testDelegate.pomFile
            assertEquals repoDefn["args"], testDelegate.repoArgs
            assertNull testDelegate.repoConfigurer
        }
    }
    
    void testDeployPluginWithRepositoryConfigurer() {
        def packageFile = new File("grails-something-1.0.zip")
        def pluginXmlFile = new File("plugin.xml")
        def pomFile = new File("target/pom.xml")
        
        def repoDefn = [ args: [ id: "remote1", url: "http://rimu:8081/artifactory/", type: "maven" ], configurer: "test" ]
        def testDelegate = new DeployTestDelegate()

        def mockAnt = mock()
        mockAnt.'install-provider'(artifactId: "wagon-http", version: "1.0-beta-2")
        mockAnt.deploy(file: packageFile, match {
            def c = it.clone()
            c.delegate = testDelegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()
            return it instanceof Closure
        })
        
        def mockNamespaceBuilder = mock(NamespaceBuilder)
        mockNamespaceBuilder.static.newInstance(mockAnt, 'antlib:org.apache.maven.artifact.ant').returns(mockAnt)

        play {
            def deployer = new MavenDeployer(mockAnt, repoDefn, "wagon-http")
            deployer.deployPlugin(packageFile, pluginXmlFile, pomFile, true)
            
            assertEquals pluginXmlFile, testDelegate.attachArgs.file
            assertEquals "xml", testDelegate.attachArgs.type
            assertEquals "plugin", testDelegate.attachArgs.classifier
            assertEquals pomFile, testDelegate.pomFile
            assertEquals repoDefn["args"]["id"], testDelegate.repoArgs["id"]
            assertEquals repoDefn["args"]["url"], testDelegate.repoArgs["url"]
            assertNull testDelegate.repoArgs["type"]
            assertEquals repoDefn["configurer"], testDelegate.repoConfigurer
        }
    }
}

class DeployTestDelegate {
    def attachArgs
    def pomFile
    def repoArgs
    def repoConfigurer
    
    void attach(args) {
        attachArgs = args
    }
    
    void pom(args) {
        pomFile = args?.file
    }
    
    void remoteRepository(args, configurer = null) {
        repoArgs = args
        repoConfigurer = configurer
    }
}
