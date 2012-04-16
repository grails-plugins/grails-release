/*
 * Copyright 2011 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugins.publish

import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.apache.ivy.core.event.EventManager
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveEngine
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.resolve.ResolvedModuleRevision
import org.apache.ivy.core.sort.SortEngine

/**
 * Resolves the excludes for all of the dependencies in an application. The reason for this
 * class' existence is that resolve engines like Ivy don't support transitive=false declaration
 * at the POM level. So we need to transform a transitive=false declaration into a set of
 * explicit exclusions when generating the POM file
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class ExcludeResolver {

    IvyDependencyManager dependencyManager

    ExcludeResolver(IvyDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager
    }

    Map<ModuleRevisionId, List<ModuleId>> resolveExcludes() {
        def results = [:]
        def applicationDescriptors = dependencyManager.getApplicationDependencyDescriptors()

        def eventManager = new EventManager()
        def sortEngine = new SortEngine(dependencyManager.ivySettings)
        def resolveEngine = new ResolveEngine(dependencyManager.ivySettings, eventManager,sortEngine)
        resolveEngine.dictatorResolver = dependencyManager.chainResolver

        def md = dependencyManager.createModuleDescriptor()
        def directModulesId = []
        for (d in applicationDescriptors) {
            // the dependency without any exclude/transitive definitions
            directModulesId << d.dependencyId
            def newDescriptor = new DefaultDependencyDescriptor(d.getDependencyRevisionId(), false)
            newDescriptor.addDependencyConfiguration(d.scope, "default")
            md.addDependency newDescriptor
        }

        if (directModulesId) {
            def options = new ResolveOptions(download:false, outputReport:false, validate:false)
            def report = resolveEngine.resolve(md, options)
            for (dep in report.dependencies) {
                def dependencyModuleId = dep.moduleId
                if (!directModulesId.contains(dependencyModuleId)) {
                    continue
                }

                def depDescriptor = dep.descriptor
                def transitiveDepList = []
                if (dep.moduleRevision == null) {
                    continue
                }

                results[dep.moduleRevision.id] = transitiveDepList
                for (transitive in depDescriptor.dependencies) {
                    def tdid = transitive.dependencyId
                    if (tdid instanceof ResolvedModuleRevision) {
                        transitiveDepList << tdid.id
                    }
                    else {
                        transitiveDepList << tdid
                    }
                }
            }
        }

        return results
    }
}
