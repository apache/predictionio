
try:
  import Queue
except ImportError:
  # pylint: disable=F0401
  # http is a Python3 module, replacing httplib. Ditto.
  import queue as Queue
import threading

try:
  import httplib
except ImportError:
  # pylint: disable=F0401
  from http import client as httplib

try:
  from urllib import urlencode
except ImportError:
  # pylint: disable=F0401,E0611
  from urllib.parse import urlencode

import datetime
import logging
import json

# use generators for python2 and python3
try:
  xrange
except NameError:
  xrange = range

# some constants
MAX_RETRY = 1  # 0 means no retry


# logger
logger = None
DEBUG_LOG = False


def enable_log(filename=None):
  global logger
  global DEBUG_LOG
  timestamp = datetime.datetime.today()
  if not filename:
    logfile = "./log/predictionio_%s.log" % timestamp.strftime(
      "%Y-%m-%d_%H:%M:%S.%f")
  else:
    logfile = filename
  logging.basicConfig(filename=logfile,
            filemode='w',
            level=logging.DEBUG,
            format='[%(levelname)s] %(name)s (%(threadName)s) %(message)s')
  logger = logging.getLogger(__name__)
  DEBUG_LOG = True


class PredictionIOAPIError(Exception):
  pass


class NotSupportMethodError(PredictionIOAPIError):
  pass


class ProgramError(PredictionIOAPIError):
  pass


class AsyncRequest(object):

  """AsyncRequest object

  """

  def __init__(self, method, path, **params):
    self.method = method  # "GET" "POST" etc
    # the sub path eg. POST /v1/users.json  GET /v1/users/1.json
    self.path = path
    # dictionary format eg. {"appkey" : 123, "id" : 3}
    self.params = params
    # use queue to implement response, store AsyncResponse object
    self.response_q = Queue.Queue(1)
    self.qpath = "%s?%s" % (self.path, urlencode(self.params))
    self._response = None
    # response function to be called to handle the response
    self.rfunc = None

  def __str__(self):
    return "%s %s %s %s" % (self.method, self.path, self.params,
                self.qpath)

  def set_rfunc(self, func):
    self.rfunc = func

  def set_response(self, response):
    """ store the response

    NOTE: Must be only called once
    """
    self.response_q.put(response)

  def get_response(self):
    """get the response

    """
    if self._response is None:
      tmp_response = self.response_q.get(True)  # NOTE: blocking
      if self.rfunc is None:
        self._response = tmp_response
      else:
        self._response = self.rfunc(tmp_response)

    return self._response


class AsyncResponse(object):

  """AsyncResponse object.

  Store the response of asynchronous request

  when get the response, user should check if error is None (which means no
  Exception happens)
  if error is None, then should check if the status is expected

  Attributes:
    error: exception object if any happens
    version: int
    status: int
    reason: str
    headers: dict
    body: str (NOTE: not necessarily can be converted to JSON,
        eg, for GET request to /v1/status)
    request: the corresponding AsyncRequest object
  """

  def __init__(self):
    self.error = None
    self.version = None
    self.status = None
    self.reason = None
    self.headers = None
    self.body = None
    self.request = None  # point back to the request object

  def __str__(self):
    return "e:%s v:%s s:%s r:%s h:%s b:%s" % (self.error, self.version,
                          self.status, self.reason,
                          self.headers, self.body)

  def set_resp(self, version, status, reason, headers, body):
    self.version = version
    self.status = status
    self.reason = reason
    self.headers = headers
    self.body = body

  def set_error(self, error):
    self.error = error

  def set_request(self, request):
    self.request = request


