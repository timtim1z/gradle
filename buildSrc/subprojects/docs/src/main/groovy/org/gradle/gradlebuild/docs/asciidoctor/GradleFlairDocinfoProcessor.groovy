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
import org.asciidoctor.extension.DocinfoProcessor;

class GradleFlairDocinfoProcessor extends DocinfoProcessor {
    private static final String COMMON_HEAD_HTML = "head-meta.html";

    @Override
    public String process(Document document) {
        return Thread.currentThread().contextClassLoader.getResource(COMMON_HEAD_HTML).text;
    }
}
