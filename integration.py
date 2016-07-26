import unittest

class TestContext:
    def __init__(self, engine_directory, data_directory, es_ip, es_port):
        self.engine_directory = engine_directory
        self.data_directory = data_directory
        self.es_ip = es_ip
        self.es_port = es_port

# The base class for all the tests cases requiring eventserver
class BaseTestCase(unittest.TestCase):

    def __init__(self, methodName='runTest', test_context):
        super(ParametrizedTestCase, self).__init__(methodName)
        self.test_context = test_context

    @classmethod
    def setUpClass(cls):
        # start the eventserver
        pass

    @classmethod
    def tearDownClass(cls):
        # stop the eventserver
        pass
