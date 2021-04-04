# cambium.core

Core module for the Cambium logging API.


## Clojars coordinates

`[cambium/cambium.core "1.1.0"]`


## Documentation

Cambium documentation: https://cambium-clojure.github.io/


## Development

Running tests requires few Leiningen profiles (see `project.clj` for available ones):
```shell
$ lein do clean, with-profile <clojure-version>,<slf4j-impl>[,<other-profile>..] test
```

Examples:
```shell
$ lein do clean, with-profile c06,logback test
$ lein do clean, with-profile c07,log4j2 test
```

Running tests across all supported Clojure versions:
```shell
$ lein do clean, test-all-logback
$ lein do clean, test-all-log4j12
$ lein do clean, test-all-log4j2
```


## License

Copyright © 2017-2021 [Shantanu Kumar](https://github.com/kumarshantanu)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
