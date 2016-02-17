sudo kill -9 `ps -ef | grep rabbit | grep -v grep | awk '{print $2}'`
