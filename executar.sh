#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
java -jar "$DIR/target/sistema-vendas-1.0-SNAPSHOT-jar-with-dependencies.jar"
