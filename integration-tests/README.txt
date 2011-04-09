Stanbol enhancer integration tests
----------------------------------
The tests run automatically during the test phase (not integration-test, as
they are testing another module), start the runnable jar of the full launcher
and test it over HTTP.

To create or debug tests you can also keep the launcher running and run 
individual tests against it, and use a specific HTTP port, as follows:

  $ cd integration-tests
  $ mvn -o clean install -DkeepJarRunning=true -Dhttp.port=8080
  
The launcher starts and keeps running until you press CTRL-C.

In a different console, you can now run individual tests:

  $ mvn -o test -Dtest.server.url=http://localhost:8080 -Dtest=StatelessEngineTest
  
And add -Dmaven.surefire.debug to enable debugging of the tests.

Or do the same from your IDE, see the POM for system properties that the tests
expect.

You can also run the tests in the same way against any other instance, 
of course.
