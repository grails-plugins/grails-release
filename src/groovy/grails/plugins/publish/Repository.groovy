package grails.plugins.publish

/**
 * Simple class representing a remote repository that plugins and applications
 * can be published to.
 */
@Immutable
class Repository {
	/** The name of this repository. */
	String name

	/** The root URI to publish to. */
	URI uri

	/** The root location of the default portal to ping when publishing to this repository. */
	URI defaultPortal

	/** The standard Grails Central repository. */
	static final Repository grailsCentral = new Repository(
			"grailsCentral",
			new URI("https://svn.codehaus.org/grails-plugins"),
			new URI("http://grails.org/plugin"))
}
