#!/bin/bash
inotifywait  -e create,moved_to,attrib --include '/UnPure-1.0-SNAPSHOT.jar' -qq target
sleep 1
mv -f target/UnPure-1.0-SNAPSHOT.jar server/plugins/UnPure-1.0-SNAPSHOT.jar