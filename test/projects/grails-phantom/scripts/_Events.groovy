eventPublishPluginStart = { pluginInfo ->
    event "StatusUpdate", ["Starting to publish plugin: $pluginInfo"]
}

eventPublishPluginEnd = { pluginInfo ->
    event "StatusUpdate", ["Finished publishing plugin: $pluginInfo"]
}

eventDeployPluginStart = { pluginInfo, pluginZip, pomFile ->
    event "StatusUpdate", ["Starting to deploy plugin: $pluginInfo, $pluginZip, $pomFile"]
}

eventDeployPluginEnd = { pluginInfo, pluginZip, pomFile ->
    event "StatusUpdate", ["Finished deploying plugin: $pluginInfo, $pluginZip, $pomFile"]
}

eventPingPortalStart = { pluginInfo, portalUrl, repoUrl ->
    event "StatusUpdate", ["Starting to ping plugin portal: $pluginInfo, $portalUrl, $repoUrl"]
}

eventPingPortalEnd = { pluginInfo, portalUrl, repoUrl ->
    event "StatusUpdate", ["Finished pinging plugin portal: $pluginInfo, $portalUrl, $repoUrl"]
}
