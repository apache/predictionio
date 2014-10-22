Engine is based off of Hello World engine template.


Register (Build) + Train + Deploy : 

```
$PIO_HOME/bin/pio register 

$PIO_HOME/bin/pio train 

$PIO_HOME/bin/pio deploy
```


To query :

```
curl -H "Content-Type: application/json" -d '{ "userId": 888 , "seed" : 999}'
http://localhost:8000/queries.json
```


UserId is harded coded (because we are currently using synthetic dataset). For
actual data, we will use userId from dataset.
Seed is used to seed randomization process. It is optional paramete : the
default is hardcoded value.


Update from 10/22/2014
Now, we can modify the file path to training data and the algorithm construct 5 random items associated to the userid provided. 
The algorithm will take a userid and link that to 5 random itemid that it picks from the training data.
When query, the provided userid such as 888 must be provided in the training data or gives an internal server error.


