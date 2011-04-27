class MavenPublisherGrailsPlugin {
    def version = "0.8.2"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Graeme Rocher"
    def authorEmail = "grocher@vmware.com"
    def title = "Maven Publisher"
    def description = '''\
A plugin that allows you to publish Grails applications and plugins to Maven repositories without needing to use Maven directly.
'''

    def license = "APACHE"
    def organization = [ name: "SpringSource", url: "http://www.springsource.org/" ]
    def developers = [ [ name: "Peter Ledbrook", email: "pledbrook@vmware.com" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.codehaus.org/browse/GRAILSPLUGINS" ]
    def scm = [ url: "https://github.com/grails-plugins/grails-maven-publisher-plugin" ]

    def documentation = "http://github.com/grails-plugins/grails-maven-publisher-plugin"
}
