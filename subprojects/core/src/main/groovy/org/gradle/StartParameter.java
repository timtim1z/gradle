/*
 * Copyright 2010 the original author or authors.
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

package org.gradle;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.gradle.internal.SystemProperties;
import org.gradle.logging.LoggingConfiguration;
import org.gradle.util.GFileUtils;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * <p>{@code StartParameter} defines the configuration used by a {@link GradleLauncher} instance to execute a build. The properties of {@code StartParameter} generally correspond to the command-line
 * options of Gradle. You pass a {@code StartParameter} instance to {@link GradleLauncher#newInstance(StartParameter)} when you create a new {@code Gradle} instance.</p>
 *
 * <p>You can obtain an instance of a {@code StartParameter} by either creating a new one, or duplicating an existing one using {@link #newInstance} or {@link #newBuild}.</p>
 *
 * @author Hans Dockter
 * @see GradleLauncher
 */
public class StartParameter extends LoggingConfiguration implements Serializable {
    public static final String GRADLE_USER_HOME_PROPERTY_KEY = "gradle.user.home";
    /**
     * The default user home directory.
     */
    public static final File DEFAULT_GRADLE_USER_HOME = new File(SystemProperties.getUserHome() + "/.gradle");

    private List<String> taskNames = new ArrayList<String>();
    private Set<String> excludedTaskNames = new HashSet<String>();
    private boolean buildProjectDependencies = true;
    private File currentDir;
    private File projectDir;
    private boolean searchUpwards = true;
    private Map<String, String> projectProperties = new HashMap<String, String>();
    private Map<String, String> systemPropertiesArgs = new HashMap<String, String>();
    private File gradleUserHomeDir;
    private CacheUsage cacheUsage = CacheUsage.ON;
    private File settingsFile;
    private boolean useEmptySettings;
    private File buildFile;
    private List<File> initScripts = new ArrayList<File>();
    private boolean dryRun;
    private boolean noOpt;
    private boolean rerunTasks;
    private boolean profile;
    private boolean continueOnFailure;
    private boolean offline;
    private RefreshOptions refreshOptions = RefreshOptions.NONE;
    private File projectCacheDir;
    private boolean refreshDependencies;
    private boolean recompileScripts;

    /**
     * Sets the project's cache location. Set to null to use the default location.
     */
    public void setProjectCacheDir(File projectCacheDir) {
        this.projectCacheDir = projectCacheDir;
    }

    /**
     * Returns the project's cache dir.
     *
     * @return project's cache dir, or null if the default location is to be used.
     */
    public File getProjectCacheDir() {
        return projectCacheDir;
    }

    /**
     * Creates a {@code StartParameter} with default values. This is roughly equivalent to running Gradle on the command-line with no arguments.
     */
    public StartParameter() {
        String gradleUserHome = System.getProperty(GRADLE_USER_HOME_PROPERTY_KEY);
        if (gradleUserHome == null) {
            gradleUserHome = System.getenv("GRADLE_USER_HOME");
            if (gradleUserHome == null) {
                gradleUserHome = DEFAULT_GRADLE_USER_HOME.getAbsolutePath();
            }
        }

        gradleUserHomeDir = GFileUtils.canonicalise(new File(gradleUserHome));
        setCurrentDir(null);
    }

    /**
     * Duplicates this {@code StartParameter} instance.
     *
     * @return the new parameters.
     */
    public StartParameter newInstance() {
        StartParameter startParameter = new StartParameter();
        startParameter.buildFile = buildFile;
        startParameter.projectDir = projectDir;
        startParameter.settingsFile = settingsFile;
        startParameter.useEmptySettings = useEmptySettings;
        startParameter.taskNames = taskNames;
        startParameter.buildProjectDependencies = buildProjectDependencies;
        startParameter.currentDir = currentDir;
        startParameter.searchUpwards = searchUpwards;
        startParameter.projectProperties = projectProperties;
        startParameter.systemPropertiesArgs = systemPropertiesArgs;
        startParameter.gradleUserHomeDir = gradleUserHomeDir;
        startParameter.cacheUsage = cacheUsage;
        startParameter.initScripts = new ArrayList<File>(initScripts);
        startParameter.setLogLevel(getLogLevel());
        startParameter.setColorOutput(isColorOutput());
        startParameter.setShowStacktrace(getShowStacktrace());
        startParameter.dryRun = dryRun;
        startParameter.noOpt = noOpt;
        startParameter.rerunTasks = rerunTasks;
        startParameter.recompileScripts = recompileScripts;
        startParameter.profile = profile;
        startParameter.projectCacheDir = projectCacheDir;
        startParameter.continueOnFailure = continueOnFailure;
        startParameter.offline = offline;
        startParameter.refreshOptions = refreshOptions;
        startParameter.refreshDependencies = refreshDependencies;
        return startParameter;
    }

