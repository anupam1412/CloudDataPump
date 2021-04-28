#!/bin/bash
today=`date '+%Y-%m-%d'`
uid=`cat /proc/sys/kernel/random/uuid`
gcloud pubsub topics publish projects/$DEVSHELL_PROJECT_ID/topics/$2 --message "$(cat $1)" --attribute MessageId=${uid},TopicName="projects/data-pump-sandboxed/topics/$2",LogicalDate=${today}
