#!/bin/bash
test -d BuildLogMsg && \
    (mv Build/ BuildInit/ && mv BuildLogMsg/ Build/ && echo Swapping to Full Build) || \
    (mv Build/ BuildLogMsg/ && mv BuildInit/ Build/ && echo Swapping to Truncated Build)
