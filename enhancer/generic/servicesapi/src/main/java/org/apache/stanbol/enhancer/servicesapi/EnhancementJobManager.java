/*
Copyright (c) 2010, IKS Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in
the documentation and/or other materials provided with the distribution.

Neither the name of the IKS Project nor the names of its contributors
may be used to endorse or promote products derived from this software
without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.
 */
package org.apache.stanbol.enhancer.servicesapi;

import java.util.List;

/**
 * Accept requests for enhancing ContentItems, and processes them either
 * synchronously or asynchronously (as decided by the enhancement engines or by
 * configuration).
 * <p>
 * The progress of the enhancement process should be made accessible in the
 * ContentItem's metadata.
 */
public interface EnhancementJobManager {

    /**
     * Create relevant asynchronous requests or enhance content immediately. The
     * result is not persisted right now. The caller is responsible for calling the
     * {@link Store#put(ContentItem)} afterwards in case persistence is
     * required.
     * <p>
     * TODO: define the expected semantics if asynchronous enhancements were to
     * get implemented.
     *
     * @throws EngineException if the enhancement process failed
     */
    void enhanceContent(ContentItem ci) throws EngineException;

    /**
     * Return the unmodifiable list of active registered engine instance that
     * can be used by the manager.
     */
    List<EnhancementEngine> getActiveEngines();

}
