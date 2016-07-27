import unittest

class TestContext:
    def __init__(self, engine_directory, data_directory, es_ip='0.0.0.0', es_port=7070):
        self.engine_directory = engine_directory
        self.data_directory = data_directory
        self.es_ip = es_ip
        self.es_port = es_port

def for_context(cls, test_context):
    cls.test_context = test_context
    return cls

# The base class for all the tests cases requiring eventserver
class BaseTestCase(unittest.TestCase):

    def __init__(self, test_context, methodName='runTest'):
        super(BaseTestCase, self).__init__(methodName)
        self.test_context = test_context

class AppContext:
    def __init__(self, name, template, engine_json_path=None):
        self.name = name
        self.template = template
        self.engine_json_path = engine_json_path
