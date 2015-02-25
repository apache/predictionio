import predictionio

import random
import time

NUM_OF_USER = 100000
NUM_OF_ITEM = 100000

NUM_OF_QUERY = 30
GAP = 0.05 # s
NUM_OF_THREADS = 3

#SEED = 7
#random.seed(SEED)

engine_client = predictionio.EngineClient(url="http://localhost:8000", threads=NUM_OF_THREADS)

for i in range(0, NUM_OF_QUERY):
  uid = "u%s" % random.randint(1, NUM_OF_USER)
  #print uid
  engine_client.asend_query({"user": uid, "num": 10, "categories" : ["c4", "c3"]})
  time.sleep(GAP)

  #print r
engine_client.close()
