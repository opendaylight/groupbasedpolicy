source openrc admin admin
glance image-create --name sf --disk-format vmdk --container-format bare --is-public True < /vagrant/SF.vmdk
openstack flavor create custom --ram 1024 --disk 15 --public
