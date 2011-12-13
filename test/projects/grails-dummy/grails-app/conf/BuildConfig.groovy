grails.plugin.location.publish = "../../.."
grails.project.work.dir = "target"
grails.project.plugins.dir = "plugins"

//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits( "global" ) {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {        
        grailsPlugins()
        grailsHome()
        grailsCentral()

        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenLocal()
        //mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.5'
        test "org.codehaus.geb:geb-spock:0.6.0"
    }

    dependencies {
        provided "org.apache.ivy:ivy:2.2.0"
        compile("org.apache.maven:maven-ant-tasks:2.1.0") {
            transitive = false
        }
    }
    plugins {
        compile ":debug:[1.0,)"
        compile ":geb:[0.5.0,0.6.0]"
        compile ":shiro:1.1-SNAPSHOT"
    }
}
