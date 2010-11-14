#!/bin/bash

xtitle "Client to Java Settler's of Catan!"

PORT=8881

echo "Starting Java Settlers of Catan Client..."

OLD_DIR=`pwd`
cd target/classes

java soc.client.PlayerClient localhost $PORT

cd "$OLD_DIR"
