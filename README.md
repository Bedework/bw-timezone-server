# bw-timezone-server [![Build Status](https://travis-ci.org/Bedework/bw-timezone-server.svg)](https://travis-ci.org/Bedework/bw-timezone-server)

A Java implementation of the tzdist protocol - [RFC7808](https://tools.ietf.org/html/rfc7808)

## Changes needed
   *  Conversion program needs a message ouput function so we can add all output to console
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

To perform a new release:

> mvn -P bedework-dev release:clean release:prepare

When prompted, select the desired version; accept the defaults for scm tag and next development version.
When the build completes, and the changes are committed and pushed successfully, execute:

> mvn -P bedework-dev release:perform

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).

## Release Notes
### 4.0.6
    * Dependency versions.
