# Build and install MongoDB
FROM centos:centos7
MAINTAINER The CentOS Project <cloud-ops@centos.org>
RUN yum -y update; yum clean all
RUN yum -y install epel-release; yum clean all
RUN yum -y install mongodb-server; yum clean all
RUN mkdir -p /data/db
EXPOSE 27017
ENTRYPOINT ["/usr/bin/mongod"]
RUN /etc/init.d/mongod start

#FROM registry.eu-gb.bluemix.net/ibmliberty
FROM registry.ng.bluemix.net/ibmliberty
COPY server.xml /opt/ibm/wlp/usr/servers/defaultServer/
ADD bluebox.war /opt/ibm/wlp/usr/servers/defaultServer/apps/
ENV LICENSE accept