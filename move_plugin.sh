#!/bin/bash
inotifywait  -e create,moved_to,attrib --include '/UnPure-1.0.jar' -qq target
sleep 1
mv -f target/UnPure-1.0.jar server/plugins/UnPure-1.0.jar