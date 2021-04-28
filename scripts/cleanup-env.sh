gsutil rm -r gs://$DEVSHELL_PROJECT_ID
gsutil rm -r gs://dataflow*
bq rm -f --table $DEVSHELL_PROJECT_ID:events.stream_appointments
bq rm -f --table $DEVSHELL_PROJECT_ID:events.batch_appointments
bq rm -f --dataset $DEVSHELL_PROJECT_ID:events
gcloud pubsub subscriptions delete xml-sub
gcloud pubsub subscriptions delete json-sub
gcloud pubsub topics delete xml-appointments
gcloud pubsub topics delete json-appointments
