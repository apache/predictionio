class AppEngine:

    def __init__(self, test_context, app_context):
        self.test_context = test_context
        self.app_context = app_context

    def build(self):
        pass

    def train(self, params):
        pass

    def deploy(self, params=None, wait_time=0):
        pass

    # runs pio app new on this app
    def new(self):
        pass

    # deletes this app from pio
    def delete(self):
        pass

    def send_events(self, events):
        pass

    def get_events(self):
        pass

    def remove_data(self):
        pass

    # gets url for the app to query eventserver
    def get_es_url(self):
        pass

    def query(self):
        pass

    # kills deoployed engine if it is running
    def stop(self):
        pass
