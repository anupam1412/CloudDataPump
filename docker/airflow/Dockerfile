FROM python:3.6-slim

RUN apt-get update && apt-get install -y --no-install-recommends --fix-missing \
    gcc \
    procps \
    vim \
    iputils-ping \
    curl \
    default-libmysqlclient-dev \
    python2.7 \
    zip \
    unzip \
    bc \
    gawk \
    dos2unix \
 && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*


RUN pip install 'apache-airflow[gcp_api,google_auth]'==1.10.3

RUN pip install Flask==1.1.1 google-api-python-client==1.7.11 google-cloud-storage==1.26.0 \
    mock==3.0.5 google-cloud-bigquery==1.23.1 google-cloud-pubsub==1.1.0 \
    Werkzeug==0.16.1 pyhocon==0.3.54 pytest==4.6.9

RUN apt-get update && apt-get install -y --no-install-recommends --fix-missing \
    sqlite3

COPY entrypoint.sh /usr/local/bin/
ENTRYPOINT ["entrypoint.sh"]
