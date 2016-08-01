import subprocess

SUPPRESS_STDOUT=False
SUPPRESS_STDERR=False
LOGGER_NAME='INT_TESTS'

def std_out():
  if SUPPRESS_STDOUT:
    return subprocess.DEVNULL
  else:
    return None

def std_err():
  if SUPPRESS_STDERR:
    return subprocess.DEVNULL
  else:
    return None
