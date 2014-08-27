"""
Import simple test data for testing getting itemrec
"""
import predictionio
import argparse

def import_testdata(app_id, api_url):
  client = predictionio.Client(app_id, 1, api_url)
  predictionio.connection.enable_log("test.log")
  client.set_user("u0")
  client.set_user("u1")
  client.set_user("u2")
  client.set_user("u3")

  client.set_item("i0", ["t1"], {"custom1": "i0c1"})
  client.set_item("i1", ["t1","t2"], {"custom1": "i1c1", "custom2": "i1c2"})
  client.set_item("i2", ["t1","t2"], {"custom2": "i2c2"})
  client.set_item("i3", ["t1"])

  ##
  client.record_user_action_on_item("rate", "u0", "i0", { "pio_rate": 2 })
  client.record_user_action_on_item("rate", "u0", "i1", { "pio_rate": 3 })
  client.record_user_action_on_item("rate", "u0", "i2", { "pio_rate": 4 })

  client.record_user_action_on_item("rate", "u1", "i2", { "pio_rate": 4 })
  client.record_user_action_on_item("rate", "u1", "i3", { "pio_rate": 1 })

  client.record_user_action_on_item("rate", "u2", "i1", { "pio_rate": 2 })
  client.record_user_action_on_item("rate", "u2", "i2", { "pio_rate": 1 })
  client.record_user_action_on_item("rate", "u2", "i3", { "pio_rate": 3 })

  client.record_user_action_on_item("rate", "u3", "i0", { "pio_rate": 5 })
  client.record_user_action_on_item("rate", "u3", "i1", { "pio_rate": 3 })
  client.record_user_action_on_item("rate", "u3", "i3", { "pio_rate": 2 })

  client.close()

def main():
  parser = argparse.ArgumentParser(description="some description here..")
  parser.add_argument('--appid', type=int, default=0)
  parser.add_argument('--apiurl', default="http://localhost:8081")

  args = parser.parse_args()
  print args

  import_testdata(
    app_id=args.appid,
    api_url=args.apiurl)

if __name__ == '__main__':
  main()
