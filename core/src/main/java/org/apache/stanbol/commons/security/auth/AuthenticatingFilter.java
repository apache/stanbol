/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stanbol.commons.security.auth;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.security.auth.Subject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.security.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service(Filter.class)
@Properties(value = {
    @Property(name = "pattern", value = ".*"),
    @Property(name = "service.ranking", intValue = Integer.MAX_VALUE)
})
@Reference(name = "weightedAuthenticationMethod",
        cardinality = ReferenceCardinality.MANDATORY_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        referenceInterface = WeightedAuthenticationMethod.class)
public class AuthenticatingFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(AuthenticatingFilter.class);
    private SortedSet<WeightedAuthenticationMethod> methodList =
            new TreeSet<WeightedAuthenticationMethod>(new WeightedAuthMethodComparator());

    private Subject getSubject() {
        Subject subject = UserUtil.getCurrentSubject();
        if (subject == null) {
            subject = new Subject();
        }
        return subject;
    }

    /**
     * Registers a
     * <code>WeightedAuthenticationMethod</code>
     *
     * @param method the method to be registered
     */
    protected void bindWeightedAuthenticationMethod(WeightedAuthenticationMethod method) {
        methodList.add(method);
    }

    /**
     * Unregister a
     * <code>WeightedAuthenticationMethod</code>
     *
     * @param method the method to be unregistered
     */
    protected void unbindWeightedAuthenticationMethod(WeightedAuthenticationMethod method) {
        methodList.remove(method);
    }

    /**
     * Compares the WeightedAuthenticationMethods, descending for weight and
     * ascending by name
     */
    static class WeightedAuthMethodComparator
            implements Comparator<WeightedAuthenticationMethod> {

        @Override
        public int compare(WeightedAuthenticationMethod o1,
                WeightedAuthenticationMethod o2) {
            int o1Weight = o1.getWeight();
            int o2Weight = o2.getWeight();
            if (o1Weight != o2Weight) {
                return o2Weight - o1Weight;
            }
            return o1.getClass().toString().compareTo(o2.getClass().toString());
        }
    }

    private void writeLoginResponse(final HttpServletRequest request, final HttpServletResponse response, Throwable e) throws ServletException, IOException {
        logger.debug("SecurityException: {}", e);
        try {
            for (AuthenticationMethod authMethod : methodList) {
                if (authMethod.writeLoginResponse(request, response, e)) {
                    break;
                }
            }
        } catch (IOException ex) {
            //only needed because jetty is doing a bad job at logging
            logger.error("Exception writing loging respone", e);
            throw ex;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
            final FilterChain chain) throws IOException, ServletException {

        logger.debug("filtering request");
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        final Subject subject = getSubject();
        {
            AuthenticationMethod authenticationMethod = null;
            try {
                for (Iterator<WeightedAuthenticationMethod> it = methodList.iterator(); it.hasNext();) {
                    authenticationMethod = it.next();
                    if (authenticationMethod.authenticate(request, subject)) {
                        break;
                    }
                }
            } catch (LoginException ex) {
                if (!authenticationMethod.writeLoginResponse(request, response, ex)) {
                    writeLoginResponse(request, response, ex);
                }
                return;
            }
        }

        Set<Principal> principals = subject.getPrincipals();
        if (principals.size() == 0) {
            principals.add(UserUtil.ANONYMOUS);
        }
        //the response wrapping is because of JERSEY-1926
        final ServletOutputStream[] out = new ServletOutputStream[1];
        final boolean[] closed = new boolean[1];
        final String[] sentErrorMsg = new String[1];
        final int[] sentErrorCode = new int[1];
        sentErrorCode[0] = -1;
        try {
            Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    HttpServletResponse wrapped = new HttpServletResponseWrapper(response) {
                        @Override
                        public ServletOutputStream getOutputStream() throws IOException {
                            final ServletOutputStream orig =  response.getOutputStream();
                            out[0] = orig;
                            return new ServletOutputStream() {

                                @Override
                                public void write(int i) throws IOException {
                                    orig.write(i);
                                }

                                @Override
                                public void close() throws IOException {
                                    closed[0] = true;
                                }
                                
                            };
                        }


                        @Override
                        public void sendError(int sc, String msg) throws IOException {
                            sentErrorCode[0] = sc;
                            sentErrorMsg[0] = msg;
                        }
                        
                    };
                    chain.doFilter(request, wrapped);
                    return null;
                }
            }, null);

        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServletException) {
                if (cause.getCause() instanceof SecurityException) {
                    //working around JERSEY-1926
                    writeLoginResponse(request, response,
                            cause.getCause());
                } else {
                    throw (ServletException) cause;
                }
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            writeLoginResponse(request, response, e);
        }
        if (!response.isCommitted()) {
            if (closed[0]) {
                try {
                    out[0].close();
                } catch (IOException e) {
                    //do nothing
                }
            }
            if (sentErrorCode[0] > -1) {
                response.sendError(sentErrorCode[0], sentErrorMsg[0]);
            }
        }

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }
}
