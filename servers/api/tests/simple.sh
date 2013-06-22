


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

# u2iactions

# rate
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=user2" \
--data-urlencode "pio_tiid=item3" \
--data-urlencode "pio_rate=3"
echo ""

# rate with latlng and t
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=user2" \
--data-urlencode "pio_tiid=item3" \
--data-urlencode "pio_rate=3" \
--data-urlencode "pio_latlng=1.234,5.678" \
--data-urlencode "pio_t=2012-09-10T12:34:56.6"
echo ""

# missing rate field in rate action
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=user2" \
--data-urlencode "pio_tiid=item4"
echo ""

# like
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=like" \
--data-urlencode "pio_uid=user3" \
--data-urlencode "pio_tiid=item4"
echo ""

# dislike
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=dislike" \
--data-urlencode "pio_uid=user4" \
--data-urlencode "pio_tiid=item5"
echo ""

# view
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=view" \
--data-urlencode "pio_uid=user6" \
--data-urlencode "pio_tiid=item7"
echo ""


# conversion
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=conversion" \
--data-urlencode "pio_uid=user7" \
--data-urlencode "pio_tiid=item8"
echo ""

# conversion with price
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=conversion" \
--data-urlencode "pio_uid=user8" \
--data-urlencode "pio_tiid=item9" \
--data-urlencode "pio_price=5.99"
echo ""

# custom action
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=custom_action" \
--data-urlencode "pio_uid=user2" \
--data-urlencode "pio_tiid=item4"
echo ""