#!/usr/bin/env sh
mkdir -p out
javac -d out src/clinic/*.java && java -cp out clinic.Main
