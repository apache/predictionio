import os
import sys
import unittest
import argparse
from subprocess import Popen
from pio_tests.integration import TestContext
from pio_tests.apps_tests.quickstart_test import QuickStartTest

parser = argparse.ArgumentParser(description='Integration tests for PredictionIO')
parser.add_argument('--eventserver-ip', default='0.0.0.0')
parser.add_argument('--eventserver-port', type=int, default=7070)

TESTS_DIRECTORY = os.path.abspath(os.path.dirname(__file__))
ENGINE_DIRECTORY = os.path.join(TESTS_DIRECTORY, "engines")
DATA_DIRECTORY = os.path.join(TESTS_DIRECTORY, "data")

def get_apps_test_suite(test_context):
    suite = unittest.TestSuite()
    # ADD APPS TESTS HERE
    suite.addTest(QuickStartTest(test_context))

    return suite

def get_test_suite(test_context):
    apps_suite = get_apps_test_suite(test_context)
    return unittest.TestSuite([apps_suite])

# usage: tests.py [eventserver-ip] [eventserver-port]
if __name__ == "__main__":
    args = vars(parser.parse_args())

    test_context = None
    test_context = TestContext(
            ENGINE_DIRECTORY, DATA_DIRECTORY, args['eventserver_ip'], int(args['eventserver_port']))

    event_server_process = Popen('pio eventserver --ip {} --port {}'
            .format(test_context.es_ip, test_context.es_port),
            shell=True)
    result = unittest.TextTestRunner(verbosity=2).run(get_test_suite(test_context))
    event_server_process.kill()

    if not result.wasSuccessful():
        sys.exit(1)
