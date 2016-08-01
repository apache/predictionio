import unittest
import logging
import pio_tests.globals as globals

class TestContext:
  """Class representing the settings provided for every test"""

  def __init__(self, engine_directory, data_directory, es_ip='0.0.0.0', es_port=7070):
    """
    Args:
      engine_directory (str): path to the directory where the engines are stored
      data_directory (str):   path to the directory where tests can keep their data
      es_ip (str):            ip of the eventserver
      es_port (int):          port of the eventserver
    """
    self.engine_directory = engine_directory
    self.data_directory = data_directory
    self.es_ip = es_ip
    self.es_port = es_port

class BaseTestCase(unittest.TestCase):
  """This is the base class for all integration tests

  This class sets up a `TestContext` object and a logger for every test case
  """
  def __init__(self, test_context, methodName='runTest'):
    super(BaseTestCase, self).__init__(methodName)
    self.test_context = test_context
    self.log = logging.getLogger(globals.LOGGER_NAME)

class AppContext:
  """ This class is a description of an instance of the engine"""

  def __init__(self, name, template, engine_json_path=None):
    """
    Args:
      name (str): application name
      template (str): either the name of an engine from the engines directory
          or a link to repository with the engine
      engine_json_path (str): path to json file describing an engine (a custom engine.json)
          to be used for the application. If `None`, engine.json from the engine's directory
          will be used
    """
    self.name = name
    self.template = template
    self.engine_json_path = engine_json_path
