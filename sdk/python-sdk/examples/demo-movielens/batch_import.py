
from appdata import AppData
import predictionio
import sys

from app_config import APP_ID, API_URL, THREADS, REQUEST_QSIZE

def batch_import_task(app_data, client, all_info=False):

  print "[Info] Importing users to PredictionIO..."
  count = 0
  for k, v in app_data.get_users().iteritems():
    count += 1
    if all_info:
      print "[Info] Importing %s..." % v
    else:
      if (count % 32 == 0):
        sys.stdout.write('\r[Info] %s' % count)
        sys.stdout.flush()

    client.aset_user(v.uid)

  sys.stdout.write('\r[Info] %s users were imported.\n' % count)
  sys.stdout.flush()

  print "[Info] Importing items to PredictionIO..."
  count = 0
  for k, v in app_data.get_items().iteritems():
    count += 1
    if all_info:
      print "[Info] Importing %s..." % v
    else:
      if (count % 32 == 0):
        sys.stdout.write('\r[Info] %s' % count)
        sys.stdout.flush()

    itypes = ("movie",) + v.genres
    client.aset_item(v.iid,
      { "pio_itypes" : list(itypes),
        "pio_starttime" : v.release_date.isoformat(),
        "name" : v.name,
        "year" : v.year } )

  sys.stdout.write('\r[Info] %s items were imported.\n' % count)
  sys.stdout.flush()

  print "[Info] Importing rate actions to PredictionIO..."
  count = 0
  for v in app_data.get_rate_actions():
    count += 1
    if all_info:
      print "[Info] Importing %s..." % v
    else:
      if (count % 32 == 0):
        sys.stdout.write('\r[Info] %s' % count)
        sys.stdout.flush()

    #client.identify(v.uid)
    #client.arecord_user_action_on_item("rate",
    #  v.uid,
    #  v.iid,
    #   { "pio_rate": v.rating },)
    properties = { "pio_rate" : v.rating }
    req = client.acreate_event({
      "event" : "rate",
      "entityType" : "pio_user",
      "entityId" : v.uid,
      "targetEntityType" : "pio_item",
      "targetEntityId": v.iid,
      "properties" : properties,
      "appId" : APP_ID,
      "eventTime" : v.t.isoformat() + 'Z'
    })
    #print req.get_response()

  sys.stdout.write('\r[Info] %s rate actions were imported.\n' % count)
  sys.stdout.flush()


if __name__ == '__main__':

  app_data = AppData()
  print
  client = predictionio.DataClient(APP_ID,
    threads = THREADS,
    qsize=REQUEST_QSIZE)
  batch_import_task(app_data, client)
  client.close()
