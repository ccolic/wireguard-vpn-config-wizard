#!/usr/bin/env bash
# This script will detect the running linux distribution and
# install Wireguard with the according steps
# running as root (preferably with sudo) is required

ubuntu_install() {
	echo "### Starting Wireguard installation for Ubuntu"
	if type add-apt-repository >/dev/null 2>&1; then
		echo "### 'add-apt-repository' is installed"
	else
		echo "### 'add-apt-repository is missing'. Need to install it."
		echo "### Running 'apt-get update' ..."
		apt-get update -qq
		apt-get install software-properties-common python-software-properties
	fi
	echo "### Adding PPA wireguard/wireguard"
	add-apt-repository ppa:wireguard/wireguard
	echo "### Running 'apt-get update' ..."
	apt-get update -qq
	apt-get install wireguard-dkms wireguard-tools
	if [[ $? -eq 0 ]]; then
		echo "### Wireguard was installed successfully."
	fi
	exit
}

debian_install() {
	echo "### Starting Wireguard installation for Debian"
	echo "deb http://deb.debian.org/debian/ unstable main" > /etc/apt/sources.list.d/unstable-wireguard.list
	printf 'Package: *\nPin: release a=unstable\nPin-Priority: 150\n' > /etc/apt/preferences.d/limit-unstable
	echo "### Running 'apt-get update' ..."
	apt-get update -qq
	apt-get install wireguard-dkms wireguard-tools
	if [[ $? -eq 0 ]]; then
		echo "### Wireguard was installed successfully."
	fi
	exit
}

fedora_install() {
	echo "### Starting Wireguard installation for Fedora"
	dnf install 'dnf-command(copr)'
	echo "### Enabling 3rd-party COPR repository 'jdoss/wireguard'"
	dnf copr enable jdoss/wireguard
	dnf install wireguard-dkms wireguard-tools
	if [[ $? -eq 0 ]]; then
		echo "### Wireguard was installed successfully."
	fi
	exit
}

centos_install() {
	echo "### Starting Wireguard installation for CentOS/Redhat"
	echo "### Adding Wireguard yum repository"
	curl -Lo /etc/yum.repos.d/wireguard.repo https://copr.fedorainfracloud.org/coprs/jdoss/wireguard/repo/epel-7/jdoss-wireguard-epel-7.repo
	echo "### Installing the EPEL repository"
	yum install epel-release
	yum install wireguard-dkms wireguard-tools
	if [[ $? -eq 0 ]]; then
		echo "### Wireguard was installed successfully."
	fi
	exit
}

arch_install() {
	echo "### Starting Wireguard installation for Arch"
	pacman -Syyq
	pacman -S wireguard-dkms wireguard-tools
	if [[ $? -eq 0 ]]; then
		echo "### Wireguard was installed successfully."
	fi
	exit
}

not_supported() {
	echo "Could not determine your distribution or you are running a not supported version"
	echo "Please check the official Wireguard documentation for installation instructions specific to your OS"
	echo "===> https://www.wireguard.com/install/"
	exit
}

# check for root
if [ "$EUID" -ne 0 ]; then
	echo "### Please run as root or with sudo"
	exit
fi

# set shell option to match patterns case-insensitive
shopt -s nocasematch

# determine distro
if [ -f /etc/os-release ]; then
	# works for most newer distros
    . /etc/os-release
    OS=$ID
elif type lsb_release >/dev/null 2>&1; then
	# works for most newer debian/ubuntu based distros
    OS=$(lsb_release -si)
elif [ -f /etc/lsb-release ]; then
    # For some versions of Debian/Ubuntu without lsb_release command
    . /etc/lsb-release
    OS=$DISTRIB_ID
elif [ -f /etc/arch-release ]; then
    # Older Debian/Ubuntu/etc.
    OS="arch"
elif [ -f /etc/debian_version ]; then
    # Older Debian/Ubuntu/etc.
    OS="debian"
elif [ -f /etc/centos-release ]; then
    # Older Fedora
	OS="centos"
elif [ -f /etc/fedora-release ]; then
    # Older Fedora
	OS="fedora"
elif [ -f /etc/redhat-release ]; then
    # Older Red Hat, CentOS, etc.
    OS="redhat"
else
	not_supported
fi

# call the appropriate function depending on the distro
case "$OS" in
	"ubuntu" )
		ubuntu_install ;;
	"debian" )
		debian_install ;;
	"fedora" )
		fedora_install ;;
	"centos" )
		centos_install ;;
	"redhat" )
		# redhat and centos use the same installation steps
		centos_install ;;
	"arch" )
		arch_install ;;
	*)
		not_supported ;;
esac
