Stanbol enhancer integration tests
----------------------------------
The tests run automatically during the test phase (not integration-test, as
they are testing another module), start the runnable jar of the full launcher
and test it over HTTP.

To create or debug tests you can also keep the launcher running and run 
individual tests against it, as follows:

  $ cd integration-tests
  $ mvn -o clean install -DkeepJarRunning=true
  
The launcher starts and keeps running until you press CTRL-C.

Note the port number on which it is running, console says something like
"HTTP server port: 54530". 
  
In a different console, you can now run individual tests:

  $ mvn -o test -Dtest.server.url=http://localhost:54530 -Dtest=StatelessEngineTest
  
Or do the same from your IDE, see the POM for system properties that the tests
expect.

You can also run the tests in the same way against any other instance, 
of course.
