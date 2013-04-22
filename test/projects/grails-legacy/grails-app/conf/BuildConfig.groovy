grails.plugin.location.publish = "../../.."

grails.plugin.repos.distribution.myRepo = "http://rimu/svn/grails-plugins"
grails.project.dependency.distribution = {
    remoteRepository(id: "maven1", url: "http://rimu:8081/artifactory/plugins-releases-local") {
        authentication username: "admin", password: "password"
    }

    remoteRepository(id: "maven1-snapshots", type: "maven", url: "http://rimu:8081/artifactory/plugins-snapshots-local") {
        authentication username: "admin", password: "password"
    }

    remoteRepository(id: "svn1", type: "svn", url: "http://peter:password@svn.codehaus.org/grails-plugins")
}

grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
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
    }
}
