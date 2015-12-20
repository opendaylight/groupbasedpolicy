#!/usr/bin/env bash

sudo kill $(ps -fe | grep port=6343 | grep -v grep | awk 'NR==1 {print $2}')
sudo rm /home/vagrant/sflow-rt/rt.out.old
sudo mv /home/vagrant/sflow-rt/rt.out /home/vagrant/sflow-rt/rt.out.old
