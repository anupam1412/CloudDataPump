gcloud pubsub topics create projects/data-pump-sandboxed/topics/json-appointments
gcloud pubsub topics create projects/data-pump-sandboxed/topics/xml-appointments
gcloud pubsub subscriptions create projects/data-pump-sandboxed/subscriptions/xml-sub --topic projects/data-pump-sandboxed/topics/xml-appointments --topic-project $DEVSHELL_PROJECT_ID
gcloud pubsub subscriptions create projects/data-pump-sandboxed/subscriptions/json-sub --topic projects/data-pump-sandboxed/topics/json-appointments --topic-project $DEVSHELL_PROJECT_ID
bq mk --dataset $DEVSHELL_PROJECT_ID:events
bq mk  --schema ../schema/bq_stream_schema_appointments.json --table $DEVSHELL_PROJECT_ID:events.stream_appointments
bq mk  --schema ../schema/bq_batch_schema_appointments.json --table $DEVSHELL_PROJECT_ID:events.batch_appointments
gsutil mb gs://$DEVSHELL_PROJECT_ID
