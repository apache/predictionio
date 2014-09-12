"""
Import simple test data for testing getting itemrec
"""
import predictionio
import argparse
import time

def import_testdata(app_id, api_url):
  client = predictionio.Client(app_id, 1, api_url)
  predictionio.connection.enable_log("test.log")
  client.set_user("u0")
  client.set_user("u1")
  client.set_user("u2")
  client.set_user("u3")

  # update user info
  client.set_user("u1", {"zip": 94115 })
  client.set_user("u2", {"zip" : 94089})
  client.set_user("u0", {"gender" : "m", "zip" : 95054 })

  client.set_item("i0", {
    "pio_itypes": ["t1"],
    "custom1": "i0c1",
    "starttime": "2014-07-01T21:39:45.618Z",
    "endtime" : "2014-07-02T21:39:45.618Z",
    "price" : 4.5 })
  client.set_item("i1", {
    "pio_itypes": ["t1","t2"],
    "custom1": "i1c1",
    "custom2": "i1c2", "price" : 5.6 })
  client.set_item("i2", {
    "pio_itypes": ["t1","t2"],
    "custom2": "i2c2", "price" : 110,
    "inactive": True})
  client.set_item("i3", {
    "pio_itypes": ["t1"],
    "starttime": "2014-07-01T21:39:45.618Z",
    "endtime" : "2014-07-03T21:39:45.618Z",
    "price" : 9.99
  })


  ## some actions
  client.record_user_action_on_item("rate", "u0", "i0", { "pio_rate": 2 })
  client.record_user_action_on_item("rate", "u0", "i1", { "pio_rate": 3 })
  client.record_user_action_on_item("rate", "u0", "i2", { "pio_rate": 4 })

  client.record_user_action_on_item("rate", "u1", "i2", { "pio_rate": 4 })
  client.record_user_action_on_item("rate", "u1", "i3", { "pio_rate": 1 })

  client.record_user_action_on_item("view", "u2", "i1")
  client.record_user_action_on_item("rate", "u2", "i2", { "pio_rate": 1 })
  client.record_user_action_on_item("rate", "u2", "i3", { "pio_rate": 3 })
  client.record_user_action_on_item("conversion", "u2", "i3")

  client.record_user_action_on_item("rate", "u3", "i0", { "pio_rate": 5 })
  client.record_user_action_on_item("view", "u3", "i1")
  client.record_user_action_on_item("rate", "u3", "i3", { "pio_rate": 2 })

  time.sleep(3)

  # change user info
  client.set_user("u3", {"gender": "f"})
  client.set_user("u0", {"zip": 94086 })
  client.set_user("u2", {"zip": 94012 })
  client.unset_user("u1", {"zip": 0})

  # change item info
  client.unset_item("i1", {"custom2": ""})
  client.set_item("i2", {"price" : 99, "inactive" : False})
  client.set_item("i3", {
    "starttime": "2014-07-04T21:39:45.618Z",
    "endtime" : "2014-07-05T21:39:45.618Z",
    "price": 7.0
  })
  client.set_item("i0", {"inactive" : True})

  # more a
  client.record_user_action_on_item("rate", "u0", "i0", { "pio_rate": 2 })
  client.record_user_action_on_item("conversion", "u0", "i1")
  client.record_user_action_on_item("view", "u0", "i2")

  client.close()

def main():
  parser = argparse.ArgumentParser(description="some description here..")
  parser.add_argument('--appid', type=int, default=0)
  parser.add_argument('--apiurl', default="http://localhost:7070")

  args = parser.parse_args()
  print args

  import_testdata(
    app_id=args.appid,
    api_url=args.apiurl)

if __name__ == '__main__':
  main()