    /**
     * <p>Creates the parameters for a new build, using these parameters as a template. Copies the environmental properties from this parameter (eg gradle user home dir, etc), but does not copy the
     * build specific properties (eg task names).</p>
     *
     * @return The new parameters.
     */
    public StartParameter newBuild() {
        StartParameter startParameter = new StartParameter();
        startParameter.gradleUserHomeDir = gradleUserHomeDir;
        startParameter.cacheUsage = cacheUsage;
        startParameter.setLogLevel(getLogLevel());
        startParameter.setColorOutput(isColorOutput());
        startParameter.setShowStacktrace(getShowStacktrace());
        startParameter.profile = profile;
        startParameter.continueOnFailure = continueOnFailure;
        startParameter.offline = offline;
        startParameter.refreshOptions = refreshOptions;
        startParameter.rerunTasks = rerunTasks;
        startParameter.recompileScripts = recompileScripts;
        startParameter.refreshDependencies = refreshDependencies;
        return startParameter;
    }

    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /**
     * Returns the build file to use to select the default project. Returns null when the build file is not used to select the default project.
     *
     * @return The build file. May be null.
     */
    public File getBuildFile() {
        return buildFile;
    }

    /**
     * Sets the build file to use to select the default project. Use null to disable selecting the default project using the build file.
     *
     * @param buildFile The build file. May be null.
     */
    public void setBuildFile(File buildFile) {
        if (buildFile == null) {
            this.buildFile = null;
            setCurrentDir(null);
        } else {
            this.buildFile = GFileUtils.canonicalise(buildFile);
            setProjectDir(this.buildFile.getParentFile());
        }
    }

    /**
     * Specifies that an empty settings script should be used.
     *
     * This means that even if a settings file exists in the conventional location, or has been previously specified by {@link #setSettingsFile(java.io.File)}, it will not be used.
     *
     * If {@link #setSettingsFile(java.io.File)} is called after this, it will supersede calling this method.
     *
     * @return this
     */
    public StartParameter useEmptySettings() {
        searchUpwards = false;
        useEmptySettings = true;
        settingsFile = null;
        return this;
    }

    /**
     * Deprecated. Use {@link #useEmptySettings()}.
     *
     * @deprecated use {@link #useEmptySettings()}
     */
    @Deprecated
    public StartParameter useEmptySettingsScript() {
        return useEmptySettings();
    }

    /**
     * Returns whether an empty settings script will be used regardless of whether one exists in the default location.
     *
     * @return Whether to use empty settings or not.
     */
    public boolean isUseEmptySettings() {
        return useEmptySettings;
    }

    /**
     * Returns the names of the tasks to execute in this build. When empty, the default tasks for the project will be executed.
     *
     * @return the names of the tasks to execute in this build. Never returns null.
     */
    public List<String> getTaskNames() {
        return taskNames;
    }

    /**
     * <p>Sets the tasks to execute in this build. Set to an empty list, or null, to execute the default tasks for the project. The tasks are executed in the order provided, subject to dependency
     * between the tasks.</p>
     *
     * @param taskNames the names of the tasks to execute in this build.
     */
    public void setTaskNames(Iterable<String> taskNames) {
        this.taskNames = Lists.newArrayList(taskNames);
    }

    /**
     * Returns the names of the tasks to be excluded from this build. When empty, no tasks are excluded from the build.
     *
     * @return The names of the excluded tasks. Returns an empty set if there are no such tasks.
     */
    public Set<String> getExcludedTaskNames() {
        return excludedTaskNames;
    }

    /**
     * Sets the tasks to exclude from this build.
     *
     * @param excludedTaskNames The task names. Can be null.
     */
    public void setExcludedTaskNames(Iterable<String> excludedTaskNames) {
        this.excludedTaskNames = Sets.newLinkedHashSet(excludedTaskNames);
    }

