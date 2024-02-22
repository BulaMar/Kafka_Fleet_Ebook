# This repository contains source code examples for Kafka Fleet E-book

## Prerequisites

- Java version 21
- docker version 25.03 or higher
- docker compose version v2.24.5-desktop.1 or relevant on other distros
- [kind](https://kind.sigs.k8s.io/) installed
- [helm](https://helm.sh/) installed
- Confluent Cloud account with credits for cloud part

NOTE! Application was run and tested on Windows 11 with WSL2 enabled.
There is no certainty that it will work on Apple M1/M2 ARMs.

## Local set up

### Truck producer

First we will need our producer image so go to

`/Local set up/truck-producer/` and build `truck-producer`:

```bash
.\gradlew clean shadowJar
```

Next build image of it and name it `truck-producer`:

```bash
docker build --tag=truck-producer .
```

### Standalone Kafka cluster

Go to directory `/Local set up/Standalone Kafka cluster/`

In order to run the Kafka cluster locally execute:

```bash
docker compose -f kafka-compose.yaml up -d
```

Now we can run trucks-compose.yaml same way:

```bash
docker compose -f trucks-compose.yaml up -d
```

### Confluent platform

Go to `/Local set up/Confluent platform`.

First we need to create k8s cluster using `kind`.

```bash
kind create cluster –config cluster.yaml
```

Next we need to install CFK(Confluent For Kubernetes) with all CRDs (Custom resources definitions):

- add confluent repo

```bash
helm repo add confluentinc https://packages.confluent.io/helm
```

- update repositories

```bash
helm repo update
```

- create namespace and set it

```bash
kubectl create namespace confluent
```

```bash
kubectl config set-context --current --namespace confluent
```

- install confluent operator

```bash
helm upgrade --install \
  confluent-operator confluentinc/confluent-for-kubernetes \
  --set kRaftEnabled=true
```

- finally, install platform itself

```bash
kubectl apply -f confluent-platform.yaml
```

We can now run producer against it:

- but first `truck-poroducer` image has to be loaded into kind cluster:

```bash
kind load -n c1 docker-image truck_producer
```

- and now we can apply trucks.yaml:

```bash
kubectl apply -f trucks.yaml
```

## Confluent Cloud

To run all application against your confluent cloud first you need to fill all properties in resources with yours.

You can find all properties required in `new client` section.
Simply copy and paste them, you can find example [here](https://docs.confluent.io/cloud/current/client-apps/config-client.html).

Each application is simple java-gradle project, so you can either run it from your IDE or build and run using java jar:

```bash
./gradlew clean shadowJar
```

```bash
java -jar builded-app.jar
```
