import os
import sys
import unittest
from pio_tests.integration import TestContext
from pio_tests.apps_tests.quickstart_test import QuickStartTest

TESTS_DIRECTORY = os.path.dirname(__file__)
ENGINE_DIRECTORY = os.path.join(TESTS_DIRECTORY, "engines")
DATA_DIRECTORY = os.path.join(TESTS_DIRECTORY, "data")

def get_apps_test_suite():
    test_context = TestContext(ENGINE_DIRECTORY, DATA_DIRECTORY)
    suite = unittest.TestSuite()

    # ADD APPS TESTS HERE
    suite.addTest(QuickStartTest(test_context))

    return suite

def get_test_suite():
    apps_suite = get_apps_test_suite()
    return unittest.TestSuite([apps_suite])

if __name__ == "__main__":
    result = unittest.TextTestRunner(verbosity=2).run(get_test_suite())

    if not result.wasSuccessful():
        sys.exit(1)
