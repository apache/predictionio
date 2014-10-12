f = open('my_file', 'w')

with open ('rec_log_train.txt') as File:
  x = 0
  for line in File:
    x = x + 1
    f.write(line)
    if x > 500:
      break

  f.close()
