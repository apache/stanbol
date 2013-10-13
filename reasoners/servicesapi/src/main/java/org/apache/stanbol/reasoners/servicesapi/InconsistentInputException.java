/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.reasoners.servicesapi;


/**
 * The process cannot be completed because the input is inconsistent. This Exception must be used by reasoning
 * services which must stop the inference process if any inconsistency is found.
 */
public class InconsistentInputException extends Exception {
public InconsistentInputException() {
}
    public InconsistentInputException(Exception cause) {
        super(cause);
    }

    /**
	 * 
	 */
    private static final long serialVersionUID = 117198026192803326L;

}
