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

# WARNING: THIS DOCKERFILE IS NOT INTENDED FOR PRODUCTION USE OR DEPLOYMENT. AT
#          THIS POINT, THIS IS ONLY INTENDED FOR USE IN AUTOMATED TESTS. IF YOU
#          ARE LOOKING TO DEPLOY PREDICTIONIO WITH DOCKER, PLEASE REFER TO
#          http://predictionio.apache.org/community/projects/#docker-installation-for-predictionio

FROM predictionio/pio-testing-base

# Include the entire code tree
ENV PIO_HOME /PredictionIO
ENV PATH ${PIO_HOME}/bin/:${PATH}
ADD . ${PIO_HOME}
