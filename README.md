# Kotlin Tools

[![Build Status](https://img.shields.io/travis/tomtom-international/kotlin-tools.svg?maxAge=3600&branch=master)](https://travis-ci.org/tomtom-international/kotlin-tools)
[![License](http://img.shields.io/badge/license-APACHE2-blue.svg)]()
[![Release](https://img.shields.io/github/release/tomtom-international/kotlin-tools.svg?maxAge=3600)](https://github.com/tomtom-international/kotlin-tools/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.tomtom.kotlin/kotlin-tools.svg?maxAge=3600)](https://maven-badges.herokuapp.com/maven-central/com.tomtom.kotlin/kotlin-tools)

Copyright (C) 2020, TomTom International BV

## Introduction

This class library contains a number of useful tools when coding in Kotlin. The code has been
made open-source by TomTom so others can use it and contribute to it.

Currently, the library contains:

* `traceevents` - This is a library to log semantic events in an application, where perhaps normally
logging and log analysis would be used.

### Building and testing the library

Use Maven to run the unit tests as follows:

```
mvn clean test
```

To build the library JAR:

```
mvn clean package
```

### Contributing and coding formatting

If you wish to contribute to this project, please feel free to do so and send us pull requests.
The source code formatting must adhere to the standard Kotlin formatting rules.

If you use IntelliJ IDEA, you can easily import the default Kotlin formatting rules like this: 
```
Preferences -> Editor -> Code Style -> Kotlin -> (Scheme) Set From...
```

And then choose the pre-defined style `Kotlin Style Guide`. Voila!

## Module: Trace events

Trace events offer a flexible way of logging semantic events during the run of an application.
Semantic events are events that have some "external meaning", like "a route is planned", "the radio
was switched on", "a connection to a phone was established", "a user logged in" and so on. (In
contrast to debug messages likes "string is too long", "found record in database", etc. which only
have an internal meaning to the program.)

The purpose of logging such events can be multiple, such as:

* for logging, or live monitoring of the system, when events are shown on a console or dashboard;
* for post-mortem debugging, to see what has happened in the system, after a bug was found;
* for system testing, where the test framework checks received events;
* for simulation purposes, where the events are forwarded to and processed by another system;
* to gather user behavior data, where user events are sent to a statistics module.

By default, a simple event logger is provided which logs all events directly to `println`.
Although it is enabled by default, you can turn it off, or replace it with your own.
It's easy to modify this to, for example, use the standard Android`Log` logger.

### Trace events vs normal Log

What makes this event logger different from a normal `Log` or, for example, Android Log directly,
is that the events are 'type-safe':
they are defined as Kotlin functions with (type-safe) parameters, rather than as
handcrafted (and often refactor-unsafe) strings.

This means that rather than calling something like `Log.d(TAG, "User logged in, user=$user")`, you
would write a type-safe expression like `tracer.userLoggedIn(user)`.

The code for that looks like this:

```
(1)  |  interface MyTraceEvents : TraceEventListener {
(1)  |     fun userLoggedIn(user: User)
(1)  |  }
     |
     |  fun someFunctionToLogIn(user: User) {
     |      // Throw the trace event:
(2)  |     tracer.userLoggedIn(user)
     |  }
     |
     |  companion object {
(3)  |      private val tracer = Tracer.Factory.create<MyTraceEvents>(this)
     |  }
```

The lines marked (1) define the trace events; the line marked (2) throws (or logs) the event. That's
it. Almost.

Trace events are defined in a local interface that derives from `TraceEventListener`. The interface
lists all events as type-safe function calls. In order to get an object to actually be able to call
these functions, you need to call `Tracer.Factory.create` from your class's companion object. This
explains line (3).

It creates a proxy object `tracer` (also shown in line (2) above), which implements all trace events
for you. The implementation of the trace event methods sends the serialized functional name and its
parameters to an event queue. The events on this queue are subsequently consumed by trace events
consumers, asynchronously.

By default, a logging trace event consumer is enabled, which sends the events to a `Log` method
that send the message to `stdout` or can be redirected to, for example, Android `Log`.
This consumer is a special case in that it is actually a synchronous consumer, to make sure the
order of events in the log is consistent with the other log messages from your application.

The logger can be enabled or disabled using `Tracer.enableTraceEventLogging`. It can also be
switched to asynchronous mode. using `Tracer.setTraceEventLoggingMode`.

Note that only the logging consumer can be synchronous or asynchronous; custom event consumers are
always processed in asynchronous mode.

### Using your own default logger (such as Android Log)

You can replace the default logger, which logs to `println` with your own by creating an
instance of the interface `Log.Logger` like this:

```
import android.util.Log

object MyAndroidLogger : Logger {
    override fun log(level: Log.Level, tag: String?, message: String, e: Throwable?) =
        when (level) {
            Log.Level.DEBUG -> Log.d(tag, message, e)
            Log.Level.INFO -> Log.d(tag, message, e)
            ...
        }
}

setLogger(MyAndroidLogger) 
```

Or, to reset it to the default implementation:

```
setLogger() 
```

### Logging in tests

Trace events are used for production code. Test cases should not use trace events. Test cases should
use the logging tools of the test framework, or simply the Android log, to show progress or states.

### Logging trace events at specific log levels

By default, trace events are logged at `Log.DEBUG` level to the default logger, that can be
redirected to, for example Android `Log`. If you prefer to have certain events logged at 
a different log level, you can specify the log level with
an annotation from `Log.Level`, like this:

```
interface MyTraceEvents : TraceEventListener {

   fun userLoggedIn()           // This events gets logged at the default DEBUG level.
   
   @LogLevel(Log.Level.WARN)
   fun cannotAccessDatabase()   // This events gets logged at WARN level.
}
```

### Coding conventions for events
 
This paragraph describes some simple coding conventions to promote consistent declaration and usage
of events.

#### Declaration of event interfaces

In general, events are coupled to specific classes and as such it is advised to declare them
inside the class file that uses the events. Place the interface declaration at the end of the class.

*Tip: In IntelliJ or Android Studio you can easily view all trace event interfaces by clicking 
on any occurrence of `TraceEventListener` and pressing `Ctrl-Alt-B` (or `Option-Cmd-B`)*

#### Naming conventions for trace events

The following naming conventions apply to events function names in a `TraceEventListener`:

Tracing is used for ongoing states and events that have occurred. It's not used for actions. As a
result, the "trace event" names specify an ongoing state description, or use past tense. They don't
use present tense and are not imperative.

Examples event and state names:

* Correct (events): `userLoggedIn()`, `connectionEstablished()`
* Correct (states): `serviceReady()`, `establishingConnection()`

Examples of incorrect names:
* Wrong (imperative): `logUserIn()`, `establishConnection()`
* Wrong (wrong state name): `serviceInReadyState()`, `startingToEstablishConnection()`,
  `serviceInReadyState()`

Note that there's not always a crystal-clear distinction between what events and states are, but for
the naming convention that doesn't matter.

#### Trace event arguments

Trace event arguments are type-safe. They don't need to be strings. In fact, it's often better to
pass the original object, rather than some form of string representation of it, as the object itself
provides more information to trace consumer.

So, rather than defining an error event like
```
fun connectionLost(message: String)
```

when you know the message comes from an exception, like `connectionLost(exception.message)` you
better define the event as
```
fun connectionLost(reason: IOException)
```

and invoke it as `connectionLost(exception)`.

### Null pointer exceptions (NPE) in trace events

Trace events should always avoid throwing unexpected NPE's. This might happen if your trace event
has a signature like
```
fun connectionEstablished(channel: Channel)
```

and you find that sometimes you need to call the
event as `connectionEstablished(channel!!)`, rather than as `connectionEstablished(channel)`.

If that happens, you should consider changing the event signature to
```
fun connectionEstablished(channel: Channel?)
```

(and the invocation to `connectionEstablished(channel)`), just to make sure the `!!` operator cannot
throw a NPE.

Especially in cases where the arguments for an event come from Java libraries, where it's not always
clear if the type is `@Nonnull` (or `@NonNull`) or `@Nullable`, care should be take to make the
event interfaces NPE-safe.

### Trace event consumers

There are 2 types of trace events consumers:

1. Generic trace event consumers, derived both from `GenericTraceEventConsumer` and from a
   `TraceEventListener` interface.

    These consumers receive every event thrown in the system. They receive the event information as
    part of their `GenericTraceEventConsumer.consumeTraceEvent` implementation.

    Generic consumers typically forward events to another system, such as the Android `Log`, store
    them in a database, or perhaps even send them across application (or machine) boundaries.

2. Specific trace event consumers, that implement a specific `TraceEventListener` interface.

    For example, you could implement the `MyTraceEvents` interface (see above) in a class called
    `MyTraceEventsConsumer` and register it as a trace events consumer. From then on, whenever a
    function from the MyTraceEvents interface is called, the corresponding implementation in
    `MyTraceEventsConsumer` will be called (asynchronously).

    Specific consumers typically provide specific handler code for specific events. They react on
    specific events, rather than forward them. For example, switching on a red light on an alarm
    dashboard, when the event `temperatureTooHigh()` is received.

Note that trace event consumer may receive mutable objects in an event, which may have been
modified after the actual event was thrown. For more info, see the FAQ below.

### Using tracers to standard `Log` messages

The `Tracer` class defines the standard `Log` functions `v()`, `d()`, `i()`, `w()` and `e()`. 
These can be used like this:

```
tracer.d("Found record $record")    // Equivalent of `Log.d(TAG, msg)
tracer.w("Exception found", e)      // Equivalent of `Log.w(TAG, msg, e)
```

By default, tracers are set up to send these log messages synchronously to the default logger.

It is possible to register trace event consumers to process these log messages as well, for example,
to send logs to another system for analysis.

**Note:** Using the `Log.x` functions on tracers defeats the advantages of using type-safe
arguments. Always consider using trace event functions, when possible.

### Advanced examples

Advanced examples of using this trace event mechanism are:

* Sending events to a simulation system, which simulates the environment of the system. For example,
  an event that the cabin temperature has been set, may be processed by a simulator which uses a
  trace event consumer to receive such messages.

* Displaying events on a dashboard, to gain more insight in the current status of the system, rather
  than having only a scrolling log to look at.

* Collecting or sending system usage data for analytics. Developers can define all sorts of semantic
  events, and the system may collect them easily in a database, for later processing.

### FAQ

* **Should I use `Trace` or (Android) `Log`?**

    As a rule of thumb: you should always use `Tracer` instead of Android `Log`. If you only want to
    regular log messages, you can use the log functions `d()` etc.

* **What is the performance penalty of using `Tracer` over Android `Log`?**

    Using the default (synchronous) logging of log messages via `Tracer`, over using Android `Log`,
    introduces a performance overhead of less than 20%.

* **Is it safe to trace mutable objects?**

    Yes and no. The tracer module never modifies objects. But consumers of trace events will receive
    a mutable object, which may have been modified since the event was thrown. This may not be
    expected by the event consumer, although often it's not a problem at all. If you have to get
    around this you need provide a clone of the object in the trace call yourself. Beware that
    cloning objects to make them immutable is relatively expensive and may generate significant
    overhead.

### More information on trace events

For more information, for example on how to define and register trace event consumers, please refer
to the documentation in the `Tracer` class.

## License

Copyright (c) 2020 - 2020 TomTom N.V. All rights reserved.

This software is the proprietary copyright of TomTom N.V. and its subsidiaries and may be
used for internal evaluation purposes or commercial use strictly subject to separate
licensee agreement between you and TomTom. If you are the licensee, you are only permitted
to use this Software in accordance with the terms of your license agreement. If you are
not the licensee then you are not authorised to use this software in any manner and should
immediately return it to TomTom N.V.

## Building and testing the library

Use Maven to run the unit tests as follows:

```
mvn clean test
```

To build the library JAR:

```
mvn clean package
```

## Contributing and coding formatting

If you wish to contribute to this project, please feel free to do so and send us pull requests.
The source code formatting must adhere to the standard Kotlin formatting rules.

If you use IntelliJ IDEA, you can easily import the default Kotlin formatting rules like this: 
```
Preferences -> Editor -> Code Style -> Kotlin -> (Scheme) Set From...
```

And then choose the pre-defined style `Kotlin Style Guide`. Voila!

## Credits

Author: Rijn Buve

Contributors: Timon Kanters, Jeroen Erik Jensen

## Release notes

### 1.0.4

* Bug fixes to simple loggers `Tracer.d(message, exception)`.

* Fixed formatting of event parameters for arrays and lists.

* Fixed unit test helper method.

### 1.0.3

* `TAG` is non-nullable for loggers. 

* Renamed directory structure from `src/main/kotlin` to `/src/main/java` for IntelliJ to
understand package names

* Added TravisCI support for Github, including status badges in README.
 
### 1.0.0-1.0.2

* Initial release
