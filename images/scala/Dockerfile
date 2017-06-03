FROM java:latest

MAINTAINER "Benjamin Schubert <ben.c.schubert@gmail.com>"

RUN echo "deb http://dl.bintray.com/sbt/debian /" > /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    apt-get update > /dev/null && \
    apt-get upgrade -y > /dev/null && \
    apt-get install -y sbt > /dev/null


ADD . /srv/app

WORKDIR /srv/app

CMD ["sbt", "run"]