/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.configuration;

import org.gradle.api.internal.artifacts.dsl.BuildScriptClasspathScriptTransformer;
import org.gradle.api.internal.artifacts.dsl.BuildScriptTransformer;
import org.gradle.api.internal.initialization.ScriptClassLoaderProvider;
import org.gradle.api.internal.initialization.ScriptHandlerFactory;
import org.gradle.api.internal.initialization.ScriptHandlerInternal;
import org.gradle.api.internal.project.*;
import org.gradle.groovy.scripts.*;

public class DefaultScriptObjectConfigurerFactory implements ScriptObjectConfigurerFactory {
    private final ScriptCompilerFactory scriptCompilerFactory;
    private final ImportsReader importsReader;
    private final ScriptHandlerFactory scriptHandlerFactory;
    private final ClassLoader defaultClassLoader;

    public DefaultScriptObjectConfigurerFactory(ScriptCompilerFactory scriptCompilerFactory,
                                                ImportsReader importsReader,
                                                ScriptHandlerFactory scriptHandlerFactory,
                                                ClassLoader defaultClassLoader) {
        this.scriptCompilerFactory = scriptCompilerFactory;
        this.importsReader = importsReader;
        this.scriptHandlerFactory = scriptHandlerFactory;
        this.defaultClassLoader = defaultClassLoader;
    }

    public ScriptObjectConfigurer create(ScriptSource scriptSource) {
        return new ScriptObjectConfigurerImpl(scriptSource);
    }

    private class ScriptObjectConfigurerImpl implements ScriptObjectConfigurer {
        private final ScriptSource scriptSource;
        private String classpathClosureName = "buildscript";
        private Class<? extends BasicScript> scriptType = DefaultScript.class;
        private ScriptClassLoaderProvider classLoaderProvider;
        private ClassLoader classLoader = defaultClassLoader;

        public ScriptObjectConfigurerImpl(ScriptSource scriptSource) {
            this.scriptSource = scriptSource;
        }

        public ScriptSource getSource() {
            return scriptSource;
        }

        public ScriptObjectConfigurer setClasspathClosureName(String name) {
            this.classpathClosureName = name;
            return this;
        }

        public ScriptObjectConfigurer setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public ScriptObjectConfigurer setClassLoaderProvider(ScriptClassLoaderProvider classLoaderProvider) {
            this.classLoaderProvider = classLoaderProvider;
            return this;
        }

        public ScriptObjectConfigurer setScriptBaseClass(Class<? extends BasicScript> type) {
            scriptType = type;
            return this;
        }

        public void apply(Object target) {
            DefaultServiceRegistry services = new DefaultServiceRegistry();
            services.add(ScriptObjectConfigurerFactory.class, DefaultScriptObjectConfigurerFactory.this);
            services.add(StandardOutputRedirector.class, new DefaultStandardOutputRedirector());

            ScriptAware scriptAware = null;
            if (target instanceof ScriptAware) {
                scriptAware = (ScriptAware) target;
                scriptAware.beforeCompile(this);
            }
            ScriptClassLoaderProvider classLoaderProvider = this.classLoaderProvider;

            if (classLoaderProvider == null) {
                ScriptHandlerInternal defaultScriptHandler = scriptHandlerFactory.create(classLoader);
                services.add(ScriptHandlerInternal.class, defaultScriptHandler);
                classLoaderProvider = defaultScriptHandler;
            }
            
            ScriptSource withImports = new ImportsScriptSource(scriptSource, importsReader, null);
            ScriptCompiler compiler = scriptCompilerFactory.createCompiler(withImports);

            compiler.setClassloader(classLoaderProvider.getClassLoader());

            BuildScriptClasspathScriptTransformer classpathScriptTransformer
                    = new BuildScriptClasspathScriptTransformer(classpathClosureName);
            compiler.setTransformer(classpathScriptTransformer);
            ScriptRunner<? extends BasicScript> classPathScript = compiler.compile(scriptType);
            setDelegate(classPathScript, target, services);

            classPathScript.run();
            classLoaderProvider.updateClassPath();

            compiler.setTransformer(new BuildScriptTransformer(classpathScriptTransformer));
            ScriptRunner<? extends BasicScript> runner = compiler.compile(scriptType);
            setDelegate(runner, target, services);
            if (scriptAware != null) {
                scriptAware.afterCompile(this, runner.getScript());
            }

            runner.run();
        }

        private void setDelegate(ScriptRunner<? extends BasicScript> scriptRunner, Object target, ServiceRegistry services) {
            scriptRunner.getScript().init(target, services);
        }
    }
}