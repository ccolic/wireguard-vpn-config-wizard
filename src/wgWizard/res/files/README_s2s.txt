### Wireguard VPN Configuration Wizard
#
# You have successfully exported a Site-to-Site configuration
#
## Instructions
#
# You can use the generated files on any supported system to install Wireguard and setup the Site-to-Site VPN tunnel
# The configuration is archived in a password protected ZIP file. Please extract the files first.
# Copy the files to both your systems where you want to setup the VPN tunnel.
#
#
# 1.) If Wireguard is not yet installed on your system, run the 'install_wireguard.sh' script as root or with sudo
#     to install it: `sudo bash ./install_wireguard.sh`
#
# 2.) After verifying that Wireguard was installed correctly, you can run the 'setup_site1.sh' script on your site-1
#     system. This will configure Wireguard and setup IPs and routes accordingly
#
# 3.) Do the same on your site-2 and run 'setup_site2.sh'
#
# After that, the tunnel should be setup and ready to use. You can check the Wireguard status by running `sudo wg`
# You can also try pinging the other site, to make sure the tunnel is up.