class PredictionIOHttpConnection(object):

  def __init__(self, host, https=True, timeout=5):
    if https:  # https connection
      self._connection = httplib.HTTPSConnection(host, timeout=timeout)
    else:
      self._connection = httplib.HTTPConnection(host, timeout=timeout)

  def connect(self):
    self._connection.connect()

  def close(self):
    self._connection.close()

  def request(self, method, url, body={}, headers={}):
    """
    http request wrapper function, with retry capability in case of error.
    catch error exception and store it in AsyncResponse object
    return AsyncResponse object

    Args:
      method: http method, type str
      url: url path, type str
      body: http request body content, type dict
      header: http request header , type dict
    """

    response = AsyncResponse()

    try:
      # number of retry in case of error (minimum 0 means no retry)
      retry_limit = MAX_RETRY
      mod_headers = dict(headers)  # copy the headers
      mod_headers["Connection"] = "keep-alive"
      enc_body = None
      if body:  # if body is not empty
        #enc_body = urlencode(body)
        enc_body = json.dumps(body)
        mod_headers[
          "Content-type"] = "application/json"
          #"Content-type"] = "application/x-www-form-urlencoded"
        #mod_headers["Accept"] = "text/plain"
    except Exception as e:
      response.set_error(e)
      return response

    if DEBUG_LOG:
      logger.debug("Request m:%s u:%s h:%s b:%s", method, url,
             mod_headers, enc_body)
    # retry loop
    for i in xrange(retry_limit + 1):
      try:
        if i != 0:
          if DEBUG_LOG:
            logger.debug("retry request %s times" % i)
        if self._connection.sock is None:
          self._connection.connect()
        self._connection.request(method, url, enc_body, mod_headers)
      except Exception as e:
        self._connection.close()
        if i == retry_limit:
          # new copy of e created everytime??
          response.set_error(e)
      else:  # NOTE: this is try's else clause
        # connect() and request() OK
        try:
          resp = self._connection.getresponse()
        except Exception as e:
          self._connection.close()
          if i == retry_limit:
            response.set_error(e)
        else:  # NOTE: this is try's else clause
          # getresponse() OK
          resp_version = resp.version  # int
          resp_status = resp.status  # int
          resp_reason = resp.reason  # str
          # resp.getheaders() returns list of tuples
          # converted to dict format
          resp_headers = dict(resp.getheaders())
          # NOTE: have to read the response before sending out next
          # http request
          resp_body = resp.read()  # str
          response.set_resp(version=resp_version, status=resp_status,
                    reason=resp_reason, headers=resp_headers,
                    body=resp_body)
          break  # exit retry loop
    # end of retry loop
    if DEBUG_LOG:
      logger.debug("Response %s", response)
    return response  # AsyncResponse object


def connection_worker(host, request_queue, https=True, timeout=5, loop=True):
  """worker function which establishes connection and wait for request jobs
  from the request_queue

  Args:
    request_queue: the request queue storing the AsyncRequest object
      valid requests:
        GET
        POST
        DELETE
        KILL
    https: HTTPS (True) or HTTP (False)
    timeout: timeout for HTTP connection attempts and requests in seconds
    loop: This worker function stays in a loop waiting for request
      For testing purpose only. should always be set to True.
  """

  connect = PredictionIOHttpConnection(host, https, timeout)

  # loop waiting for job form request queue
  killed = not loop

  while True:
    # print "thread %s waiting for request" % thread.get_ident()
    request = request_queue.get(True)  # NOTE: blocking get
    # print "get request %s" % request
    method = request.method
    if method == "GET":
      path = request.qpath
      d = connect.request("GET", path)
    elif method == "POST":
      path = request.path
      body = request.params
      d = connect.request("POST", path, body)
    elif method == "DELETE":
      path = request.qpath
      d = connect.request("DELETE", path)
    elif method == "KILL":
      # tell the thread to kill the connection
      killed = True
      d = AsyncResponse()
    else:
      d = AsyncResponse()
      d.set_error(NotSupportMethodError(
        "Don't Support the method %s" % method))

    d.set_request(request)
    request.set_response(d)
    request_queue.task_done()
    if killed:
      break

  # end of while loop
  connect.close()


class Connection(object):

  """abstract object for connection with server

  spawn multiple connection_worker threads to handle jobs in the queue q
  """

  def __init__(self, host, threads=1, qsize=0, https=True, timeout=5):
    """constructor

    Args:
      host: host of the server.
      threads: type int, number of threads to be spawn
      qsize: size of the queue q
      https: indicate it is httpS (True) or http connection (False)
      timeout: timeout for HTTP connection attempts and requests in
        seconds
    """
    self.host = host
    self.https = https
    self.q = Queue.Queue(qsize)  # if qsize=0, means infinite
    self.threads = threads
    self.timeout = timeout
    # start thread based on threads number
    self.tid = {}  # dictionary of thread object

    for i in xrange(threads):
      tname = "PredictionIOThread-%s" % i  # thread name
      self.tid[i] = threading.Thread(
        target=connection_worker, name=tname,
        kwargs={'host': self.host, 'request_queue': self.q,
            'https': self.https, 'timeout': self.timeout})
      self.tid[i].setDaemon(True)
      self.tid[i].start()

  def make_request(self, request):
    """put the request into the q
    """
    self.q.put(request)

  def pending_requests(self):
    """number of pending requests in the queue
    """
    return self.q.qsize()

  def close(self):
    """close this Connection. Call this when main program exits
    """
    # set kill message to q
    for i in xrange(self.threads):
      self.make_request(AsyncRequest("KILL", ""))

    self.q.join()  # wait for q empty

    for i in xrange(self.threads):  # wait for all thread finish
      self.tid[i].join()
