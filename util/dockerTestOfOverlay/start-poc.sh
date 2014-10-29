CONTROLLER=192.168.56.1
echo
echo "*** Removing containers... "
echo
sudo ./docker-clean.sh
echo
echo "*** Cleaning up OVS... "
sudo mn -c
echo
echo "Running POC script"
echo
sudo ./testOfOverlay.py --local s1 --controller ${CONTROLLER}
