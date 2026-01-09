#!/bin/bash

numberOfRuns=10000000
neighbourPeakScalingFactor=2.0
acceptanceProbabilityScalingFactor=1.0
temperatureScalingFactor=100.0
temperatureDivisor=20000

# used for dynamic filename including the command-line arguments
logSuffix="${numberOfRuns}-${neighbourPeakScalingFactor}-${acceptanceProbabilityScalingFactor}-${temperatureScalingFactor}-${temperatureDivisor}"

mvn clean package

java -DlogSuffix="$logSuffix" -jar target/BlackHoleEvaporation-1.0-SNAPSHOT-shaded.jar \
    numberOfRuns "$numberOfRuns" \
    neighbourPeakScalingFactor "$neighbourPeakScalingFactor" \
    acceptanceProbabilityScalingFactor "$acceptanceProbabilityScalingFactor" \
    temperatureScalingFactor "$temperatureScalingFactor" \
    temperatureDivisor "$temperatureDivisor"
