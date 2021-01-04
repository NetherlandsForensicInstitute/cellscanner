#!/bin/bash

STRINGS=app/src/main/res/values/version_info.xml

echo '<resources>
    <string name="build_date">'$(date)'</string>
</resources>
' >$STRINGS
