# cambium.core

Core module for the Cambium logging API.


## Clojars coordinates

`[cambium/cambium.core "0.9.1-SNAPSHOT"]`


## Documentation

Cambium documentation: https://cambium-clojure.github.io/


## Development

Running tests requires few Leiningen profiles (see `project.clj` for available ones):
```shell
$ lein do clean, with-profile <clojure-version>,<slf4j-impl>[,<other-profile>..] test
```

Examples:
```shell
$ lein do clean, with-profile c15,logback test
$ lein do clean, with-profile c15,log4j2,nested-test test
```


## License

Copyright © 2017 Shantanu Kumar (kumar.shantanu@gmail.com, shantanu.kumar@concur.com)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
