class ReleaseGrailsPlugin {
    def version = "1.0-SNAPSHOT"
    def grailsVersion = "1.2 > *"

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Graeme Rocher"
    def authorEmail = "grocher@vmware.com"
    def title = "Release Plugin"
    def description = '''\
A plugin that allows you to publish Grails plugins, either to a public or private repository. It also supports deploying Grails applications and plugins to Maven repositories without the need to use Maven directly.
'''

    def license = "APACHE"
    def organization = [ name: "SpringSource", url: "http://www.springsource.org/" ]
    def developers = [ [ name: "Peter Ledbrook", email: "pledbrook@vmware.com" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.codehaus.org/browse/GRAILSPLUGINS" ]
    def scm = [ url: "https://github.com/grails-plugins/grails-maven-publisher-plugin" ]

    def documentation = "http://github.com/grails-plugins/grails-maven-publisher-plugin"
}
