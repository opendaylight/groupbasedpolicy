CONTROLLER=192.168.56.1
echo
echo "*** Removing containers... "
echo
./docker-clean.sh
echo
echo "*** Cleaning up OVS... "
mn -c
echo
echo "Pulling alagalah/odlpoc_ovs230 docker image...edit script for own images"
echo
docker pull alagalah/odlpoc_ovs230
echo
echo "Running POC script"
echo
./testOfOverlay.py --local s1 --controller ${CONTROLLER}
