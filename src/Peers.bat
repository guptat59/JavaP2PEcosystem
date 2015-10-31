@ECHO OFF
javac Peer.java
start "Peer1" java Peer 8000 8444 8445
start "Peer2" java Peer 8000 8445 8446
start "Peer3" java Peer 8000 8446 8447
start "Peer4" java Peer 8000 8447 8448
start "Peer5" java Peer 8000 8448 8449
start "Peer6" java Peer 8000 8449 8450
start "Peer7" java Peer 8000 8450 8451
start "Peer8" java Peer 8000 8451 8452
start "Peer9" java Peer 8000 8452 8453
start "Peer0" java Peer 8000 8453 8444
pause