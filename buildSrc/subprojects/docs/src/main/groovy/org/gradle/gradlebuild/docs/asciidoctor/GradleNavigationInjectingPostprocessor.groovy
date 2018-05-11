/*
 * Copyright 2018 the original author or authors.
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
package org.gradle.gradlebuild.docs.asciidoctor

import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.DocumentRuby;
import org.asciidoctor.extension.Postprocessor;

/**
 * Injects static assets for docs
 */
public class GradleNavigationInjectingPostprocessor extends Postprocessor {
    private static final String COMMON_HEADER_HTML = "header.html";
    private static final String PRIMARY_NAVIGATION_HTML = "primary-navigation.html";
    private static final String USER_MANUAL_CHAPTER_META_HTML = "user-manual-chapter-meta.html";
    private static final String COMMON_FOOTER_HTML = "footer.html";

    @Override
    public String process(Document document, String output) {
        if (!document.basebackend("html")) {
            return output
        }

        String newOutput = output

        // Inject common header before page title
        newOutput = newOutput.replaceAll(~/<div id="header">/, """
${loadResourceText(COMMON_HEADER_HTML)}
<main class="main-content">
    ${loadResourceText(PRIMARY_NAVIGATION_HTML)}
    <div class="chapter">
        ${loadResourceText(USER_MANUAL_CHAPTER_META_HTML)}
        <div id="header">""")

        // Inject common footer at end of content
        newOutput = newOutput.replaceAll(~/<\/body>/, """
    </div> <!-- <end div class="chapter"> -->
    <aside class="secondary-navigation"></aside>
</main>${loadResourceText(COMMON_FOOTER_HTML)}</body>""")

        return newOutput
    }

    private String loadResourceText(final String resourcePath) {
        Thread.currentThread().contextClassLoader.getResource(resourcePath).text
    }
}
