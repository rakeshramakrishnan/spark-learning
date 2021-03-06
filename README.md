# Intro
Assuming that local kubernetes is running, along with tiller

# Setup spark operator
```
$ helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator
$ helm install --name spark incubator/sparkoperator --set sparkJobNamespace=default --set enableWebhook=true --set webhookPort=443 
```

# Build local project into docker image
```
$ ./gradlew build
$ docker build -t=sample-spark-job-image .
```

# Deploy docker image onto k8
```
kubectl apply -f spark-operator-submit.yaml
```

# Monitor application via UI of driver
```
kubectl port-forward service/spark-pi-custom-ui-svc 4040:4040
```
Note that name of application = spark-pi-custom in above example. It is set under `metadata.name` under `spark-operator-submit.yaml`

# Monitor application by describing SparkApplication
```
kubectl describe sparkapplication spark-pi-custom
```

# Delete the spark application
```
kubectl delete sparkapplication spark-pi-custom
```

# Delete spark operator
```
helm del spark --purge
```

# Kafka on k8
## Helm chart
```
helm install --name kafka bitnami/kafka --set deleteTopicEnable=true
```

## Kafka producer
```
kubectl exec -ti kafka-0 -- kafka-console-producer.sh --topic word-count-input --broker-list localhost:9092
```

## Kafka consumer
```
kubectl exec -ti kafka-0 -- kafka-console-consumer.sh --topic word-count-input --bootstrap-server localhost:9092
```
