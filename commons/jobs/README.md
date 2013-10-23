<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

Jobs
==================

The Stanbol Commons Jobs provide a shared API and rest service for running 
and monitoring long term operations.

This module includes the following:

* /api, API and implementation of the core system
* /web, a RESTful service to ping created jobs, monitoring status and getting the pointer to the job result location when ready


REST interface
------------------

The REST endpoint is available at /jobs.

It supports the following:

- GET /jobs *The home page of the Jobs service.*
- GET /jobs/*job-identifier>* *Returns response status code 200 if the job exists, 404 otherwise. The response includes the status (running|finished) 
and the location of the output. This service support the following formats: text/html, application/json and text/plain* 
- DELETE /jobs/*job-identifier* *Interrupts the job if it's running, then deletes it*
- DELETE /jobs *Interrupts and deletes all jobs*

Job output handling is demanded to the service which created the job.


Creating a job
------------------

Stanbol services can create a job accessing the `org.apache.stanbol.commons.jobs.api.JobManager` SCR component, 
by providing an implementation of the `org.apache.stanbol.commons.jobs.api.Job` interface and obtaining the job identifier.
The service **must** then return an HTTP response with status `201` including the 
`Location` HTTP header pointing to the created job. It **should** provide a human readable location of the job 
resource in the body of the response.

The job resource location can be
derived by attaching the job identifier (retrieved by the manager) to the base *stanbol-base-url/*jobs/ resource path.

For example:

* Status: `201 Created`
* Location: `http://localhost:8080/jobs/1234`

The `org.apache.stanbol.commons.jobs.web.resources.JobsResource` class in /web includes a method to create a 'test' job, 
it behaves as described above and returns a text/plain description.
It can be used as a reference implementation.


Obtaining the job results
------------------

A request to /jobs/*jobs-identifier* will include info about the status of the job and the location of the result output. 
This service supports application/json response type. 
The JSON object includes the following properties:

- status *can be running|finished*
- location *point to the location of the result*
- messages *can include a list of messages*


Testing the service with a dummy job
------------------
You can create a test job by invoking:

      http://localhost:8080/jobs/test
 
which will create a dummy job, returning response code `201` and `Location` header pointing to the job resource.
You can then follow the `Location` to check the status of the job. When it is `finished`, you can follow the Location provided
to see the result.

