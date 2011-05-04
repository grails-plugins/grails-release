grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.docs.output.dir = "docs"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits "global"
    log "warn"

    repositories {        
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
    }

    plugins {
        runtime ":svn:1.0.0.M1"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        build "org.apache.maven:maven-ant-tasks:2.1.0",
              "org.codehaus.groovy.modules.http-builder:http-builder:0.5.0", {
            excludes "commons-logging", "xml-apis", "groovy"
        }
        test  "org.gmock:gmock:0.8.0", {
            export = false
        }
    }
}
