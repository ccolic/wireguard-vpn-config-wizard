#!/usr/bin/env bash

# Enable IP routing
echo "net.ipv4.ip_forward=1" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p /etc/sysctl.conf

exit 0
