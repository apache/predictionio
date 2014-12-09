// MongoDB script to insert sample random ratings data into MongoDB
db = connect("localhost:27017/test");

print("Remove old data in test.sample_ratings collection...")
db.sample_ratings.remove();

// min <= x < max
function getRandomInt(min, max) {
  return Math.floor(Math.random() * (max - min)) + min;
}

print("Insert random movie rating data into test.sample_ratings collection...")
// for eah user 0 to 10, randomly view 10 items between 0 to 49
for (var uid = 0; uid < 10; uid++) {
  for (var n = 0; n < 10; n++) {
    db.sample_ratings.insert( {
      "uid" : uid.toString(),
      "iid" : getRandomInt(0, 50).toString(), // 0 <= iid < 50
      "rating" : getRandomInt(1, 6) // 1 <= rating < 6
    })
  }
}

print("done.")
