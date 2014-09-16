
import datetime
from operator import itemgetter, attrgetter

# can get sample data here:
# wget http://www.grouplens.org/system/files/ml-100k.zip
# app data file config
APPDATA_DIRNAME = "ml-100k"
USERS_FILENAME = "u.user"
USERS_FILE_DELIMITER = "|"
ITEMS_FILENAME = "u.item"
ITEMS_FILE_DELIMITER = "|"
RATE_ACTIONS_FILENAME = "u.data"
RATE_ACTIONS_DELIMITER = "\t"


class User:
  def __init__(self, uid):
    self.uid = uid
    self.rec = [] # recommendations, list of iid

  def __str__(self):
    return "User[uid=%s,rec=%s]" % (self.uid, self.rec)

class Item:
  def __init__(self, iid, name, release_date, genres, year):
    self.iid = iid
    self.name = name
    self.release_date = release_date # datetime.datetime object
    self.genres = genres
    self.year = year

  def __str__(self):
    return "Item[iid=%s,name=%s,release_date=%s,genres=%s]" % (self.iid, self.name, self.release_date, self.genres)

class RateAction:
  def __init__(self, uid, iid, rating, t):
    self.uid = uid
    self.iid = iid
    self.rating = rating
    self.t = t

  def __str__(self):
    return "RateAction[uid=%s,iid=%s,rating=%s,t=%s]" % (self.uid, self.iid, self.rating, self.t)


class AppData:

  def __init__(self):
    self._users = {} # dict of User obj
    self._items = {} # dict of Item obj
    self._rate_actions = [] # list of RateAction obj

    self._users_file = "%s/%s" % (APPDATA_DIRNAME, USERS_FILENAME)
    self._items_file = "%s/%s" % (APPDATA_DIRNAME, ITEMS_FILENAME)
    self._rate_actions_file = "%s/%s" % (APPDATA_DIRNAME, RATE_ACTIONS_FILENAME)
    self.__init_users()
    self.__init_items()
    self.__init_rate_actions()

  def __init_users(self):
    """
    uid|
    """
    print "[Info] Initializing users..."
    f = open(self._users_file, 'r')
    for line in f:
      data = line.rstrip('\r\n').split(USERS_FILE_DELIMITER)
      self.add_user(User(data[0]))
    f.close()
    print "[Info] %s users were initialized." % len(self._users)

  def __init_items(self):
    """
    movie id | movie title | release date | video release date |
        IMDb URL | unknown | Action | Adventure | Animation |
        Children's | Comedy | Crime | Documentary | Drama | Fantasy |
        Film-Noir | Horror | Musical | Mystery | Romance | Sci-Fi |
        Thriller | War | Western |
        The last 19 fields are the genres, a 1 indicates the movie
        is of that genre, a 0 indicates it is not; movies can be in
        several genres at once.

    """
    genre_names = [ "unknown", "Action", "Adventure", "Animation",
      "Children's", "Comedy", "Crime", "Documentary", "Drama", "Fantasy",
      "Film-Noir", "Horror", "Musical", "Mystery", "Romance", "Sci-Fi",
      "Thriller", "War", "Western"]

    print "[Info] Initializing items..."
    f = open(self._items_file, 'r')
    for line in f:
      data = line.rstrip('\r\n').split(ITEMS_FILE_DELIMITER)
      genres_flags = data[5:24]

      genres = () # tuple of genres
      for g,flag in zip(genre_names, genres_flags):
        if flag == '1':
          genres = genres + (g,)

      try:
        # eg. 01-Jan-1994
        release_date = datetime.datetime.strptime(data[2], "%d-%b-%Y").replace(microsecond=1)
        (day, month, year) = data[2].split('-')
      except:
        print "[Note] item %s %s doesn't have release date. Skip it." % (data[0], data[1])
      else:
        self.add_item(Item(
          iid=data[0],
          name=data[1],
          release_date=release_date,
          genres=genres,
          year=year))
    f.close()
    print "[Info] %s items were initialized." % len(self._items)

  def __init_rate_actions(self):
    """
    uid|iid|rating|timestamp
    """
    print "[Info] Initializing rate actions..."
    f = open(self._rate_actions_file, 'r')
    for line in f:
      data = line.rstrip('\r\n').split(RATE_ACTIONS_DELIMITER)
      t = datetime.datetime.utcfromtimestamp(int(data[3])).replace(microsecond=1)
      self.add_rate_action(RateAction(data[0], data[1], data[2], t))
    f.close()
    print "[Info] %s rate actions were initialized." % len(self._rate_actions)

  def add_user(self, user):
    self._users[user.uid] = user

  def add_item(self, item):
    self._items[item.iid] = item

  def add_rate_action(self, action):
    self._rate_actions.append(action)

  def get_users(self):
    return self._users

  def get_items(self):
    return self._items

  def get_rate_actions(self):
    return self._rate_actions

  def get_user(self, uid):
    """return single user
    """
    if uid in self._users:
      return self._users[uid]
    else:
      return None

  def get_item(self, iid):
    """return single item
    """
    if iid in self._items:
      return self._items[iid]
    else:
      return None

  def get_top_rated_items(self, uid, n):
    """get top n rated iids by this uid
    """
    if uid in self._users:
      actions = filter(lambda u: u.uid==uid, self._rate_actions)
      top = sorted(actions, key=attrgetter('rating'), reverse=True)
      topn_iids = map(lambda a: a.iid, top[:n])
      return topn_iids
    else:
      return None

  def get_top_rate_actions(self, uid, n):
    """get top n rated actions by this uid
    """
    if uid in self._users:
      actions = filter(lambda u: u.uid==uid, self._rate_actions)
      top = sorted(actions, key=attrgetter('rating'), reverse=True)
      return top[:n]
    else:
      return None
