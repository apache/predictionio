#!/usr/bin/python
import sys
print 'argument list:', str(sys.argv)
#second argument is the file name that we write to
f = open(sys.argv[2], 'w')
line_number_to_start_read = int(sys.argv[3])
line_number_to_end_read = int(sys.argv[4])

# first argument is the name and path of the file to open
with open (sys.argv[1]) as File:
  x = 0
  for line in File:
    if line_number_to_start_read <= x: 
      f.write(line)
      print x
    if x >= line_number_to_end_read-1:
      print x
      break
    x = x + 1
  f.close()
