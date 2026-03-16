#!/bin/bash

if [ -f "tpid" ]; then
    TPID=$(cat tpid)
    echo "Stopping process $TPID..."
    kill $TPID
    rm tpid
    echo "Stopped."
else
    echo "No tpid file found."
fi
