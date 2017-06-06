FROM java:latest

MAINTAINER "Benjamin Schubert <ben.c.schubert@gmail.com>"

RUN apt-get update > /dev/null && \
    apt-get -qq install apt-transport-https -y > /dev/null && \
    echo "deb http://dl.bintray.com/sbt/debian /" > /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    echo "deb https://deb.nodesource.com/node_6.x jessie main" >> /etc/apt/sources.list.d/node.list && \
    wget -qO- https://deb.nodesource.com/gpgkey/nodesource.gpg.key | apt-key add - && \
    apt-get update > /dev/null && \
    apt-get upgrade -y > /dev/null && \
    apt-get install -y sbt nodejs > /dev/null


ADD ./images/scala/bootstrap /srv/bootstrap

RUN cd /srv/bootstrap && sbt run && cd ../ && rm -rf bootstrap

ADD . /srv/app

WORKDIR /srv/app

RUN sbt clean stage

CMD ["rm", "-f", "/srv/app/server/target/universal/stage/RUNNING_PID", "&&", "./server/target/universal/stage/bin/server"]
