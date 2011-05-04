eventInitScm = { baseDir, interactive ->
    scmProvider = classLoader.loadClass("grails.plugin.release.test.DryRunScmProvider").newInstance(basediri, interactive)
}
