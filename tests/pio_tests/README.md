# PredictionIO - Integration Tests

This python module introduces a basic framework for adding integration tests to
PredictionIO. It is nothing more than a collection of utility functions mostly being wrappers
over shell executed commands.

### Prerequisites
In order to execute tests, besides a configured **PredictionIO** environment one
has to download the following python-3 packages:
* requests
* unittest
* xmlrunner

### Execution
*tests.py* - the executable script. Launches eventserver to be available for the tests.
You can pass it arguments to:
* suppress the output of executed shell commands within the tests
* enable logging
* specify which tests should be exectued (by names)

For more information run:
```shell
python3 tests.py -h
```

As soon as the tests are finishied an XML file with JUnit-like test reports 
is created in the directory of execution.

### Adding new tests
Every test should be an instance of **pio_tests.integration.BaseTestCase** defined in **pio_tests.integration**.  
Upon creation, a **pio_tests.integration.TestContext**  object is provided to it with description of:
* ip address and a port of running eventserver
* directories containing stored engines and data for specific tests

Every test should be registered in the appropriate place in *tests.py* file, whereas
its definition should reside in **pio_tests.scenarios** module. If the test requires some additional files
during the execution, you should put them under *data* directory mentioned above.

The best way to test different application engines is to make use of **pio_tests.utility.AppEngine**.
Apart from containing utility functions, it downloads engine templates if necessary.

To see an example of implemented test check **pio_tests.scenarios.quickstart_test**, which is
a repetition of the QuickStart tutorial from the doc site.
