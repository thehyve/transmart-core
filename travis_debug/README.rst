Debugging Travis
================

Debugging Travis tests is not very straightforward. They run your tests in their own environment, but as far as I know they don't make an image of that environment available for local debugging. These tools you to set up a connection with the test environment when the tests run, and debug the tests.

The script consists of two steps: first it creates an SSH connection to your machine, then it forwards a port from your machine to the test VM. The second step is to have a bash shell listening on that port in the test VM, giving you shell access to the live test environment. This uses the ``socat`` command which is installed by default on Travis. (If not install it with apt.)

To debug Travis
---------------

Setup:

- Add the public key in this directory to your ``~/.ssh/authorized_keys``, or better yet, create a new user and add the key to its authorized_keys. NB: as the private key of this is publicly visible on Github, anyone can now connect to your account.
- Make sure there is a port to your machine (or whatever machine you want to use to receive the connection) visible from the internet
- Adjust the parameters in ``travis_debug/shellserver.sh`` to reflect the address and port that Travis should connect to, and optionally the ports that are forwarded
- Adjust .travis.yml to include a ``sh -c travis_debug/shellserver.sh`` command so that it will run the shellserver.
- Push the commit to github and let Travis build the tests

Debug:

- Wait for travis to connect to your machine
- The SSH connection will forward a port (by default 34567) to the Travis test machine
- execute ``nc localhost 34567`` on your local machine. This will connect to Travis and show a bash prompt
- Debug
- Exit the connection for Travis to continue with the rest of the tests

To debug the JVM process, adjust the shellserver.sh to background itself and adjust the Grails test run to include a ``--debug-jvm`` flag. By default the debugging port 5005 is also forwarded, so you can connect to it from your debugger.

**Important!** When you are done, remove the public key from your ``authorized_keys`` file, otherwise anyone on the internet who finds the test key on Github will be able to log in to your machine!
