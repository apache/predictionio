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

FROM predictionio/pio:latest

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update \
    && apt install -y build-essential curl git gcc make openssl libssl-dev libbz2-dev \
    apt-transport-https ca-certificates g++ gnupg graphviz lsb-release openssh-client zip \
    libreadline-dev libsqlite3-dev cmake libxml2-dev wget bzip2 sudo vim unzip locales \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN echo "en_US.UTF-8 UTF-8" > /etc/locale.gen && locale-gen

ENV LC_ALL=en_US.UTF-8 \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8 \
    NB_USER=jovyan \
    NB_UID=1000 \
    NB_GID=100 \
    CONDA_DIR=/opt/conda \
    PIP_DEFAULT_TIMEOUT=180
ENV PATH=$CONDA_DIR/bin:$PATH \
    HOME=/home/$NB_USER

ADD fix-permissions /usr/local/bin/fix-permissions
RUN chmod +x /usr/local/bin/fix-permissions \
    && groupadd wheel -g 11 \
    && echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su \
    && useradd -m -s /bin/bash -N -u $NB_UID $NB_USER \
    && mkdir -p $CONDA_DIR \
    && chmod g+w /etc/passwd \
    && fix-permissions $HOME \
    && fix-permissions $CONDA_DIR

USER $NB_USER

ENV MINICONDA_VERSION 4.4.10
RUN wget -q https://repo.continuum.io/miniconda/Miniconda3-${MINICONDA_VERSION}-Linux-x86_64.sh -O /tmp/miniconda.sh \
    && echo 'bec6203dbb2f53011e974e9bf4d46e93 */tmp/miniconda.sh' | md5sum -c - \
    && bash /tmp/miniconda.sh -f -b -p $CONDA_DIR \
    && rm /tmp/miniconda.sh \
    && conda config --system --prepend channels conda-forge \
    && conda config --system --set auto_update_conda false \
    && conda config --system --set show_channel_urls true \
    && conda install --quiet --yes conda="${MINICONDA_VERSION%.*}.*" \
    && conda update --all --quiet --yes \
    && conda clean -tipsy \
    && rm -rf /home/$NB_USER/.cache/yarn \
    && fix-permissions $CONDA_DIR \
    && fix-permissions /home/$NB_USER

RUN conda install --quiet --yes 'tini=0.18.0' \
    && conda list tini | grep tini | tr -s ' ' | cut -d ' ' -f 1,2 >> $CONDA_DIR/conda-meta/pinned \
    && conda clean -tipsy \
    && fix-permissions $CONDA_DIR \
    && fix-permissions /home/$NB_USER

RUN conda install --quiet --yes 'notebook=5.6.*' 'jupyterlab=0.34.*' nodejs\
    && jupyter labextension install @jupyterlab/hub-extension@^0.11.0 \
    && jupyter notebook --generate-config \
    && conda clean -tipsy \
    && npm cache clean --force \
    && rm -rf $CONDA_DIR/share/jupyter/lab/staging \
    && rm -rf /home/$NB_USER/.cache/yarn \
    && fix-permissions $CONDA_DIR \
    && fix-permissions /home/$NB_USER

ADD requirements.txt /tmp/requirements.txt
RUN pip --no-cache-dir install -r /tmp/requirements.txt \
    && fix-permissions $CONDA_DIR \
    && fix-permissions /home/$NB_USER

COPY jupyter_notebook_config.py /home/$NB_USER/.jupyter/
COPY start*.sh /usr/local/bin/

USER root
RUN chmod +x /usr/local/bin/*.sh

EXPOSE 8888
WORKDIR $HOME
ENTRYPOINT ["tini", "--"]
CMD ["/usr/local/bin/start-jupyter.sh"]

