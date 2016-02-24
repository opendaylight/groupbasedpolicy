#!/usr/bin/env bash

if [ ! -d /home/vagrant/sflow-rt ]; then
  echo "/home/vagrant/sflow-rt not found; installing sFlow-RT..."
  wget http://www.inmon.com/products/sFlow-RT/sflow-rt.tar.gz
  tar -xvzf sflow-rt.tar.gz
  sudo chown -R vagrant:vagrant /home/vagrant/sflow-rt
fi

if [ ! -f /home/vagrant/sflow-rt/gbp_start.sh ]; then
  sed '/exec java/c sudo nohup java ${JVM_OPTS} ${RT_OPTS} ${SCRIPTS} -jar ${JAR} 1>rt.out 2>&1 &' ~/sflow-rt/start.sh >/home/vagrant/sflow-rt/gbp_start.sh
  sudo chmod +x /home/vagrant/sflow-rt/gbp_start.sh
fi
