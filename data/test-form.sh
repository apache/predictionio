#!/usr/bin/env bash
accessKey=$1

# normal
curl -i -X POST http://localhost:7070/webhooks/mailchimp?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
--data-urlencode "type"="subscribe" \
--data-urlencode "fired_at"="2009-03-26 21:35:57" \
--data-urlencode "data[id]"="8a25ff1d98" \
--data-urlencode "data[list_id]"="a6b5da1054" \
--data-urlencode "data[email]"="api@mailchimp.com" \
--data-urlencode "data[email_type]"="html" \
--data-urlencode "data[merges][EMAIL]"="api@mailchimp.com" \
--data-urlencode "data[merges][FNAME]"="MailChimp" \
--data-urlencode "data[merges][LNAME]"="API" \
--data-urlencode "data[merges][INTERESTS]"="Group1,Group2" \
--data-urlencode "data[ip_opt]"="10.20.10.30" \
--data-urlencode "data[ip_signup]"="10.20.10.30" \
-w %{time_total}

# invalid type
curl -i -X POST http://localhost:7070/webhooks/mailchimp?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
--data-urlencode "type"="something_invalid" \
--data-urlencode "fired_at"="2009-03-26 21:35:57" \
--data-urlencode "data[id]"="8a25ff1d98" \
--data-urlencode "data[list_id]"="a6b5da1054" \
--data-urlencode "data[email]"="api@mailchimp.com" \
--data-urlencode "data[email_type]"="html" \
--data-urlencode "data[merges][EMAIL]"="api@mailchimp.com" \
--data-urlencode "data[merges][FNAME]"="MailChimp" \
--data-urlencode "data[merges][LNAME]"="API" \
--data-urlencode "data[merges][INTERESTS]"="Group1,Group2" \
--data-urlencode "data[ip_opt]"="10.20.10.30" \
--data-urlencode "data[ip_signup]"="10.20.10.30" \
-w %{time_total}

# missing data (type)
curl -i -X POST http://localhost:7070/webhooks/mailchimp?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
--data-urlencode "fired_at"="2009-03-26 21:35:57" \
--data-urlencode "data[id]"="8a25ff1d98" \
--data-urlencode "data[list_id]"="a6b5da1054" \
--data-urlencode "data[email]"="api@mailchimp.com" \
--data-urlencode "data[email_type]"="html" \
--data-urlencode "data[merges][EMAIL]"="api@mailchimp.com" \
--data-urlencode "data[merges][FNAME]"="MailChimp" \
--data-urlencode "data[merges][LNAME]"="API" \
--data-urlencode "data[merges][INTERESTS]"="Group1,Group2" \
--data-urlencode "data[ip_opt]"="10.20.10.30" \
--data-urlencode "data[ip_signup]"="10.20.10.30" \
-w %{time_total}

# invalid webhooks path
curl -i -X POST http://localhost:7070/webhooks/invalid?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
--data-urlencode "type"="subscribe" \
--data-urlencode "fired_at"="2009-03-26 21:35:57" \
--data-urlencode "data[id]"="8a25ff1d98" \
--data-urlencode "data[list_id]"="a6b5da1054" \
--data-urlencode "data[email]"="api@mailchimp.com" \
--data-urlencode "data[email_type]"="html" \
--data-urlencode "data[merges][EMAIL]"="api@mailchimp.com" \
--data-urlencode "data[merges][FNAME]"="MailChimp" \
--data-urlencode "data[merges][LNAME]"="API" \
--data-urlencode "data[merges][INTERESTS]"="Group1,Group2" \
--data-urlencode "data[ip_opt]"="10.20.10.30" \
--data-urlencode "data[ip_signup]"="10.20.10.30" \
-w %{time_total}

# get normal
curl -i -X GET http://localhost:7070/webhooks/mailchimp?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
-w %{time_total}

# get invalid
curl -i -X GET http://localhost:7070/webhooks/invalid?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
-w %{time_total}
