class DummyGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [ debug: '1.0 > *', shiro: '1.2.0-SNAPSHOT', geb: '0.5.0 > 0.6.0' ]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def author = "Jane Doe"
    def authorEmail = "jdoe@springsource.org"
    def title = "Dümmy plugin"
    def description = "A dummy plugin. Only used for testing."
    def groupId = "org.example.grails"

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/dummy"

    def license = "APACHE"
    def organization = [ name: "SpringSource", url: "http://www.springsource.org/" ]
    def developers = [
            [ name: "Peter Ledbrook", email: "pledbrook@somewhere.net" ],
            [ name: "Graeme Rocher", email: "grocher@somewhere.net" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.codehaus.org/browse/GRAILSPLUGINS" ]
    def scm = [ url: "http://github.com/grails/grails-plugins/dummy" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
