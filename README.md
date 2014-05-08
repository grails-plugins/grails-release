Release Plugin
==============

This plugin enables you to publish application WAR files and plugin packages (zip files) to
Maven-compatible repositories. It can also install them to your local Maven cache.

Installation
------------

Use either the command:

    grails install-plugin release

or add this dependency to `BuildConfig.groovy` (Grails 1.3+ only):

    build ":release:<pluginVersion>"

where <pluginVersion> is the version of the plugin you want to use.

Usage
-----

The plugin provides four commands:

    grails generate-pom
    grails maven-install
    grails maven-deploy
    grails publish-plugin

The first of these simply generates a pom.xml for your project's artifact, be it an application
WAR or a packaged plugin. The second command installs the artifact into the local Maven cache.
The third one deploys the package to a remote Maven-compatible repository. The last command will
publish a plugin to the Grails central plugin repository or another repository of your choice.
The publication process includes notification of a "plugin portal" (such as http://grails.org/plugins/)
that the plugin has been released.

For more information, check out the [user guide](http://grails-plugins.github.com/grails-release/docs/).
