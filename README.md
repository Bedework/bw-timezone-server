# bw-timezone-server [![Build Status](https://travis-ci.org/Bedework/bw-timezone-server.svg)](https://travis-ci.org/Bedework/bw-timezone-server)

A Java implementation of the tzdist protocol - [RFC7808](https://tools.ietf.org/html/rfc7808)

## Changes needed
   *  Conversion program needs a message output function so we can add all output to console
   *  Conversion fails with exception if cannot compare - add a message 

## To check
   *  Etags - on load same value as dtstamp - should it be something else?

## Requirements

1. JDK 11
2. Maven 3

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release use the release script:

> ./bedework/build/quickstart/linux/util-scripts/release.sh <module-name> "<release-version>" "<new-version>-SNAPSHOT"

When prompted, indicate all updates are committed

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).

## Release Notes
### 4.0.6
* Update library versions

#### 4.0.7
* Update library versions

#### 4.0.8
* Update library versions
* Avoid issue with TemporalAmountAdaptor
* Slight refactor and remove warnings
* Use new no-fetch timezone registry for timezone server.

#### 4.0.9
* Update library versions

#### 4.0.10
* Update library versions
* Fixes to AbstractDb and H2 implementation
* Remove all traces of leveldb

#### 4.0.11
* Update library versions
* Partially completed changes to better handle aliases.

#### 4.0.12
* Update library versions

#### 4.0.13
* Update library versions

#### 5.0.0
* Use bedework-parent for builds
*  Upgrade library versions
* Use aretfacts to deploy timezone server data and calendar h2 data.

#### 5.0.1
*  Upgrade library versions
* Fail with 500 if no config
* Deploy as war not ear

#### 5.0.2
* Update parent

#### 5.0.3
* 5.0.2 skipped after failed release

#### 5.0.4
* Move timezone xml into this project out of bw-xml

#### 5.0.3
* 5.0.4 skipped after failed release due to missing version in poms.

