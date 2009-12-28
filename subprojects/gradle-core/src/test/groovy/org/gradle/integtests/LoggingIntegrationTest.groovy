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

package org.gradle.integtests

import static org.junit.Assert.*
import static org.hamcrest.Matchers.*
import static org.gradle.util.Matchers.*
import org.junit.runner.RunWith
import org.junit.Test

/**
 * @author Hans Dockter
 */
// todo To make this test stronger, we should check against the output of a file appender. Right now GradleLauncher does not provided this easily but eventually will.
@RunWith(DistributionIntegrationTestRunner.class)
class LoggingIntegrationTest {
    // Injected by test runner
    private GradleDistribution dist;
    private GradleExecuter executer;

    List quietMessages = [
            'An info log message which is always logged.',
            'A message which is logged at QUIET level',
            'A task message which is logged at QUIET level',
            'quietProject2Out',
            'quietProject2ScriptClassPathOut',
            'quietProject2CallbackOut',
            'settings quiet out',
            'init quiet out',
            'init callback quiet out',
            'buildSrc quiet',
            'nestedBuild/buildSrc quiet',
            'nestedBuild quiet',
            'nestedBuild task quiet',
            'external quiet message'
    ]
    List errorMessages = [
            'An error log message.'
    ]
    List warningMessages = [
            'A warning log message.'
    ]
    List lifecycleMessages = [
            'A lifecycle info log message.',
            '[ant:echo] An info message logged from Ant',
            'A task message which is logged at LIFECYCLE level',
            'settings lifecycle log',
            'init lifecycle log',
            'external lifecycle message',
            'LOGGER: evaluating :',
            'LOGGER: evaluating :project1',
            'LOGGER: evaluating :project2',
            'LOGGER: executing :project1:logInfo',
            'LOGGER: executing :project1:logLifecycle',
            'LOGGER: executing :project1:nestedBuildLog',
            'LOGGER: executing :project1:log',
            ':buildSrc:classes',
            ':nestedBuild:log'
    ]
    List infoMessages = [
            'An info log message.',
            'A message which is logged at INFO level',
            'A task message which is logged at INFO level',
            'An info log message logged using SLF4j',
            'An info log message logged using JCL',
            'An info log message logged using Log4j',
            'An info log message logged using JUL',
            'infoProject2ScriptClassPathOut',
            'settings info out',
            'settings info log',
            'init info out',
            'init info log',
            'LOGGER: build finished',
            'LOGGER: evaluated project',
            'LOGGER: executed task',
            'LOGGER: task starting work',
            'LOGGER: task completed work',
            'buildSrc info',
            'nestedBuild/buildSrc info',
            'nestedBuild info',
            'external info message'
    ]
    List debugMessages = [
            'A debug log message.'
    ]
    List traceMessages = [
            'A trace log message.'
    ]
    List forbiddenMessages = [
            // the default message generated by JUL
            'INFO: An info log message logged using JUL',
            // the custom logger should override this
            'BUILD SUCCESSFUL'
    ]
    List allOuts = [
            errorMessages,
            quietMessages,
            warningMessages,
            lifecycleMessages,
            infoMessages,
            debugMessages,
            traceMessages,
            forbiddenMessages
    ]

    LogLevel quiet = new LogLevel(
            args: ['-q'],
            includeMessages: [quietMessages, errorMessages]
    )
    LogLevel lifecycle = new LogLevel(
            args: [],
            includeMessages: [quietMessages, errorMessages, warningMessages, lifecycleMessages]
    )
    LogLevel info = new LogLevel(
            args: ['-i'],
            includeMessages: [quietMessages, errorMessages, warningMessages, lifecycleMessages, infoMessages]
    )
    LogLevel debug = new LogLevel(
            args: ['-d'],
            includeMessages: [quietMessages, errorMessages, warningMessages, lifecycleMessages, infoMessages, debugMessages],
            matchPartialLine: true
    )

    @Test
    public void loggingSamples() {
        checkOutput(quiet)
        checkOutput(lifecycle)
        checkOutput(info)
        checkOutput(debug)
    }

    void checkOutput(LogLevel level) {
        TestFile loggingDir = dist.samplesDir.file('logging')
        loggingDir.file("buildSrc/build").deleteDir()
        loggingDir.file("nestedBuild/buildSrc/build").deleteDir()

        String initScript = new File(loggingDir, 'init.gradle').absolutePath
        String[] allArgs = level.args + ['-I', initScript]

        ExecutionResult result = executer.inDirectory(loggingDir).withArguments(allArgs).withTasks('log').run()
        level.includeMessages.each {List messages ->
            if (messages == errorMessages) {
                checkOuts(true, result.error, messages, level.matchPartialLine)
            }
            else {
                checkOuts(true, result.output, messages, level.matchPartialLine)
            }
        }
        (allOuts- level.includeMessages).each {List messages ->
            checkOuts(false, result.output, messages, true)
            checkOuts(false, result.error, messages, true)
        }
    }

    void checkOuts(boolean shouldContain, String result, List outs, boolean partialLine) {
        outs.each {String expectedOut ->
            def matcher = partialLine ? containsLine(containsString(expectedOut)) : containsLine(expectedOut)
            if (!shouldContain) {
                matcher = not(matcher)
            }
            assertThat(result, matcher)
        }
    }
}

class LogLevel {
    List args
    List includeMessages
    boolean matchPartialLine
}