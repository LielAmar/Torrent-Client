#!/bin/bash

ps -f -u mvandenboom | grep java | grep -v grep | awk '{print $2}' | xargs -rp kill

