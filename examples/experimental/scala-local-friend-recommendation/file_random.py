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

import sys
import random

read_file = open("data/user_profile.txt", 'r')
write_file = open("data/mini_user_profile.txt", 'w')
number_of_lines = int(sys.argv[1])
number_of_items = int(sys.argv[2])

#record number of lines
count = 0
random_num_list = []
# loop through the file to get number of lines in the file
for line in read_file:
    count += 1

print "generating random numbers"
# generating a list of random lines to read from
for i in range(0, number_of_lines):
    random_num_list.append(random.randint(0, count))

#get rid of any duplicates
no_duplicate_list = list(set(random_num_list))

#sort the list
no_duplicate_list.sort()
#print no_duplicate_list

#go to file begining
read_file.seek(0)
count = 0
index = 0
user_id_list = []
print "getting lines from user_profile"
for line in read_file:
    if count == no_duplicate_list[index]:
        write_file.write(line)
        index += 1
        user_id_list.append(int(line.split()[0]))
        if index == len(no_duplicate_list):
            break
    count += 1

#user_id_list is sorted

user_id_list = map(str, user_id_list)
user_id_list.sort()
#print user_id_list
print "user_id finished"

print "getting lines from item"
read_file = open("data/item.txt", 'r')
write_file = open("data/mini_item.txt", 'w')
count = 0
random_num_list = []
for line in read_file:
    count += 1

for i in range(0, number_of_items):
    random_num_list.append(random.randint(0, count))

#no duplicate
random_num_list = list(set(random_num_list))

random_num_list.sort()

read_file.seek(0)
count = 0
index = 0
item_id_list = []
for line in read_file:
    if count == random_num_list[index]:
        write_file.write(line)
        index += 1
        item_id_list.append(int(line.split()[0]))
        if index == len(random_num_list):
            break
    count += 1
print "item finished"

print "getting mini user_key_word"
read_file = open("data/user_key_word.txt", 'r')
write_file = open("data/mini_user_key_word.txt", 'w')

#record number of lines
count = 0
index = 0
# loop through the file to get number of lines in the file
for line in read_file:
    if line.split()[0] == user_id_list[index]:
        write_file.write(line)
        index += 1
        if index == len(user_id_list):
            #print "break"
            break
print "user keyword finished"
#go to file begining
#getting the user_sns_small

print "getting user sns"
#print user_id_list
read_file = open("data/user_sns.txt", 'r')

#write_file = open("data/mini_user_sns_small.txt", 'w')
user_sns_list = []
index = 0
met = False
count = 0
for line in read_file:
    count += 1
    #print count
    #Same user multiple following
    if met:
        if line.split()[0] != user_id_list[index]:
            index += 1
            met = False
            if index == len(user_id_list):
                break
    if line.split()[0] == user_id_list[index]:
        #print "here"
        user_sns_list.append(line)
        met = True
    # if the current line's user is greater than the user list, that means
    # the user doesn't follow or are following, then we move to next user
    if line.split()[0] > user_id_list[index]:
        index += 1
        if index == len(user_id_list):
            break

#print user_sns_list
write_file = open("data/mini_user_sns.txt",'w')
for line in user_sns_list:
    for user_id in user_id_list:
        if line.split()[1] == user_id:
            write_file.write(line)
            break
print "sns got"

print "getting user action"
#for line in write_file:
read_file = open("data/user_action.txt", 'r')
user_action_list = []
index = 0
met = False
count = 0
for line in read_file:
    count += 1
    #print count
    if met:
        if line.split()[0] != user_id_list[index]:
            index += 1
            met = False
            if index == len(user_id_list):
                break
    if line.split()[0] == user_id_list[index]:
        #print "here"
        user_action_list.append(line)
        met = True
    if line.split()[0] > user_id_list[index]:
        index += 1
        if index == len(user_id_list):
            break
#print user_action_list
write_file = open("data/mini_user_action.txt",'w')
for line in user_action_list:
    for user_id in user_id_list:
        if line.split()[1] == user_id:
            write_file.write(line)
            break
print "user action got"

print "getting rec_log_train"
user_set = set(user_id_list)
item_set = set(item_id_list)
read_file = open("data/rec_log_train.txt", 'r')
write_file = open("data/mini_rec_log_train.txt",'w')
count = 0
#for item in item_set:
#    print type(item)
#for user in user_set:
#    print type(user)
for line in read_file:
    words = line.split()
#    if words[0] in user_set and (words[1] in user_set or words[1] in item_set):
    if words[0] in user_set and words[1] in item_set:
        write_file.write(line)
    print count
    count += 1

print "Done"

