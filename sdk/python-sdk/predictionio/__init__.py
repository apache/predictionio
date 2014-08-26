
__version__ = "0.8.0-SNAPSHOT"

# import packages
import re
try:
  import httplib
except ImportError:
  # pylint: disable=F0401
  # http is a Python3 module, replacing httplib
  from http import client as httplib
import json
import urllib

from predictionio.connection import Connection
from predictionio.connection import AsyncRequest
from predictionio.connection import PredictionIOAPIError

class NotCreatedError(PredictionIOAPIError):
  pass

class NotFoundError(PredictionIOAPIError):
  pass

class Client(object):
  def __init__(self, appid, threads=1, apiurl="http://localhost:8000",
         apiversion="", qsize=0, timeout=5):
    """Constructor of Client object.

    """
    self.appid = appid
    self.threads = threads
    self.apiurl = apiurl
    self.apiversion = apiversion
    self.qsize = qsize
    self.timeout = timeout

    # check connection type
    https_pattern = r'^https://(.*)'
    http_pattern = r'^http://(.*)'
    m = re.match(https_pattern, apiurl)
    self.https = True
    if m is None:  # not matching https
      m = re.match(http_pattern, apiurl)
      self.https = False
      if m is None:  # not matching http either
        raise InvalidArgumentError("apiurl is not valid: %s" % apiurl)
    self.host = m.group(1)

    self._uid = None  # identified uid
    self._connection = Connection(host=self.host, threads=self.threads,
                    qsize=self.qsize, https=self.https,
                    timeout=self.timeout)

  def close(self):
    """Close this client and the connection.

    Call this method when you want to completely terminate the connection
    with PredictionIO.
    It will wait for all pending requests to finish.
    """
    self._connection.close()

  def pending_requests(self):
    """Return the number of pending requests.

    :returns:
      The number of pending requests of this client.
    """
    return self._connection.pending_requests()

  def get_status(self):
    """Get the status of the PredictionIO API Server

    :returns:
      status message.

    :raises:
      ServerStatusError.
    """
    path = "/"
    request = AsyncRequest("GET", path)
    request.set_rfunc(self._aget_resp)
    self._connection.make_request(request)
    result = request.get_response()
    return result

  def acreate_event(self, data):
    path = "/events"
    request = AsyncRequest("POST", path, **data)
    request.set_rfunc(self._acreate_resp)
    self._connection.make_request(request)
    return request

  def aget_event(self, event_id):
    enc_event_id = urllib.quote(event_id, "") # replace special char with %xx
    path = "/events/%s" % enc_event_id
    request = AsyncRequest("GET", path)
    requset.set_rfunc(self._aget_resp)
    self._connection.make_request(request)
    return request

  def arecord_user(self, uid, params={}):
    return self.acreate_event({
      "event" : "$set",
      "entityId" : uid,
      "properties" : params,
      "tags" : [ "user" ],
      "appId" : self.appid
    })

  def arecord_item(self, iid, itypes=[], params={}):
    tags = [ "items" ] + itypes
    return self.acreate_event({
      "event" : "$set",
      "entityId" : iid,
      "properties" : params,
      "tags" : tags,
      "appId" : self.appid
    })

  def arecord_user_action_on_item(self, action, uid, iid, params={}):
    return self.acreate_event({
      "event" : action,
      "entityId" : uid,
      "targetEntityId": iid,
      "properties" : params,
      "appId" : self.appid
    })

  def create_user(self, uid, params={}):
    return self.arecord_user(uid, params).get_response()

  def create_item(self, iid, itypes=[], params={}):
    return self.arecord_item(iid, itypes, params).get_response()

  def record_user_action_on_item(self, action, uid, iid, params={}):
    return self.arecord_user_action_on_item(action, uid, iid, params).get_response()

  def _acreate_resp(self, response):
    if response.error is not None:
      raise NotCreatedError("Exception happened: %s for request %s" %
                    (response.error, response.request))
    elif response.status != httplib.CREATED:
      raise NotCreatedError("request: %s status: %s body: %s" %
                    (response.request, response.status,
                     response.body))

    return None

  def _aget_resp(self, response):
    if response.error is not None:
      raise NotFoundError("Exception happened: %s for request %s" %
                  (response.error, response.request))
    elif response.status != httplib.OK:
      raise NotFoundError("request: %s status: %s body: %s" %
                  (response.request, response.status,
                   response.body))

    data = json.loads(response.body)  # convert json string to dict
    return data
