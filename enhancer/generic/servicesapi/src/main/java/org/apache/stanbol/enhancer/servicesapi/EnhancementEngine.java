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

/**
 * Interface to internal or external semantic enhancement engines. There will
 * usually be several of those, that the EnhancementJobManager uses to enhance
 * content items.
 */
public interface EnhancementEngine {

    /**
     * Return value for {@link #canEnhance}, meaning this engine cannot enhance
     * supplied {@link ContentItem}
     */
    int CANNOT_ENHANCE = 0;

    /**
     * Return value for {@link #canEnhance}, meaning this engine can enhance
     * supplied {@link ContentItem}, and suggests enhancing it synchronously
     * instead of queuing a request for enhancement.
     */
    int ENHANCE_SYNCHRONOUS = 1;

    /**
     * Return value for {@link #canEnhance}, meaning this engine can enhance
     * supplied {@link ContentItem}, and suggests queuing a request for
     * enhancement instead of enhancing it synchronously.
     */
    int ENHANCE_ASYNC = 1;

    /**
     * Indicate if this engine can enhance supplied ContentItem, and if it
     * suggests enhancing it synchronously or asynchronously. The
     * {@link EnhancementJobManager} can force sync/async mode if desired, it is
     * just a suggestion from the engine.
     *
     * @throws EngineException if the introspecting process of the content item
     *             fails
     */
    int canEnhance(ContentItem ci) throws EngineException;

    /**
     * Compute enhancements for supplied ContentItem. The results of the process
     * are expected to be stored in the metadata of the content item.
     *
     * The client (usually an {@link EnhancementJobManager}) should take care of
     * persistent storage of the enhanced {@link ContentItem}.
     *
     * @throws EngineException if the underlying process failed to work as
     *             expected
     */
    void computeEnhancements(ContentItem ci) throws EngineException;

}
