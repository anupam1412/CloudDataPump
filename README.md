# Cloud-Data-Pump

 A unified data ingestion framework built specifically for Google Cloud Platform.

# Introduction

- Developed on Google Cloud Dataflow (Apache Beam - 2.19.0) 

- It has the capability to handle both batch and streaming data ingestion modes with multiple file format types like JSON/XML/CSV/AVRO  etc.

- It uses below tech stack from Google Cloud Platform

	- Google Dataflow
	- Google Pub/Sub
	- Google BigQuery
	- Google Cloud Storage
	

![Design](https://github.com/mohemed2087/cloud-data-pump/blob/feature/xml-changes/design/gcp-pipe.png)

	
# Pre-requisites

  - Enable respective services
  
        $ gcloud services enable dataflow.googleapis.com
        $ gcloud services enable pubsub.googleapis.com
  
  - Create PubSub topic 
  
        $ gcloud pubsub topics create json-appointments
        $ gcloud pubsub topics create xml-appointments
        
  - Create subscription
  
        $ gcloud pubsub subscriptions create xml-sub --topic xml-appointments --topic-project $DEVSHELL_PROJECT_ID
        $ gcloud pubsub subscriptions create json-sub --topic json-appointments --topic-project $DEVSHELL_PROJECT_ID	
      
  - Create BQ dataset and table
  
        $ bq mk --dataset $DEVSHELL_PROJECT_ID:events
        $ bq mk  --schema schema/bq_stream_schema_appointments.json --table $DEVSHELL_PROJECT_ID:events.stream_appointments
        $ bq mk  --schema schema/bq_batch_schema_appointments.json --table $DEVSHELL_PROJECT_ID:events.batch_appointments
      
  - Create staging bucket
  
        $ gsutil mb gs://$DEVSHELL_PROJECT_ID
        
   - Build package
  
            $ mvn package

# Create Dataflow template
  
  # JSON Stream Template
  
	  $ java  -jar target/cloud-data-pump-1.0-SNAPSHOT.jar \
            --project=$DEVSHELL_PROJECT_ID \
            --stagingLocation=gs://${DEVSHELL_PROJECT_ID}/staging/stream/json \
            --tempLocation=gs://${DEVSHELL_PROJECT_ID}/temp --runner=DataflowRunner \
            --templateLocation=gs://${DEVSHELL_PROJECT_ID}/templates/stream/json/cloud-data-pump.json \
            --workerMachineType=n1-standard-1 \
            --numWorkers=1 \
            --ingestionMode=json_stream

  # CSV Batch Template
      
      $ java  -jar target/cloud-data-pump-1.0-SNAPSHOT.jar \
          --project=$DEVSHELL_PROJECT_ID \
          --stagingLocation=gs://${DEVSHELL_PROJECT_ID}/staging/batch/csv \
          --tempLocation=gs://${DEVSHELL_PROJECT_ID}/temp --runner=DataflowRunner \
          --templateLocation=gs://${DEVSHELL_PROJECT_ID}/templates/batch/csv/cloud-data-pump.json \
          --workerMachineType=n1-standard-1 \
          --numWorkers=1 \
          --ingestionMode=csv_batch 
  
  # AVRO  Batch Template
		
	    $ java  -jar target/cloud-data-pump-1.0-SNAPSHOT.jar \
            --project=$DEVSHELL_PROJECT_ID \
            --stagingLocation=gs://${DEVSHELL_PROJECT_ID}/staging/batch/avro \
            --tempLocation=gs://${DEVSHELL_PROJECT_ID}/temp --runner=DataflowRunner \
            --templateLocation=gs://${DEVSHELL_PROJECT_ID}/templates/batch/avro/cloud-data-pump.json \
            --workerMachineType=n1-standard-1 \
            --numWorkers=1 \
            --ingestionMode=avro_batch
	    
  # XML Stream Template
  
  	  $ java  -jar target/cloud-data-pump-1.0-SNAPSHOT.jar \
        --project=$DEVSHELL_PROJECT_ID \
        --stagingLocation=gs://${DEVSHELL_PROJECT_ID}/staging/stream/xml \
        --tempLocation=gs://${DEVSHELL_PROJECT_ID}/temp --runner=DataflowRunner \
        --templateLocation=gs://${DEVSHELL_PROJECT_ID}/templates/stream/xml/cloud-data-pump.json \
        --workerMachineType=n1-standard-1 \
        --numWorkers=1 \
        --ingestionMode=xml_stream
		
 # Run Dataflow job

   # JSON Streaming
    
        $ gsutil cp dataflow-conf/json-stream-template.conf gs://$DEVSHELL_PROJECT_ID/confs/
        
	    $ gcloud dataflow jobs run data-pump-json-stream \
               --project=$DEVSHELL_PROJECT_ID \
               --gcs-location=gs://${DEVSHELL_PROJECT_ID}/templates/stream/json/cloud-data-pump.json \
               --region=europe-west1 \
               --parameters config=gs://${DEVSHELL_PROJECT_ID}/confs/json-stream-template.conf,subscription=projects/$DEVSHELL_PROJECT_ID/subscriptions/json-sub,ingestionMode=json_stream
               
   # CSV Batch
        
            $ gsutil cp dataflow-conf/csv-batch-template.conf gs://$DEVSHELL_PROJECT_ID/confs/
            $ gsutil cp src/test/resources/data/batch/csv/appointments.csv gs://$DEVSHELL_PROJECT_ID/csv/appointments/
            
    	    $ gcloud dataflow jobs run data-pump-csv-batch \
                   --project=$DEVSHELL_PROJECT_ID \
                   --gcs-location=gs://${DEVSHELL_PROJECT_ID}/templates/batch/csv/cloud-data-pump.json  \
                   --region=europe-west1 \
                   --parameters config=gs://${DEVSHELL_PROJECT_ID}/confs/csv-batch-template.conf,filePath=gs://$DEVSHELL_PROJECT_ID/csv/appointments/appointments.csv,ingestionMode=csv_batch

   # AVRO Batch
        
            $ gsutil cp dataflow-conf/avro-batch-template.conf gs://$DEVSHELL_PROJECT_ID/confs/
            $ gsutil cp src/test/resources/data/batch/avro/appointments.avro gs://$DEVSHELL_PROJECT_ID/avro/appointments/
            
    	    $ gcloud dataflow jobs run data-pump-avro-batch \
                   --project=$DEVSHELL_PROJECT_ID \
                   --gcs-location=gs://${DEVSHELL_PROJECT_ID}/templates/batch/avro/cloud-data-pump.json \
                   --region=europe-west1 \
                   --parameters config=gs://${DEVSHELL_PROJECT_ID}/confs/avro-batch-template.conf,filePath=gs://$DEVSHELL_PROJECT_ID/avro/appointments/appointments.avro,ingestionMode=avro_batch	
		   
   # XML Stream
   
   	     $ gsutil cp dataflow-conf/xml-stream-template.conf gs://$DEVSHELL_PROJECT_ID/confs/
	     
	     $ gcloud dataflow jobs run data-pump-xml-stream \
           --project=$DEVSHELL_PROJECT_ID \
           --gcs-location=gs://${DEVSHELL_PROJECT_ID}/templates/stream/xml/cloud-data-pump.json \
           --region=europe-west1 \
           --parameters config=gs://${DEVSHELL_PROJECT_ID}/confs/xml-stream-template.conf,subscription=projects/$DEVSHELL_PROJECT_ID/subscriptions/xml-sub,ingestionMode=xml_stream
                   
# Publish message to Pub/Sub topic
        
        $ cd scripts
        $ chmod +x *
        $ ./pubsub-publish.sh ../src/test/resources/data/streaming/json/appointments.json json-appointments
	$ ./pubsub-publish.sh ../src/test/resources/data/streaming/xml/appointments.xml xml-appointments

# Cleanup

		$ gsutil rm -r gs://$DEVSHELL_PROJECT_ID 
		$ gsutil rm -r gs://dataflow*
		$ bq rm --table $DEVSHELL_PROJECT_ID:events.batch_appointments
		$ bq rm --table $DEVSHELL_PROJECT_ID:events.stream_appointments
		$ bq rm --dataset $DEVSHELL_PROJECT_ID:events
		$ gcloud pubsub subscriptions delete xml-sub
		$ gcloud pubsub subscriptions delete json-sub
		$ gcloud pubsub topics delete xml-appointments
		$ gcloud pubsub topics delete json-appointments
