Register (Build) + Train + Deploy : 


$PIO_HOME/bin/pio register && $PIO_HOME/bin/pio train && $PIO_HOME/bin/pio
deploy


Query : 

curl -H "Content-Type: application/json" -d '{ "userId": 888 , "seed" : 999}'
http://localhost:8000/queries.json


UserId is harded coded (because we are currently using synthetic dataset). For
actual data, we will use userId from dataset.
Seed is used to seed randomization process. It is optional paramete : the
default is hardcoded value.
