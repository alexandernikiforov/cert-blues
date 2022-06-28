We depend on the following modules of the Java platform
   java.datatransfer
   java.desktop
   java.logging
   java.management
   java.naming
   java.sql
   java.xml
   jdk.httpserver
   jdk.unsupported

# execute in the build/distributions/lib directory:
~/.jdks/azul-11.0.13/bin/jdeps --list-deps -recursive --ignore-missing-deps --multi-release 11 --module-path . -cp . *.jar
