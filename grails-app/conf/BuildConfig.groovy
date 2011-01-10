grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir	= "target/test-reports"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits "global"
    log "warn"

    repositories {        
        grailsPlugins()
        grailsHome()

        mavenLocal()
        mavenCentral()
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        build "org.apache.maven:maven-ant-tasks:2.1.0",
              "org.codehaus.groovy.modules.http-builder:http-builder:0.5.0", {
            excludes "commons-logging", "xml-apis", "groovy"
        }
        compile "org.tmatesoft.svnkit:svnkit:1.3.3", {
            excludes "jna", "trilead-ssh2", "sqljet"
        }
        test  "org.gmock:gmock:0.8.0", {
            export = false
        }
    }
}

grails.project.dependency.distribution = {
    portal id: "beta", url: "http://beta.grails.org/plugin/"
}
