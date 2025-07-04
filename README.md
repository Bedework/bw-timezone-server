# bw-timezone-server

A Java implementation of the tzdist protocol - [RFC7808](https://tools.ietf.org/html/rfc7808)

## Changes needed
   *  Conversion program needs a message output function so we can add all output to console
   *  Conversion fails with exception if cannot compare - add a message 

## To check
   *  Etags - on load same value as dtstamp - should it be something else?

## Requirements

1. JDK 21
2. Maven 3


## Using this project
See documentation at [github pages for this project](https://bedework.github.io/bw-timezone-server/)

## Reporting Issues
Please report issues via the github issues tab at
> https://github.com/Bedework/bw-timezone-server/issues

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security - Vulnerability reporting
See [SECURITY.md](SECURITY.md).
