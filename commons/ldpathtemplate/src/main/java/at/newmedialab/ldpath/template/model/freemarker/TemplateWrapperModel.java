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

import freemarker.template.AdapterTemplateModel;

/**
 * Add file description here!
 * <p/>
 * Author: Sebastian Schaffert
 */
public class TemplateWrapperModel<T> implements AdapterTemplateModel {

    private T object;

    public TemplateWrapperModel(T object) {
        this.object = object;
    }

    /**
     * Retrieves the underlying object, or some other object semantically
     * equivalent to its value narrowed by the class hint.
     *
     * @param hint the desired class of the returned value. An implementation
     *             should make reasonable effort to retrieve an object of the requested
     *             class, but if that is impossible, it must at least return the underlying
     *             object as-is. As a minimal requirement, an implementation must always
     *             return the exact underlying object when
     *             <tt>hint.isInstance(underlyingObject) == true</tt> holds. When called
     *             with <tt>java.lang.Object.class</tt>, it should return a generic Java
     *             object (i.e. if the model is wrapping a scripting lanugage object that is
     *             further wrapping a Java object, the deepest underlying Java object should
     *             be returned).
     * @return the underlying object, or its value accommodated for the hint
     *         class.
     */
    @Override
    public T getAdaptedObject(Class hint) {
        return object;
    }
}
