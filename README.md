# Salary Calculator Demo

This is a small coding exercise for an unspecified company as part of the
recruitment process. The exact specifications of the taks will, therefore,
remain undisclosed.

This is a Java based project that uses [Jetty] for HTTP, [Mithril] for
UI, [Fluid Tools] for packaging and dependency injection, and [Maven] to
build.

  [Maven]: https://maven.apache.org/
  [Jetty]: http://www.eclipse.org/jetty/
  [Mithril]: http://mithril.js.org/
  [Fluid Tools]: https://github.com/aqueance/fluid-tools
 
## Building the Demo

Assuming you have [Maven] installed, the following command will build the
project deployables. 

```console
$ mvn package -q
```

Use the following command see what components are used and what modules get
packaged in the deployable archives:

```console
$ mvn package -Dverbose
```

## The Demo

The build generates two deployable archives: a command line tool, and an
online tool.

### Command Line Tool

The command line tool is generated as
`salary-calculator-cli/target/salary-calculator-cli-1.0-SNAPSHOT.jar`. To use
it, just run it as a Java archive and pass it the input file's path as the
first argument, like so:

```console
$ java -jar salary-calculator-cli/target/salary-calculator-cli-1.0-SNAPSHOT.jar <input file path>
```

### Online Tool

The online tool is generated as
`salary-calculator-http/target/salary-calculator-http-1.0-SNAPSHOT.jar`. To
use it, just run it as a Java archive, open `http://localhost:8080`, and drop
the input file to the designated area:

```console
$ java -jar salary-calculator-http/target/salary-calculator-http-1.0-SNAPSHOT.jar
```

To stop the server, just kill the Java process (e.g., press Ctrl-C).

If you want the server to listen on a different port, pass the port number as
the first argument:

```console
$ java -jar salary-calculator-http/target/salary-calculator-http-1.0-SNAPSHOT.jar <port>
```

## Highlights

My goals with this demo were twofold:

 1. to implement the task with the least amount code, and
 1. to explore tools I haven't used before.

As to the first goal, I used [Fluid Tools], which allowed me to have dependency
injection with _zero configuration_, be it code or data. It also allowed me to
generate self-contained executable Java archives, again without custom coding
or configuration. 

Zero-configuration dependency injection and code packaging then allowed me to
break down the code into _fine grained modules_ to aggressively avoid
duplication, which to me is a good thing, although as the author of
[Fluid Tools] I may be biased, or even blind to the complexity that many small
modules may lead to in practice.

As to the second goal, [Mithril] allowed me to make an HTML UI strictly in
JavaScript, with HTML only providing a bare wire-frame thereto, while [Jetty]
version 9.3+ gave me [highly efficient](https://webtide.com/eat-what-you-kill/)
HTTP processing, both of which I am very happy about.

In any case, the result is a self-contained command line tool that is about
**380 KB**, and a self-contained online tool that is about **2.3 MB**.

In the latter, in addition to the client assets, much of the space is taken up
by the embedded web container, so the size of the executable is not bad.

Originally, I used [Vert.x-Web](http://vertx.io/docs/vertx-web/java/) instead
of [Jetty], but that was a mistake. It required lower level coding to serve
the same content, and the size of the executable was about 6.5 MB – almost
three times as much as with [Jetty]. Apparently, [Vert.x](http://vertx.io/) is
something else than a web server.

## Technical Debt

In a real-life scenario, I would have implemented the online tool differently:

What is now the entire tool should have been split into two: a proper REST
API, and a separate client based on that API.

The client project should also have been based on [npm](https://www.npmjs.com/)
and used the proper tools for JavaScript client development.

***

Copyright © 2016 Tibor Adam Varga. All rights reserved.
