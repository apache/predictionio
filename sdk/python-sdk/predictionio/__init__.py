
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

  def aset_user(self, uid, properties={}):
    """set properties of an user"""
    return self.acreate_event({
      "event" : "$set",
      "entityType" : "user",
      "entityId" : uid,
      "properties" : properties,
      "appId" : self.appid
    })

  def aunset_user(self, uid, properties={}):
    """unset properties of an user"""
    return self.acreate_event({
      "event" : "$unset",
      "entityType" : "user",
      "entityId" : uid,
      "properties" : properties,
      "appId" : self.appid
    })

  def aset_item(self, iid, properties={}):
    #tags = itypes # TODO: itypes should be in tags ? or properties?
    return self.acreate_event({
      "event" : "$set",
      "entityType" : "item",
      "entityId" : iid,
      "properties" : properties,
      #"tags" : tags,
      "appId" : self.appid
    })

  def aunset_item(self, iid, properties={}):
    # tags = itypes # TODO: itypes should be in tags ? or properties?
    return self.acreate_event({
      "event" : "$unset",
      "entityType" : "item",
      "entityId" : iid,
      "properties" : properties,
      #"tags" : tags,
      "appId" : self.appid
    })

  def arecord_user_action_on_item(self, action, uid, iid, properties={}):
    return self.acreate_event({
      "event" : action,
      "entityType" : "user",
      "entityId" : uid,
      "targetEntityType" : "item",
      "targetEntityId": iid,
      "properties" : properties,
      "appId" : self.appid
    })

  def set_user(self, uid, properties={}):
    return self.aset_user(uid, properties).get_response()

  def set_item(self, iid, properties={}):
    return self.aset_item(iid, properties).get_response()

  def unset_user(self, uid, properties={}):
    return self.aunset_user(uid, properties).get_response()

  def unset_item(self, iid, properties={}):
    return self.aunset_item(iid, properties).get_response()

  def record_user_action_on_item(self, action, uid, iid, properties={}):
    return self.arecord_user_action_on_item(
      action, uid, iid, properties).get_response()

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
