flickring [![Build](https://travis-ci.org/edersonmf/flickring.png)](https://travis-ci.org/edersonmf/flickring)
=========

Daemon that syncs a folder in the file system to your Flickr account.

There is no official distribution at this point. That means you need to build the application on your own.

1. System requirements: it's required to have those tools installed and configured before proceeding with installation.
  * Java Runtime Edition 1.7
  * Git
  * Linux: [jsvc](http://commons.apache.org/proper/commons-daemon/jsvc.html)
  * Windows: [procrun](http://commons.apache.org/proper/commons-daemon/procrun.html)
  
2. How to install

  2.1. Clone the repository
  <pre><code> >> git clone git@github.com:edersonmf/flickring.git</code></pre>

  2.3. Change directory to the recent created repository
  <pre><code> >> cd flickring</code></pre>
  
  2.2. Start a build
  
    - Linux version
    <pre><code> >> ./gradlew clean build distZip</code></pre>

    - Windows version
    <pre><code> >> gradle.bat clean build distZip</code></pre>

  2.3. Unzip the file located in
  <code> $FLICKRING_REPO_DIR/build/distributions/flickring-&lt;CURRENT_VERSION&gt;.zip</code> to a location of your choice.
  
  2.4. Now it's needed to configure flickring with api and secret keys of your yahoo account:
    * change directory to <code>$UNZIPPED_DIR</code>
    - Linux version:
      * run <code> >> ./setup.sh</code> and follow the instructions
    - Windows version
      * run <code> >> java -jar flickring-&lt;CURRENT_VERSION&gt;.jar</code> and follow the instructions (No time to work on a windows script so far. Any help will be really appreciated)
      
    * after this step is done a configuration file will be generated in <code>$UNZIPPED_DIR/conf/flickring.conf</code>
    * additionally you can edit this file and include some(only one for now) more configuration properties:
    <pre><code># Number of upload threads
    upload.thread.size = 3</code></pre>

  2.5. It's time to start the daemon already

    - Linux version:
      * run to start <code> >> sudo ./flickring.sh</code>
      * run to stop <code> >> sudo ./flickring.sh stop</code>
    - Windows version
      * Again, no time to work on a script for windows. Any help will be really appreciated. I will work on it some time in the future anyway.
