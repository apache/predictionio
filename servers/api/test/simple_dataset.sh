

curl --request POST http://localhost:9124/users.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_uid=u0" \
--data-urlencode "pio_latlng=12.34,5.678" \
--data-urlencode "custom1=value0"

curl --request POST http://localhost:9124/users.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_uid=u1" \
--data-urlencode "pio_latlng=12.34,5.678" \
--data-urlencode "custom1=value1"

curl --request POST http://localhost:9124/users.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_uid=u2" \
--data-urlencode "pio_latlng=12.34,5.678" \
--data-urlencode "custom1=value2"

curl --request POST http://localhost:9124/users.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_uid=u3" \
--data-urlencode "pio_latlng=12.34,5.678" \
--data-urlencode "custom1=value3"

curl --request POST http://localhost:9124/items.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_iid=i0" \
--data-urlencode "pio_itypes=t1" \
--data-urlencode "custom1=ic0"

curl --request POST http://localhost:9124/items.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_iid=i1" \
--data-urlencode "pio_itypes=t1" \
--data-urlencode "custom1=ic1"

curl --request POST http://localhost:9124/items.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_iid=i2" \
--data-urlencode "pio_itypes=t1" \
--data-urlencode "custom1=ic2"

curl --request POST http://localhost:9124/items.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_iid=i3" \
--data-urlencode "pio_itypes=t1" \
--data-urlencode "custom1=ic3"

##
curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u0" \
--data-urlencode "pio_iid=i0" \
--data-urlencode "pio_rate=2"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u0" \
--data-urlencode "pio_iid=i1" \
--data-urlencode "pio_rate=3"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u0" \
--data-urlencode "pio_iid=i2" \
--data-urlencode "pio_rate=4"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u1" \
--data-urlencode "pio_iid=i2" \
--data-urlencode "pio_rate=4"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u1" \
--data-urlencode "pio_iid=i3" \
--data-urlencode "pio_rate=1"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u2" \
--data-urlencode "pio_iid=i1" \
--data-urlencode "pio_rate=2"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u2" \
--data-urlencode "pio_iid=i2" \
--data-urlencode "pio_rate=1"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u2" \
--data-urlencode "pio_iid=i3" \
--data-urlencode "pio_rate=3"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u3" \
--data-urlencode "pio_iid=i0" \
--data-urlencode "pio_rate=5"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u3" \
--data-urlencode "pio_iid=i1" \
--data-urlencode "pio_rate=3"

curl --request POST http://localhost:9124/actions/u2i.json --header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO" \
--data-urlencode "pio_action=rate" \
--data-urlencode "pio_uid=u3" \
--data-urlencode "pio_iid=i3" \
--data-urlencode "pio_rate=2"
