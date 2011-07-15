grails.plugin.location.publish = "../../.."

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits "global"
    log "warn"
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()
    }
    plugins {
        // TODO This should be a version range, but the Grails repository resolver
        // can't handle those yet: http://jira.grails.org/browse/GRAILS-7491.
        compile group: "org.grails.plugins", name: "spring-security-core", version: "1.0", {
            exclude "excluded-dep"
        }
        compile "org.grails.plugins:fixtures:1.0.3", "org.grails.plugins:hibernate:1.3.7"
    }
    dependencies {
        compile "org.apache.httpcomponents:httpclient:4.1.1", {
            excludes "commons-logging", "commons-codec"
        }
        compile group: "commons-io", name: "commons-io", version: "[1.4,)"
    }
}

grails {
    release.scm.enabled = false
    plugin.repos.distribution.myRepo = "svn+ssh://localhost:23445/svn"
    project {
        work.dir = "target"

        portal.'my-portal'.url = "http://beta.grails.org/plugin/"
        portal {
            beta {
                url = "http://beta.grails.org/plugin/"
                username = "dil"
                password = "pass"
            }
        }

        repos {
            maven1 {
                url = "http://rimu:8081/artifactory/plugins-releases-local"
                username = "admin"
                password = "password"
            }

            'maven1-snapshots' {
                type = "maven"
                url = "http://rimu:8081/artifactory/plugins-snapshots-local"
                username = "dilbert"
                password = "test"
                custom = {
                    authentication username: "admin", password: "password"
                }
                portal = "my-portal"
            }

            svn1 {
                type = "svn"
                url = "http://peter:password@svn.codehaus.org/grails-plugins"
                portal = "grailsCentral"
            }

            bad {
                type = "svn"
                url = "svn+ssh://svn.codehaus.org/grails-plugins"
                username = "peter"
            }
        }
    }
}
