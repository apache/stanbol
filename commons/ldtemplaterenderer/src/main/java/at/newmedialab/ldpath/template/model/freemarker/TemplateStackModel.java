/*
 * Copyright (c) 2011 Salzburg Research.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.newmedialab.ldpath.template.model.freemarker;

import freemarker.template.TemplateModel;

import java.util.Stack;

/**
 * Add file description here!
 * <p/>
 * Author: Sebastian Schaffert
 */
public class TemplateStackModel implements TemplateModel {

    private Stack<TemplateModel> stack;

    public TemplateStackModel() {
        stack = new Stack<TemplateModel>();
    }


    public TemplateModel push(TemplateModel item) {
        return stack.push(item);
    }

    public TemplateModel pop() {
        return stack.pop();
    }

    public TemplateModel peek() {
        return stack.peek();
    }

    public boolean empty() {
        return stack.empty();
    }
}
