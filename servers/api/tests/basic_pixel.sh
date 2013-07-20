

echo "user without custom attributes"
curl --request GET -L "http://localhost:9124/p/users.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_uid=testuid3\
&pio_latlng=12.34,5.678\
&pio_inactive=true"
echo ""

echo "user with custom attributes"
curl --request GET -L "http://localhost:9124/p/users.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_uid=testuid2\
&pio_latlng=12.34,5.678\
&custom1=value1\
&pio_inactive=true"
echo ""


# item
echo "create item"
curl --request GET -L "http://localhost:9124/p/items.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_iid=testiid2\
&pio_itypes=type1,type2\
&pio_price=1.23\
&pio_profit=9.87\
&pio_startT=123456789\
&pio_endT=2013-02-12T05:43:21.4\
&pio_latlng=12.34,5.678\
&pio_inactive=true\
&custom1=value1\
&custom2=2.34"
echo ""

curl --request GET -L "http://localhost:9124/p/items.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_iid=testiid3\
&pio_itypes=type1,type2\
&pio_price=1.23\
&pio_profit=9.87\
&pio_startT=123456789\
&pio_endT=2013-02-12T05:43:21.4\
&pio_latlng=12.34,5.678\
&pio_inactive=true\
&custom1=value1\
&custom2=2.34"
echo ""



echo "rate action"
curl --request GET -L "http://localhost:9124/p/actions/u2i.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_action=rate\
&pio_uid=user2\
&pio_iid=item3\
&pio_rate=3"
echo ""

echo "rate with latlng and t"
curl --request GET -L "http://localhost:9124/p/actions/u2i.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_action=rate\
&pio_uid=user2\
&pio_iid=item3\
&pio_rate=3\
&pio_latlng=1.234,5.678\
&pio_t=2012-09-10T12:34:56.6"
echo ""

echo "missing rate field in rate action"
curl --request GET -L "http://localhost:9124/p/actions/u2i.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_action=rate\
&pio_uid=user2\
&pio_iid=item4"
echo ""

echo "like action"
curl --request GET -L "http://localhost:9124/p/actions/u2i.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_action=like\
&pio_uid=user3\
&pio_iid=item4"
echo ""

echo "dislike"
curl --request GET -L "http://localhost:9124/p/actions/u2i.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_action=dislike\
&pio_uid=user4\
&pio_iid=item5"
echo ""

echo "view"
curl --request GET -L "http://localhost:9124/p/actions/u2i.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_action=view\
&pio_uid=user6\
&pio_iid=item7"
echo ""


echo "conversion"
curl --request GET -L "http://localhost:9124/p/actions/u2i.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_action=conversion\
&pio_uid=user7\
&pio_iid=item8"
echo ""

echo "conversion with price"
curl --request GET -L "http://localhost:9124/p/actions/u2i.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_action=conversion\
&pio_uid=user8\
&pio_iid=item9\
&pio_price=5.99"
echo ""

echo "custom action"
curl --request GET -L "http://localhost:9124/p/actions/u2i.json\
?pio_appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&pio_action=custom_action\
&pio_uid=user2\
&pio_iid=item4"
echo ""
