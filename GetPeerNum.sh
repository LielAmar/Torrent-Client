echo | cat RunDir/PeerInfo.cfg | grep `hostname` | awk '{print $1;}'