    /**
     * Returns the directory to use to select the default project, and to search for the settings file.
     *
     * @return The current directory. Never returns null.
     */
    public File getCurrentDir() {
        return currentDir;
    }

    /**
     * Sets the directory to use to select the default project, and to search for the settings file. Set to null to use the default current directory.
     *
     * @param currentDir The directory. Should not be null.
     */
    public void setCurrentDir(File currentDir) {
        if (currentDir != null) {
            this.currentDir = GFileUtils.canonicalise(currentDir);
        } else {
            this.currentDir = GFileUtils.canonicalise(new File(System.getProperty("user.dir")));
        }
    }

    public boolean isSearchUpwards() {
        return searchUpwards;
    }

    public void setSearchUpwards(boolean searchUpwards) {
        this.searchUpwards = searchUpwards;
    }

    public Map<String, String> getProjectProperties() {
        return projectProperties;
    }

    public void setProjectProperties(Map<String, String> projectProperties) {
        this.projectProperties = projectProperties;
    }

    public Map<String, String> getSystemPropertiesArgs() {
        return systemPropertiesArgs;
    }

    public void setSystemPropertiesArgs(Map<String, String> systemPropertiesArgs) {
        this.systemPropertiesArgs = systemPropertiesArgs;
    }

    /**
     * Returns a newly constructed map that is the JVM system properties merged with the system property args. <p> System property args take precedency overy JVM system properties.
     *
     * @return The merged system properties
     */
    public Map<String, String> getMergedSystemProperties() {
        Map<String, String> merged = new HashMap<String, String>();
        merged.putAll((Map) System.getProperties());
        merged.putAll(getSystemPropertiesArgs());
        return merged;
    }

    /**
     * Returns the directory to use as the user home directory.
     *
     * @return The home directory.
     */
    public File getGradleUserHomeDir() {
        return gradleUserHomeDir;
    }

    /**
     * Sets the directory to use as the user home directory. Set to null to use the default directory.
     *
     * @param gradleUserHomeDir The home directory. May be null.
     */
    public void setGradleUserHomeDir(File gradleUserHomeDir) {
        this.gradleUserHomeDir = gradleUserHomeDir == null ? DEFAULT_GRADLE_USER_HOME : GFileUtils.canonicalise(gradleUserHomeDir);
    }

    /**
     * Returns true if project dependencies are to be built, false if they should not be. The default is true.
     */
    public boolean isBuildProjectDependencies() {
        return buildProjectDependencies;
    }

    /**
     * Specifies whether project dependencies should be built. Defaults to true.
     *
     * @return this
     */
    public StartParameter setBuildProjectDependencies(boolean build) {
        this.buildProjectDependencies = build;
        return this;
    }

    public CacheUsage getCacheUsage() {
        return cacheUsage;
    }

