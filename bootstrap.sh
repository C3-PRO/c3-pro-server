#!/usr/bin/env bash

# Set the proxy to enable http connections from the VM
# Only enable the line if inside BCH, or replace proxy properly
# export http_proxy=http://proxy.tch.harvard.edu:3128

# Install basic tools
sudo apt-get clean
sudo apt-get update
sudo apt-get install openjdk-7-jdk
sudo apt-get install unzip
sudo apt-get install maven

# To be changed in the near future.
# We should move this to the pom.xml of the project
cp /vagrant/settings.xml /home/vagrant/.m2

# Install jboss as7
wget http://download.jboss.org/jbossas/7.1/jboss-as-7.1.1.Final/jboss-as-7.1.1.Final.zip
sudo unzip jboss-as-7.1.1.Final.zip -d /usr/share/
sudo chown -fR vagrant:vagrant /usr/share/jboss-as-7.1.1.Final/

