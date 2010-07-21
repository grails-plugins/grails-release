includeTargets << new File(mavenPublisherPluginDir, "scripts/_GrailsMaven.groovy")

target(default: "Generate a POM for a plugin project.") {
    depends(generatePom)
}
