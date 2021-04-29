# Kotlin Tools

[![Build Status](https://img.shields.io/travis/tomtom-international/kotlin-tools.svg?maxAge=3600&branch=master)](https://travis-ci.org/tomtom-international/kotlin-tools)
[![License](http://img.shields.io/badge/license-APACHE2-blue.svg)]()
[![Release](https://img.shields.io/github/release/tomtom-international/kotlin-tools.svg?maxAge=3600)](https://github.com/tomtom-international/kotlin-tools/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.tomtom.kotlin/kotlin-tools.svg?maxAge=3600)](https://maven-badges.herokuapp.com/maven-central/com.tomtom.kotlin/kotlin-tools)

Copyright (C) 2020, TomTom International BV

## Introduction

This class library contains a number of useful tools when coding in Kotlin. The code has been made
open-source by TomTom so others can use it and contribute to it.

Currently, the library contains:

* `traceevents` - This is a library to log semantic events in an application, where perhaps normally
  logging and log analysis would be used.

* `uid` - This is a simple library to deal with more performant and 'typesafe' UUIDs.

* `memoization` - This modules the ability to cache function results.

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

If you wish to contribute to this project, please feel free to do so and send us pull requests. The
source code formatting must adhere to the standard Kotlin formatting rules.

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

By default, a simple event logger is provided which logs all events directly to `println`. Although
it is enabled by default, you can turn it off, or replace it with your own. It's easy to modify this
to, for example, use the standard Android`Log` logger.

### Trace events vs normal Log

What makes this event logger different from a normal `Log` or, for example, Android Log directly, is
that the events are 'type-safe':
they are defined as Kotlin functions with (type-safe) parameters, rather than as handcrafted (and
often refactor-unsafe) strings.

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
(3)  |      private val tracer = Tracer.Factory.create<MyTraceEvents>()
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
consumers, asynchronously. You can add a 'tagging' object to `create`. The class name of the
object (typically `this`) may be added to log if `@TraceOptions(includeTaggingClass = true)` is
used.

By default, a logging trace event consumer is enabled, which sends the events to a `Log` method that
send the message to `stdout` or can be redirected to, for example, Android `Log`. This consumer is a
special case in that it is actually a synchronous consumer, to make sure the order of events in the
log is consistent with the other log messages from your application.

The logger can be enabled or disabled using `Tracer.enableTraceEventLogging`. It can also be
switched to asynchronous mode. using `Tracer.setTraceEventLoggingMode`.

Note that only the logging consumer can be synchronous or asynchronous; custom event consumers are
always processed in asynchronous mode.

### Using your own default logger (such as Android Log)

You can replace the default logger, which logs to `println` with your own by creating an instance of
the interface `Log.Logger` like this:

```
import android.util.Log
import TraceLog.Logger

object MyAndroidLogger : Logger {
    override fun log(logLevel: LogLevel, tag: String?, message: String, e: Throwable?) =
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, e)
            LogLevel.INFO -> Log.d(tag, message, e)
            ...
        }
}

setLogger(MyAndroidLogger) 
```

Or, to reset it to the default implementation:

```
setLogger() 
```

### Using the `@TraceLogLevel` annotation

Trace events in a `TraceEventListener` can be annotated with the `@TraceLogLevel` annotation.

#### `@TraceLoglevel(logLevel=...)`

`@TraceLoglevel` allows you to specify a log level, using parameter `logLevel` that will be used to
output the trace event to a standard logger using a specific log level, such as `INFO`, `DEBUG`
, `ERROR`, etc.

### Using the `@TraceOptions` annotation

#### `@TraceOptions(includeExceptionStackTrace=true|false)`

`@TraceOptions` offers the option to specify logging a full stack trace of a logged exception, using
the optional parameter `includeExceptionStackTrace`. This applies to the last argument of an event (
if it is a `Throwable` object). By default, a stack trace is included.

#### `@TraceOptions(includeFileLocation=true|false)`

`@TraceOptions` also offers the option to specify logging the filename and line number of the caller
of an event. By default, the caller source code location is not provided.

#### `@TraceOptions(includeTaggingClass=true|false)`

`@TraceOptions` offers the option to add the class passed to `create()`
to the log message, or omit that. By default, the caller class is not included.

#### `@TraceOptions(includeEventInterface=true|false)`

`@TraceOptions` offers the option to add the interface that defines the event to the log message, or
omit that. By default, the event interface is not included.

Example:

```
interface MyEvents : TraceEventListener {

    @TraceLogLevel(LogLevel.ERROR, includeExceptionStackTrace = true, includeOwnerClass = true)
    fun foundAnException(e: Exception)
}
``` 

### Logging in tests

Trace events are used for production code. Test cases should not use trace events. Test cases should
use the logging tools of the test framework, or simply the Android log, to show progress or states.

### Logging trace events at specific log levels

By default, trace events are logged at `LogLevel.DEBUG` level to the default logger, that can be
redirected to, for example Android `Log`. If you prefer to have certain events logged at a different
log level, you can specify the log level with an annotation from `TraceLogLevel`, like this:

```
interface MyTraceEvents : TraceEventListener {

   fun userLoggedIn()           // This events gets logged at the default DEBUG level.
   
   @TraceLogLevel(LogLevel.WARN)
   fun cannotAccessDatabase()   // This events gets logged at WARN level.
}
```

### Coding conventions for events

This paragraph describes some simple coding conventions to promote consistent declaration and usage
of events.

#### Declaration of event interfaces

In general, events are coupled to specific classes and as such it is advised to declare them inside
the class file that uses the events. Place the interface declaration at the end of the class.

*Tip: In IntelliJ or Android Studio you can easily view all trace event interfaces by clicking on
any occurrence of `TraceEventListener` and pressing `Ctrl-Alt-B` (or `Option-Cmd-B`)*

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

and you find that sometimes you need to call the event as `connectionEstablished(channel!!)`, rather
than as `connectionEstablished(channel)`.

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

Note that trace event consumer may receive mutable objects in an event, which may have been modified
after the actual event was thrown. For more info, see the FAQ below.

### Using tracers to standard `Log` messages

The `Tracer` class defines the standard `Log` functions `v()`, `d()`, `i()`, `w()` and `e()`. These
can be used like this:

```
tracer.d("Found record $record")    // Equivalent of `Log.d(TAG, msg)
tracer.w("Exception found", e)      // Equivalent of `Log.w(TAG, msg, e)
```

By default, tracers are set up to send these log messages synchronously to the default logger.

It is possible to register trace event consumers to process these log messages as well, for example,
to send logs to another system for analysis.

If you wish to define a tracer to *only* log standard messages using `d()` etc., you don't need to
create (or use) an `TraceEventListener` interface. You can just use this:

```
val tracer = Tracer.Factory.createLoggerOnly()
```

**Note:** Using the `Log.x` functions on tracers defeats the advantages of using type-safe
arguments. Always consider using trace event functions, when possible.

### Formatting trace arguments

By default, trace event arguments are formatted using their default `toString` function. If you wish
to override them, use `Tracer.registerToString<T>{ <toString implementation> }` to override
the `toString` method. For example:

```
Tracer.registerToString<Coordinate>{ "($lat, $lon" }
```

By default, the `toString` for `Array`s is replaced with one that provides a list of elements,
rather than an object reference.

Use `Tracer.resetToDefaults()` to de-register all custom `toString` handlers.

### Using a `context` to disambiguate tracers with the same name

Sometimes multiple tracers may exist for a single class (if multiple instances of the tracer are initiated).
In those cases, it may be necessary to be able to disambiguate the tracer that the trace events came from.
This is solved by adding a `context` string to the `create` method. This context string is passed to
trace event consumers. Alternatively, trace event consumers can specify a regular expression to make sure
they only get the trace events for the specified tracer context(s).

```
// Declare 2 tracers.
val tracerMain = Tracer.Factory.create<SomeClass>(this::class, "main loop")
val tracerSec  = Tracer.Factory.create<SomeClass>(this::class, "secondary")

// Declare a consumer for events from any tracer starting with "main".
val consumerMain = MyEventConsumerForSomeClass()
Tracer.addTraceEventConsumer(consumerMain, Regex("main.*"));

// Declare a consumer for all events (leaving out the regex).
val consumerAll = MyEventConsumerForSomeClass()
Tracer.addTraceEventConsumer(consumerAll);
```

Note that only `GenericTraceEventConsumer`s are able to retrieve the context string passed by the tracer (as it is
part of the `TraceEvent` data object. Specific `TraceEventConsumer`s (that implement the original tracer
interface), cannot access the context while processing events. 

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

  Yes and no. The tracer module never modifies objects. But consumers of trace events will receive a
  mutable object, which may have been modified since the event was thrown. This may not be expected
  by the event consumer, although often it's not a problem at all. If you have to get around this
  you need provide a clone of the object in the trace call yourself. Beware that cloning objects to
  make them immutable is relatively expensive and may generate significant overhead.

### More information on trace events

For more information, for example on how to define and register trace event consumers, please refer
to the documentation in the `Tracer` class.

## Module: Uid

Generic immutable unique ID class. Really just an abstraction of UUIDs. Used to uniquely identify
things. No 2 Uid objects shall be equal unless they are the same instance of the Uid class or their
underlying UUIDs are equal.

The class has a generic type T to allow creating typesafe Uid's, like `Uid<Message>`. The class
represents UUIDs as Strings internally, to avoid loads of UUID to String conversions all the time.
This makes the class considerably faster in use than the regular Java UUID class.

Examples:

```kotlin
var messageId = Uid<Message>()    // Creates a new unique message ID.
var personId = Uid<Person>()      // Creates a new unique person ID.

messageId = personId              // <-- Does not compile, the IDs are type safe.

val testId = Uid.fromString("1-2-3-4-5")    // Allows shorthand notation for UUIDs,
// easy for testing, or input.

val s = messageId.toString()      // Serialized to string.
val id = Uid<Message>(s)          // Deserialized from string. Faster than `fromString`
// if the format is known to be the serialized format.

val messageId: Uid<Message>()              // If you need, you can translate IDs from one
val personId: messageId as Uid<Person>     // type to another using 'as'. This is useful if
// the type information was lost, for example,
// in serialization.
```

## Module: Function Memoization

Provides `memoize` extension to Kotlin functions that allows optimizing expensive functions by
caching the results corresponding to some set of specific inputs.

In order for memoization to work properly:

- all input arguments must have proper `equals` and `hashCode` implementations,
- given the same input, the function must always return same output (i.e. not dependent on external
  parameters), and
- the function should not exhibit any side effects, other than the returned result.

To memoize a function, use the extenstion:

- `memoize(Int)` - Extension creates Least Recently Used (LRU) cached function that will remove
  least recently used item if number of stored results exceeds provided limit.

Note that currently only functions with a maximum of 4 parameters are supported by memoize.

Example:

```kotlin
val function1: (Int) -> String = { p1: Int -> p1.toString() }.memoize()

function1(10) // First call with 10 - actual function is called and result is cached.
function1(11) // First call with 11 - actual function is called and result is cached.
function1(10) // Second call with 10 - value is returned from cache.
```

## License

Copyright (C) 2020-2020, TomTom (http://tomtom.com). Licensed under the Apache License, Version
2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain
a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0]

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.

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

If you wish to contribute to this project, please feel free to do so and send us pull requests. The
source code formatting must adhere to the standard Kotlin formatting rules.

If you use IntelliJ IDEA, you can easily import the default Kotlin formatting rules like this:

```
Preferences -> Editor -> Code Style -> Kotlin -> (Scheme) Set From...
```

And then choose the pre-defined style `Kotlin Style Guide`. Voila!

## Credits

Author: Rijn Buve

Contributors: Timon Kanters, Jeroen Erik Jensen, Krzysztof Karczewski

## Release notes

### 1.3.0

* Added `context` to `Tracer.create` to allow disambiguation of tracers, if there are multiple for the same class.

### 1.2.1 - 1.2.2

* Updated dependecies and copyright.

### 1.2.0

* Added Kotlin function memoization extensions.

### 1.1.1

* Updated dependencies (except `mockk` as 1.10.2 will fail the test for unclear reasons).

### 1.1.0

* Added `Uid` class for UUID handling.

### 1.0.17-1.0.18

* Minor bug fixes.

### 1.0.16

* Updated dependencies. No functional changes.

### 1.0.15

* Made `TraceEventConsumerCollection` a public class to be able to use it also for consuming
  streamed events.

### 1.0.14

* Minor bug fixes.

* Made `TraceEvent.stackTraceHolder` nullable.

### 1.0.13

* Bug fix, renaming non-DEX formattable function name.

### 1.0.12

* Renamed
  annotations: `@TraceOptions(includeExceptionStackTrace, includeTaggingClass, includeFileLocation, includeEventInterface)`

* Added ability to add tagging class to tracers.

### 1.0.11

* Removed restriction that tracers can only be created in a `companion object`.

### 1.0.10

* Cleaned up annotation `@TraceLogLevel` to only include trace level.

*
Added `@TraceOptions(includeExceptionStackTrace, includeCalledFromClass, includeCalledFromFile, includeEventInterface)`

* Added `throwableHolder` to `TraceEvent` so event handlers can inspect the stack as well.

### 1.0.9

* Added `includeFileLocation` to `@TraceLoglevel` add the caller filename and line number to the
  logger.

### 1.0.8

* Added `includeExceptionStackTrace` to annotation `@TraceLogLevel`.

* Added `includeOwnerClass` to annotation `@TraceLogLevel`.

* Removed time stamp from `SYNC` logging (already added by most loggers).

* Added unit tests to check message formats.

### 1.0.7

* Added `Tracer.Factory.createLoggerOnly(this)`.

### 1.0.6

* Rename `Log` to `TraceLog` and `LogLevel` to `TraceLogLevel`.

* Fixed string representation of arrays.

* Added unit tests for message formatting.

### 1.0.5

* Added `Tracer.RegisterToString` to register string handlers for class types.

### 1.0.4

* Fixed license and copyright messages to Apache License 2.0.

### 1.0.3

* Bug fixes to simple loggers `Tracer.d(message, exception)`.

* Fixed formatting of event parameters for arrays and lists.

* Fixed unit test helper method.

* `TAG` is non-nullable for loggers.

* Renamed directory structure from `src/main/kotlin` to `/src/main/java` for IntelliJ to understand
  package names

* Added TravisCI support for Github, including status badges in README.

### 1.0.0-1.0.2

* Initial release
