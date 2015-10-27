#!/usr/bin/env bash

# Set the proxy to enable http connections from the VM
# Only enable the line if inside BCH, or replace proxy properly
# export http_proxy=http://proxy.tch.harvard.edu:3128

# Install basic tools
sudo apt-get clean
sudo apt-get -y update
sudo apt-get -y install openjdk-7-jdk
sudo apt-get -y install unzip
sudo apt-get -y install maven

# Install jboss as7
wget http://download.jboss.org/jbossas/7.1/jboss-as-7.1.1.Final/jboss-as-7.1.1.Final.zip
sudo unzip jboss-as-7.1.1.Final.zip -d /usr/share/
sudo chown -fR vagrant:vagrant /usr/share/jboss-as-7.1.1.Final/

