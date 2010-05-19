#! /bin/bash

JSettlersDir="/usr/games/jsettlers-1.0.6"
NUM_PLAYERS=3

if ! [ -e /var/run/mysqld/mysqld.sock ]; then
	sudo mysqld &
fi

java -jar ${JSettlersDir}/JSettlersServer.jar 8880 10 root "" &
server_proc_id=$!
echo "Server Process ID: $server_proc_id"

sleep 5s

for ((num=1; num<=NUM_PLAYERS; num++)); do
	java -cp ${JSettlersDir}/JSettlersServer.jar soc.robot.SOCRobotClient localhost 8880 "Computer"${num} "" &
	CATANID[$num]=$!
	echo "Computer${num} Process ID: ${CATANID[$num]}"
done

java -jar ${JSettlersDir}/JSettlers.jar localhost 8880

for ((num=1; num<=NUM_PLAYERS; num++)); do
	echo "Cleaning up: killing ${CATANID[$num]}"
	kill ${CATANID[$num]};
done
echo "Cleaning up: killing $server_proc_id"; kill $server_proc_id;

exit 0