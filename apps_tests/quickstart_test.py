import os
import unittest
import subprocess
import random
from pio_tests.integration import BaseTestCase, AppContext
from utils import AppEngine

def read_events(file_path):
    RATE_ACTIONS_DELIMITER = "::"
    SEED = 3
    with open(file_path, 'r') as f:
        events = []
        for line in f:
            data = line.rstrip('\r\n').split(RATE_ACTIONS_DELIMITER)
            if random.randint(0, 1) == 1:
                events.append( {
                        "event": "rate",
                        "entity_type": "user",
                        "entity_id": data[0],
                        "target_entity_type": "item",
                        "target_entity_id": data[1],
                        "properties": { "rating" : float(data[2]) } })
            else:
                events.append({
                    "event": "buy",
                    "entity_type": "user",
                    "entity_id": data[0],
                    "target_entity_type": "item",
                    "target_entity_id": data[1] })

        return events


class QuickStartTest(BaseTestCase):

    def setUp(self):
        template_path = os.path.join(
                self.test_context.engine_directory, "recommendation-engine")
        engine_json = os.path.join(
                self.test_context.data_directory, "quickstart_test/engine.json")

        self.training_data_path = os.path.join(
                self.test_context.data_directory,
                "quickstart_test/training_data.txt")

        # downloading training data
        subprocess.run('curl https://raw.githubusercontent.com/apache/spark/master/' \
                'data/mllib/sample_movielens_data.txt --create-dirs -o {0}'
                .format(self.training_data_path), shell=True)

        app_context = AppContext(
                name="MyRecommender",
                template=template_path,
                engine_json=engine_json)

        self.app = AppEngine(self.test_context, app_context)
        self.app_pid = None

    def runTest(self):
        self.app.new()

        event1 = {
            "event" : "rate",
            "entityType" : "user",
            "entityId" : "u0",
            "targetEntityType" : "item",
            "targetEntityId" : "i0",
            "properties" : {
                "rating" : 5
                },
            "eventTime" : "2014-11-02T09:39:45.618-08:00" }

        event2 = {
            "event" : "buy",
            "entityType" : "user",
            "entityId" : "u1",
            "targetEntityType" : "item",
            "targetEntityId" : "i2",
            "eventTime" : "2014-11-10T12:34:56.123-08:00" }

        self.app.send_events([event1, event2])
        stored_events = self.app.get_events()
        # TODO compare events
        new_events = read_events(self.training_data_path)
        self.app.send_events(new_events)
        stored_events = self.app.get_events()
        self.assertEquals(len(new_events) + 2, len(stored_events))

        self.app.build()
        self.app.train()
        self.app.deploy(wait_time=20)

        user_query = { "user": 1, "num": 4 }
        recommendations = self.app.query(user_query)
        # TODO compare recommendations


    def tearDown(self):
        self.app.stop()
        self.app.remove_data()
        self.app.delete()
