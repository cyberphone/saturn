#!/bin/sh

set -e

DIR="$( cd "$( dirname "$0" )" && pwd )"

# adjust the line below to your installation
NODE_PATH=/home/server/node-webpki.org-master/node_modules

node $DIR/acquirer.js

