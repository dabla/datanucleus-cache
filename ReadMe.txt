Download Coherence 3.7.1 from Oracle website at:

http://www.oracle.com/technetwork/middleware/coherence/downloads/index.html

Unpack the downloaded archive/zip and go to the extracted 'coherence/lib' directory.

There, you'll install the coherence.jar library to your local Maven repository like this:

call mvn install:install-file -Dfile=coherence.jar -DgroupId=com.oracle.coherence -DartifactId=coherence -Dversion=3.7.1.0b27797 -Dpackaging=jar -DgeneratePom=true

Now the project will be buildable against the original Coherence interfaces.