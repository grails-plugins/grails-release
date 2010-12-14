Maven Publisher Plugin
======================

This plugin enables you to publish application WAR files and plugin packages (zip files) to a Maven-compatible
repository. It can also install them to your local Maven cache.

*As of version 0.7* you can publish your plugins to both Maven-compatible and Subversion repositories.

Installation
------------

Use either the command:

    grails install-plugin maven-publisher

or add this dependency to `BuildConfig.groovy` (Grails 1.3+ only):

    build ":maven-publisher:0.6"

Don't forget to specify the version you want in the latter example (0.6 may not be the latest when you read
this).

Usage
-----

The plugin provides two commands:

    grails maven-install
    grails maven-deploy

The first of these commands installs the WAR or zip (depending on the type of your project) to your local Maven
cache. The second one deploys the package to a remote Maven-compatible repository.

Configuration comes in the form of a `grails.project.dependency.distribution` setting in `BuildConfig.groovy`,
which allows you to define multiple repositories identified by name and the location of the local Maven cache.
Let's take a look at an example:

    grails.project.dependency.distribution = {
        localRepository = "/path/to/local/cache"

        remoteRepository(id: "snapshots", url: "http://myserver:8081/artifactory/libs-snapshots-local")

        remoteRepository(id: "releases", url: "http://myserver:8081/artifactory/libs-releases-local") {
            authentication username: "admin", password: "password"
        }
    }

As you can probably gather, you can change the path for the local Maven cache by setting `localRepository` to
a local file path. The syntax for configuring remote repositories is different, but it's fairly self-explanatory.
In this example, we set up two repositories called "snapshots" and "releases" respectively. In many cases you
just need to specify the URL of the repository, but if the repository requires authentication, you can use the
syntax shown in the second remote repository definition.

The `maven-install` command takes no arguments, but with `maven-deploy`, you need to tell it which repository
to publish your artifact to. This is done with a `--repository` argument that you set to the name of the
repository you want:

    grails maven-deploy --repository=releases

The name of the repository on the command line must match one of the IDs in the remote repository definitions
in `BuildConfig.groovy`.

New to 0.7
----------

Version 0.7 of the plugin introduces some new commands. The first of these,
    
    grails generate-pom
    
will generate a pom.xml file for the current project. This is the same one as generated when you execute
`maven-deploy`, but now it can include extra information such as who the developers are and how to access both
issue tracking and source control. Where does this information come from? It's only supported by plugins at the
moment as it is extracted from properties of the plugin descriptor. The supported properties, with example values,
are:

    def license = "APACHE"
    def organization = [ name: "SpringSource", url: "http://www.springsource.org/" ]
    def developers = [
            [ name: "Peter Ledbrook", email: "pledbrook@somewhere.net" ],
            [ name: "Graeme Rocher", email: "grocher@somewhere.net" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.codehaus.org/browse/GRAILSPLUGINS" ]
    def scm = [ url: "http://svn.grails-plugins.codehaus.org/browse/grails-plugins/" ]

The names of the properties and the keys used in the maps match the element names used in the POM.

The other new command is

    grails publish-plugin [--dry-run] [--snapshot] [--repository=repoId] [--protocol=protocol] [--portal=portal] [--pingOnly]

which is dedicated to publishing plugins. Although the arguments are very similar to the ones used by the
`maven-deploy` command, `publish-plugin` will also deploy to Subversion repositories just like the existing
`release-plugin` command.

If you don't provide any arguments at all, the command will publish your plugin to the Grails Central repository.
Otherwise, it will use the repository with the given ID. You can define repositories in the same way as you do
for `maven-deploy`, but there are a couple of extra options.

First, you can use an old style repository definition:

    grails.plugin.repos.distribution.<repoId> = "http://peter:password@myrepo.somewhere.net/svn/grails-plugins"

Note that in this case, any authentication credentials are included in the URL itself. The second option is a
`type` argument to the newer repository definition syntax, like so:

    grails.project.dependency.distribution = {
        remoteRepository(id: "myRepo", type: "svn", url: "http://peter:password@myrepo.somewhere.net/svn/grails-plugins")
    }

Again, the credentials must be included in the URL. Also, you can specify a type of "maven" if you want to be
explicit, but you don't have to since it's the default for this syntax.

The plugin can notify any portal of the plugin release, although it defaults to the main portal on grails.org. You can define alternative portals using a similar syntax to remote repositories:

    grails.project.dependency.distribution = {
        portal id: "beta", url: "http://beta.grails.org/plugin/"
    }

`--dry-run` will simply print out what files will be deployed. It won't actually perform the deployment.

`--snapshot` forces the command to treat the plugin as a snapshot version. In other words, it will not be
marked as the latest release. This currently only affects deployments to Subversion repositories. By default,
plugins are treated as release versions unless their version number has a '-SNAPSHOT' suffix.

`--repository` specifies which Subversion or Maven-compatible repository the plugin should be deployed to. A remote
repository must first be defined with that ID in BuildConfig.groovy.

`--protocol` specifies the protocol to use when deploying the plugin to a Maven-compatible repository. Can be one of
'http', 'scp', 'scpexe', 'ftp', or 'webdav'. The default is 'http'.

`--portal` specifies the ID of the portal to notify. For this to work, a portal must be configured for that ID
in BuildConfig.groovy.

`--pingOnly` (version 0.7.3+) forces the command to only notify the configured portal - the plugin is _not_
published to a repository.

One final note: `publish-plugin` does not automatically commit source code changes to a Subversion repository.
It's the equivalent of `release-plugin --zipOnly`.
