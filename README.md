PredictionIO
============

PredictionIO is a prediction server for building smart applications. While you search data through a database server, you can make prediction through PredictionIO.

With PredictionIO, you can write apps
* that predict user behaviors based on solid data science
*	using your choice of state-of-the-art machine learning algorithms
*	without worrying about scalability

Detailed documentation *will be* available on our [wiki](https://github.com/PredictionIO/PredictionIO/wiki) page.

PREREQUISITES
=============
The current default PredictionIO setup assumes that you have the following installed and configured in a trusted environment:
* A recent version of Linux (other OS's have not been tested yet)
* Apache Hadoop 1.0+ (or any compatible distribution that supports the "hadoop jar" command)
* MongoDB 2.0+ (http://www.mongodb.org/)
* Scala 2.9.2+ (http://www.scala-lang.org/)
* sbt 0.12.1+ (http://www.scala-sbt.org/)
* Play 2.0+ (http://www.playframework.org/)

QUICK START
===========
Cloning
-------
Simply clone PredictionIO to your local machine.
The following steps assume that you have cloned the repo at your home directory.

    cd ~
    git clone git://github.com/PredictionIO/PredictionIO.git

Compiling PredictionIO
----------------------
Compile dependencies first using sbt.

    cd ~/PredictionIO/commons
    sbt +publish
    cd ~/PredictionIO/output
    sbt +publish

Compile and build a process assembly using sbt,
where `>` indicates commands that will be run in the sbt console.

    cd ~/PredictionIO/process/hadoop/scala
    sbt
    > project scala-assembly
    > assembly

Compile and pack the command line user administration tool.
The default configuration assumes that you are running MongoDB at localhost:27017.
If this is not the case, update the configuration in
`~/PredictionIO/tools/users/src/main/resources/application.conf` before compiling.

    io.prediction.commons.settings.db.type=mongodb
    io.prediction.commons.settings.db.host=your.host.com
    io.prediction.commons.settings.db.port=12345

After that, compile the tool.

    cd ~/PredictionIO/tools/users
    sbt pack

Adding a User
-------------
You must add at least one user to be able to log in.
Run

    ~/PredictionIO/tools/users/target/pack/bin/users

and follow the on-screen instructions to create a user.

Launch the Admin Panel
----------------------
Similar to the CLI tool, you may want to change your configuration, which is located at
`~/PredictionIO/adminServer/conf/application.conf`

Notice that the commons settings database should be the same as the one specified in the CLI tool.

Assuming you have installed the Play framework at /opt/play,
where `>` indicates commands that will be run in the Play console.

    cd ~/PredictionIO/adminServer
    /opt/play/play
    > update
    > compile
    > run

To access the admin panel, point your browser to http://localhost:9000/.
After the first run, you may skip `update` and `compile`.

Start the API Server
--------------------
Again, change the configuration in `~/PredictionIO/output/api/conf/application.conf`
where you see fit. With the same assumption from the step before,

    cd ~/PredictionIO/output/api
    /opt/play/play
    > update
    > compile
    > run 8000

This will start the API server on the default port 8000.

Start the Scheduler
-------------------
Change the configuration in `~PredictionIO/scheduler/conf/application.conf`
where you see fit.

In this configuration, however, you may want to change all database host names to one
that can be resolved by all nodes in your Hadoop farm.

With the same assumption from the step before,

    cd ~/PredictionIO/scheduler
    /opt/play/play
    > update
    > compile
    > run 7000

This will start the scheduler on the default port 7000.


STEP-BY-STEP TUTORIAL
=====================
Build a Recommendation Engine with 5 steps
===========================================

In this tutorial, we are building a unique recommendation engine on PredictionIO for a restaurant discovery app.
Sign into PredictionIO web admin panel using the administrator account you have created during installation.
Then follow these 5 steps:

Step 1: Add your App
--------------------

In the Applications page, add a new app by giving it a name, e.g. ‘My Restaurant App’, and click **Create**.

Step 2:  Obtain an App Key
--------------------------

Click **Develop** on ‘My Restaurant App’, and you will find the App Key.
This is the information you need when you integrate your app with PredictionIO SDKs later.

Step 3:  Create the Engine
--------------------------
Click **Add an Engine**. You will see the available engine types of PredictionIO.
In this example, we want to use ‘Item Recommendation Engine’ which can predict user preferences for items.
In our case, restaurants are the items.

Give your new engine a name, e.g. ‘restaurant-rec’, and click **Create**.

Now you have a working recommendation engine. You can start using it right away! 
If you can spare another minute with us, see how you can fine-tune this engine in ‘Adjust Prediction Settings’ (Step 4). Otherwise, skip to ‘Start Using the Engine’ (Step 5).

Step 4: Adjust Prediction Settings  (Optional)
----------------------------------------------

After your first engine is created, you will arrive at the Prediction Settings page.

1. Item Types Settings

    Here, you can define which types of items, i.e. Item Types, this engine should handle.

    With our example, we may assign a single item type ‘restaurant’ to all restaurants. But other item types such as ‘cafe’, ‘bar’, ‘fast-food’, ‘casual’ and ‘fine-dining’ may be assigned to individual restaurants.  

    If you want to this engine to only handle ‘fast-food’ and ‘casual’ types of restaurants, you should add ‘fast-food’ and ‘casual’ in the Item Types Settings area.

    By default, an Item Recommendation Engine would “Include ALL item types”.

2. Recommendation Preferences

    Recommendation preferences of different applications vary. For a newsfeed application or a group buying site, it is more desirable to recommend new items to users; for our example of restaurant discovery app, you may not always need to recommend the newest restaurants.  You can fine-tune this engine in the Recommendation Preferences area.

3. Recommendation Goal

    You can adjust what to optimize with this engine in this area.

Step 5: Start Using the Engine
------------------------------
Ruby SDK is used in examples below.

    client = PredictionIO::Client.new(<appkey>)

1. Import your Data

    Import your users, items and behaviors data into ‘My Restaurant App’ through the API key that you have obtained:

    Add User

        client.acreate_user(<username>)

    Add Item (restaurant)
    
        client.acreate_item(<itemname>, <array_of_item_types>)
    
    Add Behavior

        client.auser_rate_item(<username>, <itemname>, <rating_from_1_to_5>)
    
    > Note 1: Item Recommendation Engine uses previous user behavior data to predict users’ future preferences. 
    
    > Note 2: The data you import into ‘My Restaurant App’ will be shared among all engines you create.


2. Retrieve Prediction

    Item Recommendation Engine is trained/re-trained with new data every day. 
    
    To predict top N restaurants that a user may like:
    
        client.get_itemrec_top_n(<enginename>, <username>, <N>)
    
    Item Recommendation Engine also supports location-based and item validity scenario. Please refer to the [wiki](https://github.com/PredictionIO/PredictionIO/wiki) for more information.
    

Extra Step: Select and Tune Algorithms
--------------------------------------

An **Algorithms** tab can be found next to the **Prediction Settings** tab.
This is the place where you can fine-tune the underlying algorithm of the engine.


SUPPORT
===========

Forum
-----
https://groups.google.com/group/predictionio-user

LICENSE
=======
PredictionIO source files are made available under the terms of the [GNU Affero General Public License](http://www.gnu.org/licenses/agpl-3.0.html) (AGPL).
