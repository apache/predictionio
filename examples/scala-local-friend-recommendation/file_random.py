import sys
import random

read_file = open("data/user_profile.txt", 'r')
write_file = open("data/mini_user_key_word.txt", 'w')
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
user_id_list.sort()

user_id_list = map(str, user_id_list)
print user_id_list
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

#getting the user_sns_small
"""
read_file = open("user_sns.txt", 'r')
write_file = open("user_sns_small.txt", 'w')
index = 0
met = False
count = 0
for line in read_file:
    count += 1
    print count
    if met:
        if line.split()[0] != user_id_list[index]:
            index += 1
            met = False
            if index == len(user_id_list):
                break
    if line.split()[0] == user_id_list[index]:
        print "here"
        write_file.write(line)
        met = True
"""




