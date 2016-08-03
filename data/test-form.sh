#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

accessKey=$1

# normal subscribe event
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

# normal unsubscribe event
curl -i -X POST http://localhost:7070/webhooks/mailchimp?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
--data-urlencode "type"="unsubscribe" \
--data-urlencode "fired_at"="2009-03-26 21:40:57" \
--data-urlencode "data[action]"="unsub" \
--data-urlencode "data[reason]"="manual" \
--data-urlencode "data[id]"="8a25ff1d98" \
--data-urlencode "data[list_id]"="a6b5da1054" \
--data-urlencode "data[email]"="api+unsub@mailchimp.com" \
--data-urlencode "data[email_type]"="html" \
--data-urlencode "data[merges][EMAIL]"="api+unsub@mailchimp.com" \
--data-urlencode "data[merges][FNAME]"="MailChimp" \
--data-urlencode "data[merges][LNAME]"="API" \
--data-urlencode "data[merges][INTERESTS]"="Group1,Group2" \
--data-urlencode "data[ip_opt]"="10.20.10.30" \
--data-urlencode "data[campaign_id]"="cb398d21d2" \
-w %{time_total}

# normal profile update event
curl -i -X POST http://localhost:7070/webhooks/mailchimp?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
--data-urlencode "type"="profile" \
--data-urlencode "fired_at"="2009-03-26 21:31:21" \
--data-urlencode "data[id]"="8a25ff1d98" \
--data-urlencode "data[list_id]"="a6b5da1054" \
--data-urlencode "data[email]"="api@mailchimp.com" \
--data-urlencode "data[email_type]"="html" \
--data-urlencode "data[merges][EMAIL]"="api@mailchimp.com" \
--data-urlencode "data[merges][FNAME]"="MailChimp" \
--data-urlencode "data[merges][LNAME]"="API" \
--data-urlencode "data[merges][INTERESTS]"="Group1,Group2" \
--data-urlencode "data[ip_opt]"="10.20.10.30" \
-w %{time_total}

# normal email update event
curl -i -X POST http://localhost:7070/webhooks/mailchimp?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
--data-urlencode "type"="upemail" \
--data-urlencode "fired_at"="2009-03-26 22:15:09" \
--data-urlencode "data[list_id]"="a6b5da1054" \
--data-urlencode "data[new_id]"="51da8c3259" \
--data-urlencode "data[new_email]"="api+new@mailchimp.com" \
--data-urlencode "data[old_email]"="api+old@mailchimp.com" \
-w %{time_total}

# normal cleaned email event
curl -i -X POST http://localhost:7070/webhooks/mailchimp?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
--data-urlencode "type"="cleaned" \
--data-urlencode "fired_at"="2009-03-26 22:01:00" \
--data-urlencode "data[list_id]"="a6b5da1054" \
--data-urlencode "data[campaign_id]"="4fjk2ma9xd" \
--data-urlencode "data[reason]"="hard" \
--data-urlencode "data[email]"="api+cleaned@mailchimp.com" \
-w %{time_total}

# normal campaign sending status event
curl -i -X POST http://localhost:7070/webhooks/mailchimp?accessKey=$accessKey \
-H "Content-type: application/x-www-form-urlencoded" \
--data-urlencode "type"="campaign" \
--data-urlencode "fired_at"="2009-03-26 21:31:21" \
--data-urlencode "data[id]"="5aa2102003" \
--data-urlencode "data[subject]"="Test Campaign Subject" \
--data-urlencode "data[status]"="sent" \
--data-urlencode "data[reason]"="" \
--data-urlencode "data[list_id]"="a6b5da1054" \
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