    public void setCacheUsage(CacheUsage cacheUsage) {
        this.cacheUsage = cacheUsage;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isNoOpt() {
        return noOpt;
    }

    public void setNoOpt(boolean noOpt) {
        this.noOpt = noOpt;
    }

    /**
     * Sets the settings file to use for the build. Use null to use the default settings file.
     *
     * @param settingsFile The settings file to use. May be null.
     */
    public void setSettingsFile(File settingsFile) {
        if (settingsFile == null) {
            this.settingsFile = null;
        } else {
            this.useEmptySettings = false;
            this.settingsFile = GFileUtils.canonicalise(settingsFile);
            currentDir = this.settingsFile.getParentFile();
        }
    }

    /**
     * Returns the explicit settings file to use for the build, or null.
     *
     * Will return null if the default settings file is to be used. However, if {@link #isUseEmptySettings()} returns true, then no settings file at all will be used.
     *
     * @return The settings file. May be null.
     * @see #isUseEmptySettings()
     */
    public File getSettingsFile() {
        return settingsFile;
    }

    /**
     * Adds the given file to the list of init scripts that are run before the build starts.  This list is in addition to the default init scripts.
     *
     * @param initScriptFile The init scripts.
     */
    public void addInitScript(File initScriptFile) {
        initScripts.add(initScriptFile);
    }

    /**
     * Sets the list of init scripts to be run before the build starts. This list is in addition to the default init scripts.
     *
     * @param initScripts The init scripts.
     */
    public void setInitScripts(List<File> initScripts) {
        this.initScripts = initScripts;
    }

    /**
     * Returns all explicitly added init scripts that will be run before the build starts.  This list does not contain the user init script located in ${user.home}/.gradle/init.gradle, even though
     * that init script will also be run.
     *
     * @return list of all explicitly added init scripts.
     */
    public List<File> getInitScripts() {
        return Collections.unmodifiableList(initScripts);
    }

    /**
     * Sets the project directory to use to select the default project. Use null to use the default criteria for selecting the default project.
     *
     * @param projectDir The project directory. May be null.
     */
    public void setProjectDir(File projectDir) {
        if (projectDir == null) {
            setCurrentDir(null);
            this.projectDir = null;
        } else {
            File canonicalFile = GFileUtils.canonicalise(projectDir);
            currentDir = canonicalFile;
            this.projectDir = canonicalFile;
        }
    }

    /**
     * Returns the project dir to use to select the default project.
     *
     * Returns null when the build file is not used to select the default project
     *
     * @return The project dir. May be null.
     */
    public File getProjectDir() {
        return projectDir;
    }

    /**
     * Specifies if a profile report should be generated.
     *
     * @param profile true if a profile report should be generated
     */
    public void setProfile(boolean profile) {
        this.profile = profile;
    }

    /**
     * Returns true if a profile report will be generated.
     */
    public boolean isProfile() {
        return profile;
    }

    /**
     * Specifies whether the build should continue on task failure. The default is false.
     */
    public boolean isContinueOnFailure() {
        return continueOnFailure;
    }

    /**
     * Specifies whether the build should continue on task failure. The default is false.
     */
    public void setContinueOnFailure(boolean continueOnFailure) {
        this.continueOnFailure = continueOnFailure;
    }

    /**
     * Specifies whether the build should be performed offline (ie without network access).
     */
    public boolean isOffline() {
        return offline;
    }

    /**
     * Specifies whether the build should be performed offline (ie without network access).
     */
    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    /**
     * Supplies the refresh options to use for the build.
     */
    public void setRefreshOptions(RefreshOptions refreshOptions) {
        this.refreshOptions = refreshOptions;
    }

    /**
     * Returns the refresh options used for the build.
     */
    public RefreshOptions getRefreshOptions() {
        return refreshOptions;
    }

    /**
     * Specifies whether the depencendies should be refreshed..
     */
    public boolean isRefreshDependencies() {
        return refreshDependencies;
    }

    /**
     * Specifies whether the depencendies should be refreshed..
     */
    public void setRefreshDependencies(boolean refreshDependencies) {
        this.refreshDependencies = refreshDependencies;
    }

    /**
     * Specifies whether the cached task results should be ignored and each task should be forced to be executed.
     */
    public boolean isRerunTasks() {
        return rerunTasks;
    }

    /**
     * Specifies whether the cached task results should be ignored and each task should be forced to be executed.
     */
    public void setRerunTasks(boolean rerunTasks) {
        this.rerunTasks = rerunTasks;
    }

    /**
     * Specifies whether the build scripts should be recompiled.
     */
    public boolean isRecompileScripts() {
        return recompileScripts;
    }

    /**
     * Specifies whether the build scripts should be recompiled.
     */
    public void setRecompileScripts(boolean recompileScripts) {
        this.recompileScripts = recompileScripts;
    }

    @Override
    public String toString() {
        return "StartParameter{"
                + "taskNames=" + taskNames
                + ", excludedTaskNames=" + excludedTaskNames
                + ", currentDir=" + currentDir
                + ", searchUpwards=" + searchUpwards
                + ", projectProperties=" + projectProperties
                + ", systemPropertiesArgs=" + systemPropertiesArgs
                + ", gradleUserHomeDir=" + gradleUserHomeDir
                + ", cacheUsage=" + cacheUsage
                + ", logLevel=" + getLogLevel()
                + ", showStacktrace=" + getShowStacktrace()
                + ", buildFile=" + buildFile
                + ", initScripts=" + initScripts
                + ", dryRun=" + dryRun
                + ", noOpt=" + noOpt
                + ", rerunTasks=" + rerunTasks
                + ", recompileScripts=" + recompileScripts
                + ", offline=" + offline
                + ", refreshDependencies=" + refreshDependencies
                + '}';
    }
}