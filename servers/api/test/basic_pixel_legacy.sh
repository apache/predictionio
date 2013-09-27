
# legacy pixel tracking format

echo "user without custom attributes"
curl --request GET -L "http://localhost:9124/p/users.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=testuid3\
&latlng=12.34,5.678\
&inactive=true"
echo ""

echo "user with custom attributes"
curl --request GET -L "http://localhost:9124/p/users.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=testuid2\
&latlng=12.34,5.678\
&custom1=value1\
&inactive=true"
echo ""


# item
echo "create item"
curl --request GET -L "http://localhost:9124/p/items.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&iid=testiid2\
&itypes=type1,type2\
&price=1.23\
&profit=9.87\
&startT=123456789\
&endT=2013-02-12T05:43:21.4\
&latlng=12.34,5.678\
&inactive=true\
&custom1=value1\
&custom2=2.34"
echo ""

curl --request GET -L "http://localhost:9124/p/items.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&iid=testiid3\
&itypes=type1,type2\
&price=1.23\
&profit=9.87\
&startT=123456789\
&endT=2013-02-12T05:43:21.4\
&latlng=12.34,5.678\
&inactive=true\
&custom1=value1\
&custom2=2.34"
echo ""



echo "rate action"
curl --request GET -L "http://localhost:9124/p/actions/u2i/rate.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=user2\
&iid=item3\
&rate=3"
echo ""

echo "rate with latlng and t"
curl --request GET -L "http://localhost:9124/p/actions/u2i/rate.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=user2\
&iid=item3\
&rate=3\
&latlng=1.234,5.678\
&t=2012-09-10T12:34:56.6"
echo ""

echo "missing rate field in rate action"
curl --request GET -L "http://localhost:9124/p/actions/u2i/rate.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=user2\
&iid=item4"
echo ""

echo "like action"
curl --request GET -L "http://localhost:9124/p/actions/u2i/like.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=user3\
&iid=item4"
echo ""

echo "dislike"
curl --request GET -L "http://localhost:9124/p/actions/u2i/dislike.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=user4\
&iid=item5"
echo ""

echo "view"
curl --request GET -L "http://localhost:9124/p/actions/u2i/view.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=user6\
&iid=item7"
echo ""


echo "conversion"
curl --request GET -L "http://localhost:9124/p/actions/u2i/conversion.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=user7\
&iid=item8"
echo ""

echo "conversion with price"
curl --request GET -L "http://localhost:9124/p/actions/u2i/conversion.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=user8\
&iid=item9\
&price=5.99"
echo ""

echo "custom action"
curl --request GET -L "http://localhost:9124/p/actions/u2i/custom_action.json\
?appkey=jLIvMU9FNydsJHpO7otA4Dh4FQTDDyP3hFA9DltuyAdadcxXdtpMXYLnOLtCTsWO\
&uid=user2\
&iid=item4"
echo ""
