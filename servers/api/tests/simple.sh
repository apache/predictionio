


curl --request GET http://localhost:9124/
echo ""

## JSON format
curl --header 'Content-Type: application/json' --request POST \
--data '{"pio_appkey": "jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO", "pio_uid" : "testuid", "custom1" : "value1", "custom2" : 2, "custom3" : 4.123, "custom4" : false }' http://localhost:9124/users.json
echo ""

curl --header 'Content-Type: application/json' --request POST \
--data '{"pio_appkey": "jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO", "pio_uid" : "testuid", "pio_latlng" : "1.23,4.56", "pio_inactive" : true,  "custom1" : "value1", "custom2" : 2, "custom3" : 4.123, "custom4" : false }' http://localhost:9124/users.json
echo ""

curl --request GET http://localhost:9124/users/testuid.json?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO
echo ""

curl --request DELETE http://localhost:9124/users/testuid.json?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO
echo ""

### form-urlencoded

# user
curl --request POST http://localhost:9124/users.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_uid=testuid2" \
--data-urlencode "pio_latlng=12.34,5.678" \
--data-urlencode "custom1=value1" \
--data-urlencode "pio_inactive=true"
echo ""

curl --request GET http://localhost:9124/users/testuid2.json?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO
echo ""

curl --request DELETE http://localhost:9124/users/testuid2.json?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO
echo ""

# item
curl --request POST http://localhost:9124/items.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_iid=testiid2" \
--data-urlencode "pio_itypes=type1,type2" \
--data-urlencode "pio_price=1.23" \
--data-urlencode "pio_profit=9.87" \
--data-urlencode "pio_startT=123456789" \
--data-urlencode "pio_endT=2013-02-12T05:43:21.4" \
--data-urlencode "pio_latlng=12.34,5.678" \
--data-urlencode "pio_inactive=true" \
--data-urlencode "custom1=value1" \
--data-urlencode "custom2=2.34"
echo ""

curl --request GET http://localhost:9124/items/testiid2.json?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO
echo ""

curl --request DELETE http://localhost:9124/items/testiid2.json?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO
echo ""

