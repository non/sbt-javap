## sbt-javap

> As above, so below.

### overview

*sbt-javap* is an SBT plugin to call `javap` directly from SBT.

Java provides the `javap` tool to disassemble and inspect Java
bytecode. This allows authors to see how their code operates at a low
level, to understand how high-level Scala concepts are encoded, and to
potentially spot performance issues.

One challenge with using `javap` is correctly specifying a classpath.
Since SBT knows your project's classpath, *sbt-javap* can
automatically invoke `javap` for you.

### quick start

Currently, *sbt-javap* is published for SBT 1.x.

To use *sbt-javap*, add the following to `project/plugins.sbt`:

```scala
addSbtPlugin("org.spire-math" % "sbt-javap" % "0.0.1")
```

### usage

*sbt-javap* provides one new SBT task: `javap`.

This command takes the fully-qualified name of a class, dissassembles
that class into a file, and then displays that file interactively via
a pager (by default `less`).

For example, running `javap scala.Unit` produces the following:

```
sbt:myproject> javap scala.Unit
decompiling scala.Unit to /Users/erik/t/sbt-javap-test/target/scala-2.12/javap/scala.Unit.bytecode
```

At that point, `less` will be used to view that file:

```
Compiled from "Unit.scala"
public abstract class scala.Unit {
  public static java.lang.String toString();
    Code:
       0: getstatic     #16                 // Field scala/Unit$.MODULE$:Lscala/Unit$;
       3: invokevirtual #18                 // Method scala/Unit$.toString:()Ljava/lang/String;
       6: areturn

  public static void unbox(java.lang.Object);
    Code:
       0: getstatic     #16                 // Field scala/Unit$.MODULE$:Lscala/Unit$;
       3: aload_0
       4: invokevirtual #22                 // Method scala/Unit$.unbox:(Ljava/lang/Object;)V
       7: return

  public static scala.runtime.BoxedUnit box(scala.runtime.BoxedUnit);
    Code:
       0: getstatic     #16                 // Field scala/Unit$.MODULE$:Lscala/Unit$;
       3: aload_0
       4: invokevirtual #26                 // Method scala/Unit$.box:(Lscala/runtime/BoxedUnit;)Lscala/runtime/BoxedUnit;
       7: areturn

  public scala.Unit();
    Code:
       0: aload_0
       1: invokespecial #30                 // Method java/lang/Object."<init>":()V
       4: return
}
(END)
```

Note that you can decompile any Scala or Java class that's on your
classpath, not just classes that you defined. Decompiling classes from
your dependencies (or from the standard library) can be very
illuminating.

### name-mangling

Scala uses name-mangling to encode various types of names into the
single Java class namespace. Here are some examples:

 * Scala objects (`object Foo`) have a `$` suffix appended (`Foo$`).
 * Scala traits (`trait Bar`) have a `$class` suffix appended (`Bar$class`).
 * Scala classes (`class Qux`) use their names as normal (`Qux`).

(In all of the above cases the other name-mangling rules may still apply.)

Scala types are often found inside of `object` values as a form of
namespacing. Scala uses a `$` delimiter to mangle these names. For
example, given `object Kennel { class Dog }` the inner class name
would become `Kennel$Dog`.

Finally, Scala also allows a wider range of names than Java. For
example, `+` is a valid name (e.g. `object + { ... }`), and would be
encoded in Java as `$plus`. If your class/trait/object has a name
containing these characters, you'll need to determine how the name was
mangled.

It's often useful to use the REPL to see how particular characters of
a name are encoded:

```
scala> object +*%
defined object $plus$times$percent
```

### future work

Error handling and reporting could be a lot better.

It's likely that the SBT-related code could be improved.

It should be possible to cross-publish this plugin for SBT 0.13.x as
well as SBT 1.x.

This README could easily be expanded to document SBT options, expand
on name-mangling, and explain how to interpet bytecode (or link to
other references).

It might be nice to parse and/or highlight bytecode to make it easier
to read. It would also be nice to support an option to decompile only
a particular method, rather than an entire class.

### copyright & license

All code is available to you under the Apache 2 license, available at
https://opensource.org/licenses/Apache-2.0.

Copyright Erik Osheim, 2017-2018.
