# Annot8 CoreNLP Components

This project contains components for the [Annot8 Data Processing Framework](https://github.com/annot8)
which use the [Stanford CoreNLP](https://stanfordnlp.github.io/CoreNLP/) libraries to perform Natural
Language Processing tasks such as Named Entity Recognition and Relation extraction.

## Building

This project uses the Maven build system, and standard Maven commands such as `mvn install` can be used.

To build a shaded version of the Annot8 CoreNLP Components suitable for use with [Baleen 3](https://github.com/dstl/baleen3),
you can run the following command:

```
mvn -Pplugins package
```

## Licence

The code in this repository is licenced under a [GPLv3 Licence](LICENSE).