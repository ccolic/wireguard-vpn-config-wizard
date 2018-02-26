### Wireguard VPN Configuration Wizard
#
# You have successfully exported a Client-to-Client configuration
#
## Instructions
#
# You can use the generated files on any supported system to install Wireguard and setup the Client-to-Client VPN tunnel
# The configuration is archived in a password protected ZIP file. Please extract the files first.
# Copy the files to all your systems where you want to setup the VPN tunnel.
#
#
# 1.) If Wireguard is not yet installed on your system, run the 'install_wireguard.sh' script as root or with sudo
#     to install it: `sudo bash ./install_wireguard.sh`
#
# 2.) After verifying that Wireguard was installed correctly, you can run the 'setup_client1.sh' script on your client-1
#     system. This will configure Wireguard and setup IPs and routes accordingly
#
# 3.) To setup the other client, simply run the 'setup_client2.sh' script.
#
# After that, the tunnel should be setup and ready to use. You can check the Wireguard status by running `sudo wg`
# You can also try pinging the other site, to make sure the tunnel is up.